package firecrest.actors

import akka.actor.Actor
import akka.event.Logging

import scala.concurrent.duration._

class DummyActor extends Actor {
  import context._
  val log = Logging(context.system, this)


  override def preStart() = {
    system.scheduler.scheduleOnce(1000 millis, self, "tick")
  }

  override def postRestart(reason: Throwable) = {}

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason, message)
    log.info("preRestart")
  }

  override def postStop(): Unit = {
    log.info("postStop")
  }

  override def receive = {
    case "tick" =>
      log.info("Tick")
      system.scheduler.scheduleOnce(3000 millis, self, "tick")
  }
}
