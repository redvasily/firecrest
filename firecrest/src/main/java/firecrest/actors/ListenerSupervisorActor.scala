package firecrest.actors

import akka.actor._
import akkaguiceutils.GuiceUtils

import scala.concurrent.duration._
import scala.compat.java8.FunctionConverters._

class ListenerSupervisorActor extends Actor
  with ActorLogging
  with GuiceUtils {

  import context._

  val kafkaSink: ActorRef = actorOf(props(classOf[KafkaOutputActor]), "kafka-graph")

  val listenerActor: ActorRef = actorOf(
    props(
      classOf[TcpListener],
      classOf[TcpListener.Factory],
      ((factory: TcpListener.Factory) => factory.create(kafkaSink)).asJava),
    "listener")

  val graphiteListenerActor: ActorRef = actorOf(
    props(
      classOf[UdpGraphiteListener],
      classOf[UdpGraphiteListener.Factory],
      ((factory: UdpGraphiteListener.Factory) => factory.create(kafkaSink)).asJava),
    "graphiteListener")

  val graphiteListenerTcpActor: ActorRef = actorOf(
    props(
      classOf[TcpGraphiteListener],
      classOf[TcpGraphiteListener.Factory],
      ((factory: TcpGraphiteListener.Factory) => factory.create(kafkaSink)).asJava),
    "graphiteListenerTcp")

  val templateUploadActor: ActorRef = actorOf(props(classOf[TemplateUploadActor]))

  override def preStart() = {
    system.scheduler.scheduleOnce(1.second, self, "tick")
  }

  override def postRestart(reason: Throwable) = {}

  override def receive: Receive = {
    case "tick" =>
      system.scheduler.scheduleOnce(10.seconds, self, "tick")
      log.debug("Searching")
      self ! context.parent.path

    case path: ActorPath =>
      context.actorSelection(path / "*") ! Identify(())

    case identity @ ActorIdentity(_, Some(ref)) =>
      log.debug(s"Actor: ${ref.path}")
      self ! ref.path
  }
}
