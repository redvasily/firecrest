package firecrest.actors

import akka.actor._
import scala.concurrent.duration._

class SupervisorActor extends Actor with ActorLogging {
  import context._

  val kafkaSink = actorOf(Props[KafkaGraphActor], "kafka-graph")
  system.actorOf(Props(classOf[TcpListener], kafkaSink), "listener")

  override def preStart() = {
    system.scheduler.scheduleOnce(1000 millis, self, "tick")
  }

  override def postRestart(reason: Throwable) = {}

  override def receive: Receive = {
    case "tick" =>
      system.scheduler.scheduleOnce(3000 millis, self, "tick")
      log.info("Searching")
      self ! context.parent.path

    case path: ActorPath =>
      context.actorSelection(path / "*") ! Identify(())

    case identity @ ActorIdentity(_, Some(ref)) =>
      log.info(s"Actor: ${ref.path}")
      self ! ref.path
  }
}
