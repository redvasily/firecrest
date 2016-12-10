package firecrest.actors

import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

import akka.actor._
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, ClosedShape}
import akkaguiceutils.GuiceUtils
import com.softwaremill.react.kafka.ReactiveKafka
import firecrest.config.{ElasticSearchConfig, KafkaConfiguration}

import scala.concurrent.duration._

class IndexerActor @Inject() (kafkaConfig: KafkaConfiguration,
                                 elasticSearchConfig: ElasticSearchConfig)
  extends Actor with ActorLogging with GuiceUtils {

  implicit val materializer = ActorMaterializer()
  import context._

  val kafka = new ReactiveKafka()

  val consumerProperties = props(classOf[KafkaActorPublisher])

  log.info(s"Consumer properties: $consumerProperties")

  val consumerActorProps = props(classOf[KafkaActorPublisher])
  val kafkaSource = Source.actorPublisher[String](consumerActorProps)

  val batchId = new AtomicLong()

  val graph = RunnableGraph.fromGraph(GraphDSL.create(kafkaSource) {
    implicit builder =>
      implicit source =>
        import GraphDSL.Implicits._

        val printSink = Sink.foreach[AnyRef](line => println(s"Received: $line"))
        val group = Flow[String].groupedWithin(10000, 5000 millis)
        val batchWrap = Flow[Seq[String]].map(lines =>
          EsActorSubscriber.Batch(batchId.incrementAndGet(), lines)
        )
        val esSink = Sink.actorSubscriber(props(classOf[EsActorSubscriber]))

        // @formatter:off
        source.out ~> group ~> batchWrap ~> esSink.async
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
