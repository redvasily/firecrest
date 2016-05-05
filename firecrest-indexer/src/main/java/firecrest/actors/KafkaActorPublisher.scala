package firecrest.actors

import java.util.Properties
import javax.inject.Inject

import akka.actor._
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import firecrest.{KafkaConfiguration, actors}
import org.apache.kafka.clients.consumer.KafkaConsumer
import scala.concurrent.duration._
import scala.collection.JavaConversions._

import scala.annotation.tailrec

object KafkaActorPublisher {
  case class Batch(messages: Seq[String])
}

class KafkaActorPublisher @Inject() (kafkaConfig: KafkaConfiguration)
  extends ActorPublisher[String] with ActorLogging {

  import KafkaActorPublisher._
  import context._

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

  def startReader(): ActorRef = {
    val reader = actorOf(Props.create(classOf[KafkaReader], consumer))
    watch(reader)
    reader
  }

  override def postStop(): Unit = {
    super.postStop()
    consumer.close()
  }

  val maxBufferSize = 1024
  var buffer = Vector.empty[String]

  var reader  = startReader()
  var readerAlive = true
  var wantReaderAlive = true
  var stopSent = false

  def controlReader() = {
    if (!wantReaderAlive && !stopSent) {
      log.info("Stopping a reader")
      // want it stopped, but the stop hasn't been sent
      stop(reader)
      stopSent = true
    }
    if (!readerAlive && wantReaderAlive) {
      // it's dead, but we want a new one
      log.info("Starting a reader")
      reader = startReader()
      readerAlive = true
      stopSent = false
    }
  }

  override def receive = {
    case Batch(messages) =>
      log.info(s"Received a batch: ${messages.size} messages")
      buffer = buffer ++ messages
      deliverBuf()
      if (buffer.size >= maxBufferSize) {
        wantReaderAlive = false
        controlReader()
      }
      log.info(s"Buffer size: ${buffer.size}")

    case Request(_) =>
      deliverBuf()

    case terminated: Terminated =>
      readerAlive = false
      controlReader()

    case Cancel =>
      context.stop(self)
  }

  @tailrec final def deliverBuf(): Unit = {

    def maybeResume() = if (buffer.size <= maxBufferSize / 2) {
      wantReaderAlive = true
      controlReader()
    }

    if (totalDemand > 0) {
      if (totalDemand <= Int.MaxValue) {
        val (use, keep) = buffer.splitAt(totalDemand.toInt)
        buffer = keep
        //        use.foreach(msg => log.info(s"Sending: $msg"))
        log.info(s"use: ${use.size} keep: ${keep.size}")
        use.foreach(onNext)
        maybeResume()
      } else {
        val (use, keep) = buffer.splitAt(Int.MaxValue)
        buffer = keep
        log.info(s"use: ${use.size} keep: ${keep.size}")
        //        use.foreach(msg => log.info(s"Sending: $msg"))
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

class KafkaReader(consumer: KafkaConsumer[String, String])
  extends Actor with ActorLogging {

  import KafkaActorPublisher._
  import KafkaReader._
  import context._

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason, message)
    log.info("Closing a consumer")
  }

  override def preStart(): Unit = {
    super.preStart()
    self ! Tick()
  }

  override def postRestart(reason: Throwable) = {
    // overridden to avoid sending a message to itself
  }

  override def receive = {
    case Tick() =>
      self ! Tick()
      //      log.info("Reading from kafka")
      val records = consumer.poll(100)
      if (records.count() > 0) {
        log.info(s"Got ${records.count()} records")
        consumer.commitSync()
        log.info("Committed positions")
      }
      val messages = records.map(record => record.value()).toVector
      if (messages.nonEmpty) {
        parent ! Batch(messages)
      }
  }

}
