package firecrest

import java.util.Properties

import akka.actor.{Props, ActorSystem, ActorLogging}
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.{Sink, Source}
import org.apache.kafka.clients.consumer.KafkaConsumer
import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.collection.JavaConversions._

class KafkaActorPublisher extends ActorPublisher[String] with ActorLogging {
  import akka.stream.actor.ActorPublisherMessage._
  import context._

  val maxBufferSize = 10
  var buffer = Vector.empty[String]
  var counter = 0

  val props = new Properties()
  props.put("bootstrap.servers", "localhost:9092")
  props.put("group.id", "test")
  props.put("enable.auto.commit", "true")
  props.put("auto.commit.interval.ms", "1000")
  props.put("session.timeout.ms", "30000")
  props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
  props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
  val consumer = new KafkaConsumer[String, String](props)
  consumer.subscribe(Vector("firecrest-messages"))


  override def preStart() = {
    system.scheduler.scheduleOnce(1 second, self, "tick")
  }

  override def postRestart(reason: Throwable) = {}

  override def receive = {
    case "tick" =>
      system.scheduler.scheduleOnce(1 second, self, "tick")
//      log.info("Tick")

      if (buffer.size < maxBufferSize) {
        log.info("Reading from kafka")
        val records = consumer.poll(100)

        log.info(s"Got ${records.count()} records")
        consumer.commitSync();
        log.info("Committed positions")

        for (record <- records) {
          buffer :+= record.toString
        }

        if (buffer.nonEmpty && totalDemand > 0) {
          deliverBuf()
        }

//                if (buffer.isEmpty && totalDemand > 0) {
//                  onNext(msg)
//                } else {
//                  buffer :+= msg
//                  deliverBuf()
//                }
      }

//      if (buffer.size < maxBufferSize) {
//        val msg = s"Message $counter"
//        counter += 1
//
//        if (buffer.isEmpty && totalDemand > 0) {
//          onNext(msg)
//        } else {
//          buffer :+= msg
//          deliverBuf()
//        }
//      }

    case Request(_) =>
      deliverBuf()

    case Cancel =>
      context.stop(self)
  }

  @tailrec final def deliverBuf(): Unit = {
    if (totalDemand > 0) {
      if (totalDemand <= Int.MaxValue) {
        val (use, keep) = buffer.splitAt(totalDemand.toInt)
        buffer = keep
        use.foreach(onNext)
      } else {
        val (use, keep) = buffer.splitAt(Int.MaxValue)
        buffer = keep
        use.foreach(onNext)
        deliverBuf()
      }
    }
  }
}

object KafkaActorPublisher extends App {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

//  val publisherActor = system.actorOf(Props[KafkaActorPublisher])
  val publisher = Source.actorPublisher[String](Props[KafkaActorPublisher])
  val sink = Sink.foreach[String](println)
  val g = publisher.to(sink)
  g.run()
//  Thread.sleep(10.seconds.toMillis)
//  system.terminate()
}
