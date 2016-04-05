package firecrest.actors

import akka.actor._
import akkaguiceutils.GuiceUtils
import scala.concurrent.duration._
import scala.compat.java8.FunctionConverters._

class SupervisorActor extends Actor
  with ActorLogging
  with GuiceUtils {

  import context._

  val kafkaSink = actorOf(props(classOf[KafkaOutputActor]), "kafka-graph")

  val listenerActor = actorOf(
    props(
      classOf[TcpListener],
      classOf[TcpListener.Factory],
      ((factory: TcpListener.Factory) => factory.create(kafkaSink)).asJava),
    "listener")

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
