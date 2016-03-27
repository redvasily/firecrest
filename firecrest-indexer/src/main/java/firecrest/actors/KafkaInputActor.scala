package firecrest.actors

import akka.actor.{Terminated, ActorRef, ActorLogging, Actor}
import akka.stream.{ClosedShape, ActorMaterializer}
import akka.stream.scaladsl.{Sink, GraphDSL, RunnableGraph, Source}
import com.softwaremill.react.kafka.{ConsumerProperties, ReactiveKafka}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer

import scala.concurrent.duration._

class KafkaInputActor extends Actor with ActorLogging {
  implicit val materializer = ActorMaterializer()
  import context._

  val kafka = new ReactiveKafka()

  val consumerProperties = ConsumerProperties(
    bootstrapServers = "localhost:9092",
    topic = "firecrest-messages",
    groupId = "firecrest-indexer",
    valueDeserializer = new StringDeserializer()
  )
  val consumerActorProps = kafka.consumerActorProps(consumerProperties)
  val kafkaSource = Source
    .actorPublisher[ConsumerRecord[Array[Byte], String]](consumerActorProps)

  val graph = RunnableGraph.fromGraph(GraphDSL.create(kafkaSource) {
    implicit builder =>
      implicit source =>
        import GraphDSL.Implicits._

        val printSink = Sink.foreach[AnyRef](line => println(s"Received: $line"))

        source.out ~> printSink

        ClosedShape
  })

  val sourceActor: ActorRef = graph.run()
  watch(sourceActor)
  log.info(s"Started. Watching $sourceActor")

  override def receive: Receive = {
    case "die" =>
      log.info("Witness me")
      throw new RuntimeException("Kafka failure")

    case terminated: Terminated =>
      log.info(s"Received terminated: $terminated")
      log.info("Asking to crash")
      system.scheduler.scheduleOnce(3000 millis, self, "die")
  }
}
