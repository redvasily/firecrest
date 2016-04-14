package firecrest.actors

import java.util.Properties
import javax.inject.Inject

import akka.actor.{Actor, Props, ActorRef, ActorLogging}
import akka.stream.actor.ActorPublisher
import firecrest.{actors, KafkaConfigIndexer}
import org.apache.kafka.clients.consumer.KafkaConsumer
import scala.concurrent.duration._
import scala.collection.JavaConversions._

import scala.annotation.tailrec

object KafkaActorPublisher {
  case class Batch(messages: Seq[String])
  case class Stop()
  case class Resume()
}

class KafkaActorPublisher @Inject() (kafkaConfig: KafkaConfigIndexer)
  extends ActorPublisher[String] with ActorLogging {

  import KafkaActorPublisher._
  import context._

  val maxBufferSize = 1024
  var buffer = Vector.empty[String]
  val reader: ActorRef = actorOf(Props.create(classOf[KafkaReader], kafkaConfig))

  override def receive = {
    case Batch(messages) =>
      log.info(s"Received a batch: ${messages.size} messages")
      buffer = buffer ++ messages
      deliverBuf()
      if (buffer.size >= maxBufferSize) {
        reader ! Stop()
      }
  }

  @tailrec final def deliverBuf(): Unit = {

    def maybeResume() = if (buffer.size <= maxBufferSize / 2) { reader ! Resume() }

    if (totalDemand > 0) {
      if (totalDemand <= Int.MaxValue) {
        val (use, keep) = buffer.splitAt(totalDemand.toInt)
        buffer = keep
        use.foreach(msg => log.info(s"Sending: $msg"))
        use.foreach(onNext)
        maybeResume()
      } else {
        val (use, keep) = buffer.splitAt(Int.MaxValue)
        buffer = keep
        use.foreach(msg => log.info(s"Sending: $msg"))
        use.foreach(onNext)
        maybeResume()
        deliverBuf()
      }
    }
  }
}

object KafkaReader {
  case class Tick()
}

class KafkaReader(kafkaConfig: KafkaConfigIndexer) extends Actor with ActorLogging {

  import KafkaActorPublisher._
  import KafkaReader._
  import context._

  var active = true

  val props = new Properties()
  props.put("bootstrap.servers", s"${kafkaConfig.host}:${kafkaConfig.port}")
  props.put("group.id", "test")
  props.put("enable.auto.commit", "false")
  props.put("auto.commit.interval.ms", "1000")
  props.put("session.timeout.ms", "30000")
  props.put(
    "key.deserializer",
    "org.apache.kafka.common.serialization.StringDeserializer")
  props.put(
    "value.deserializer",
    "org.apache.kafka.common.serialization.StringDeserializer")
  val consumer = new KafkaConsumer[String, String](props)
  consumer.subscribe(Vector(kafkaConfig.topic))

  override def preStart() = {
    system.scheduler.scheduleOnce(1 second, self, Tick())
  }

  override def postRestart(reason: Throwable) = {
    log.info("Closing a consumer")
    consumer.close()
  }

  override def receive = {
    case Stop() =>
      active = false
      log.info("Stopping consumption")

    case Resume() =>
      log.info("Resuming consumption")
      if (!active) {
        self ! Tick()
      }
      active = true

    case Tick() =>
      log.info("Reading from kafka")
      val records = consumer.poll(100)
      log.info(s"Got ${records.count()} records")
      if (records.count() > 0) {
        self ! Tick()
      } else {
        log.info("The last batch was empty. Waiting a bit")
        system.scheduler.scheduleOnce(1 second, self, Tick())
      }
      consumer.commitSync()
      log.info("Committed positions")
      val messages = records.map(record => record.value()).toVector
      if (messages.nonEmpty) {
        parent ! Batch(messages)
      }
  }
}
