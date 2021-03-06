package firecrest.actors

import javax.inject.Inject

import akka.actor._
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, ClosedShape, OverflowStrategy}
import akka.util.ByteString
import com.softwaremill.react.kafka._
import firecrest.config.KafkaConfiguration
import org.apache.kafka.common.serialization.ByteArraySerializer

import scala.concurrent.duration._

class KafkaOutputActor @Inject() (config: KafkaConfiguration) extends Actor
  with ActorLogging {

  implicit val materializer = ActorMaterializer()

  import context._

  val kafka = new ReactiveKafka()
  val producerProperties = ProducerProperties(
    bootstrapServers = s"${config.host}:${config.port}",
    topic = config.topic,
    valueSerializer = new ByteArraySerializer()
  )
  log.info(s"Config: $config")
  val producerActorProps: Props = kafka.producerActorProps(producerProperties)
  val kafkaSink = Sink.actorSubscriber(producerActorProps)
  val source = Source.actorRef[ByteString](65536, OverflowStrategy.dropNew)

  val graph = RunnableGraph.fromGraph(GraphDSL.create(source, kafkaSink)((_, _)) {
    implicit builder =>
      (src, kafka) =>
        import GraphDSL.Implicits._

        val bcast = builder.add(Broadcast[ByteString](2))
        val printSink = Sink.foreach[String](line => log.debug("Sending: {}", line))
        val toString = Flow[ByteString].map(byteString => byteString.utf8String)
        val toKafkaMessage = Flow[ByteString].map(
          byteString => ValueProducerMessage(byteString.toArray))

        // @formatter:off
        src.out ~> bcast ~> toKafkaMessage ~> kafka
                   bcast ~> toString ~> printSink
        // @formatter:on
        ClosedShape
  })

  val (sourceActor, sinkActor) = graph.run()

  context.watch(sinkActor)

  override def receive: Receive = {
    case "die" =>
      log.info("Kafka failure. Crashing this actor.")
      throw new RuntimeException("Kafka failure")

    case data: ByteString =>
      log.debug("Forwarding: {}", data.utf8String)
      sourceActor ! data

    case terminated: Terminated =>
      log.info(s"Received terminated: $terminated")
      log.info("Asking to crash")
      system.scheduler.scheduleOnce(3000 millis, self, "die")
  }
}
