package firecrest.misc

import akka.actor._
import scala.concurrent.duration._

class WatchTestActor extends Actor with ActorLogging {

  import context._

  val child = actorOf(Props[Child])
  child ! "work"
  watch(child)

  system.scheduler.scheduleOnce(1 second, self, "stop")

  override def receive = {
    case "stop" =>
      stop(child)

    case term: Terminated =>
      log.info(s"Got a message: $term")
  }

}

class Child extends Actor with ActorLogging {

  log.info("Starting")

  override def receive = {
    case "work" =>
      log.info("Working...")
      Thread.sleep(10.seconds.toMillis)
      log.info("Finished working!")
  }

}


object WatchTest extends App {
  val system = ActorSystem()
  system.actorOf(Props[WatchTestActor])
}