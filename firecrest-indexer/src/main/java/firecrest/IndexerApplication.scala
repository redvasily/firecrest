package firecrest

import javax.inject.Inject

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{GraphDSL, RunnableGraph, Sink, Source}
import akka.stream.{ActorMaterializer, ClosedShape, SourceShape}
import com.softwaremill.react.kafka.{ConsumerProperties, ReactiveKafka}
import io.dropwizard.lifecycle.Managed
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration._

class IndexerApplication @Inject()(system: ActorSystem) extends Managed {

  val logger = LoggerFactory.getLogger(getClass)

  override def stop(): Unit = {
    logger.info("Stop")
    //    actor.foreach(system.stop)
    system.terminate()
    val res = Await.result(system.whenTerminated, 1000 millis)
    logger.info("{}", res)
  }

  override def start(): Unit = {
    logger.info("Start")
    buildIndexerGraph()
  }

  def buildIndexerGraph(): Unit = {
    implicit val actorSystem = system
    implicit val materializer = ActorMaterializer()

    val kafka = new ReactiveKafka()

    val publisher = kafka.consume(ConsumerProperties(
      bootstrapServers = "localhost:9092",
      topic = "firecrest-messages",
      groupId = "firecrest-indexer",
      valueDeserializer = new StringDeserializer()
    ))
    val kafkaSource = Source.fromPublisher(publisher)

    val g: RunnableGraph[NotUsed] = RunnableGraph.fromGraph(GraphDSL.create() {
      implicit builder =>
        import GraphDSL.Implicits._

        val src: SourceShape[ConsumerRecord[Array[Byte], String]] = builder.add(kafkaSource)
        val printSink = Sink.foreach[AnyRef](line => println(s"Received: $line"))

        src.out ~> printSink

        ClosedShape
    })

    g.run()

  }
}
