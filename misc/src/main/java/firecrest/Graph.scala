package firecrest

import akka.actor.ActorRef
import akka.stream.javadsl.RunnableGraph
import akka.stream.scaladsl._
import akka.stream.{ClosedShape, OverflowStrategy}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import firecrest.Message
import org.elasticsearch.client.support.AbstractClient

import scala.collection.immutable.Seq
import scala.concurrent.duration._

object Graph {
  def build(client: AbstractClient): RunnableGraph[ActorRef] = {
    val source: Source[Message, ActorRef] = Source.actorRef(16, OverflowStrategy.fail)

    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper.registerModule(new JodaModule())

    val esSinkObj = new EsSink(mapper, client)

    RunnableGraph.fromGraph(GraphDSL.create(source) {
      implicit builder =>
        src =>
          import GraphDSL.Implicits._

          val grouping = Flow[Message].groupedWithin(5, 1.seconds)
          val sink1 = Sink.foreach[Seq[Message]](in => pprint.pprintln(in))
          val esSink = Sink.foreach[Seq[Message]](esSinkObj.index)

          val bcast = builder.add(Broadcast[Seq[Message]](2))

          src.out ~> grouping ~> bcast

          bcast ~> sink1
          bcast ~> esSink

          ClosedShape
    })
  }

}
