package firecrest

import javax.inject.Inject

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source}
import akka.stream.{ActorMaterializer, ClosedShape, OverflowStrategy}
import akka.util.ByteString
import com.softwaremill.react.kafka.{ProducerMessage, ProducerProperties, ReactiveKafka, ValueProducerMessage}
import firecrest.actors.{DummyActor, TcpListener}
import io.dropwizard.lifecycle.Managed
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.reactivestreams.Subscriber
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration._

class BridgeApplication @Inject()(system: ActorSystem) extends Managed {
  val logger = LoggerFactory.getLogger(getClass)
  var actor = Option.empty[ActorRef]

  override def stop(): Unit = {
    logger.info("Stop")
    //    actor.foreach(system.stop)
    system.terminate()
    val res = Await.result(system.whenTerminated, 1000 millis)
    logger.info("{}", res)
  }

  override def start(): Unit = {
    logger.info("Start")

    actor = Some(system.actorOf(Props[DummyActor], "ticker"))
    val graphSource = buildGraph(system)
    system.actorOf(Props(classOf[TcpListener], graphSource), "listener")
  }

  def buildGraph(actorSystem: ActorSystem): ActorRef = {
    implicit val system = actorSystem
    implicit val materializer = ActorMaterializer()

    val source: Source[Nothing, ActorRef] =
      Source.actorRef(16, OverflowStrategy.dropNew)

    //    val source2: Source[Nothing, ActorRef] =
    //      Source.actorRef(16, OverflowStrategy.dropNew)

    val kafka = new ReactiveKafka()
    val subscriber: Subscriber[ProducerMessage[Array[Byte], Array[Byte]]] = kafka.publish(ProducerProperties(
      bootstrapServers = "localhost:9092",
      topic = "firecrest-messages",
      valueSerializer = new ByteArraySerializer()
    ))
    val kafkaSink = Sink.fromSubscriber(subscriber)

    val g = RunnableGraph.fromGraph(GraphDSL.create(source) {
      implicit builder =>
        src =>
          import GraphDSL.Implicits._

          val bcast = builder.add(Broadcast[ByteString](2))
          val printSink = Sink.foreach[String](line => println(s"Sending: $line"))
          val toString = Flow[ByteString].map(byteString => byteString.utf8String)
          val toKafkaMessage = Flow[ByteString].map(byteString => ValueProducerMessage(byteString.toArray))
          val kafka = builder.add(kafkaSink)

          // @formatter:off
          src.out ~> bcast ~> toKafkaMessage ~> kafka
                     bcast ~> toString ~> printSink
          // @formatter:on

          ClosedShape
    })

    g.run()
  }
}
