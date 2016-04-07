package firecrest.actors

import java.net.InetAddress
import javax.inject.Inject

import akka.actor._
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, ClosedShape}
import akkaguiceutils.GuiceUtils
import com.softwaremill.react.kafka.{ConsumerProperties, ReactiveKafka}
import firecrest.{ElasticSearchConfig, KafkaConfigIndexer}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress

import scala.concurrent.duration._

class KafkaInputActor @Inject() (kafkaConfig: KafkaConfigIndexer,
                                 elasticSearchConfig: ElasticSearchConfig)
  extends Actor with ActorLogging with GuiceUtils {

  implicit val materializer = ActorMaterializer()
  import context._

  val kafka = new ReactiveKafka()

  val consumerProperties = ConsumerProperties(
    bootstrapServers = s"${kafkaConfig.host}:${kafkaConfig.port}",
    topic = kafkaConfig.topic,
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
        val extractBody = Flow[ConsumerRecord[Array[Byte], String]]
          .map(record => record.value())
        val group = Flow[String].groupedWithin(100, 5000 millis)
        val batchWrap = Flow[Seq[String]].map(lines => EsActorSubscriber.Batch(lines))
        val bcast = builder.add(Broadcast[EsActorSubscriber.Batch](2))
        val esSink = Sink.actorSubscriber(props(classOf[EsActorSubscriber]))

        // @formatter:off
        source.out ~> extractBody ~> group ~> batchWrap ~> bcast ~> printSink
                                                           bcast ~> esSink
        // @formatter:on

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
