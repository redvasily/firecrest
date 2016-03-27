package firecrest

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.actor.{ActorSubscriber, ActorSubscriberMessage, MaxInFlightRequestStrategy}
import akka.stream.scaladsl.{Sink, Source}

import scala.concurrent.duration._

object SubscriberTest {

  def main(args: Array[String]): Unit = {
    println("Started")

    implicit val actorSystem = ActorSystem()
    implicit val materializer = ActorMaterializer()

    val N = 70
    Source(1 to N).runWith(Sink.actorSubscriber(Props[SubscriberTest]))

    println(s"End of main")
  }

  case class Done(element: Integer)
}

class SubscriberTest extends ActorSubscriber with ActorLogging {

  import ActorSubscriberMessage._
  import SubscriberTest._
  import context._

  val concurrentRequests = 10
  var inFlight = 0

  override val requestStrategy = new MaxInFlightRequestStrategy(max = concurrentRequests) {
    override def inFlightInternally: Int = {
      log.info("Requesting inFlightInternally")
      inFlight
    }
  }

  override def receive: Receive = {
    case OnNext(element) =>
      val elementInt = element.asInstanceOf[Int]
      inFlight += 1
      log.info(s"Starting processing of $elementInt")
      system.scheduler.scheduleOnce(1000 millis, self, "stuff")
      system.scheduler.scheduleOnce(2000 millis, self, "stuff")
      system.scheduler.scheduleOnce(3000 millis, self, "stuff")
      system.scheduler.scheduleOnce(3000 millis, self, Done(elementInt))

    case OnComplete =>
      log.info("COMPLETE")

    case OnError =>
      log.warning("ERROR")

    case Done(element) =>
      inFlight -= 1
      log.info(s"Finished processing of $element")
  }
}

