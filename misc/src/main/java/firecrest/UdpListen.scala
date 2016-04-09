package firecrest

import java.net.InetSocketAddress

import akka.actor._
import akka.actor.Actor.Receive
import akka.io.{IO, Udp}

class UdpListen extends Actor with ActorLogging {
  import context.system

  IO(Udp) ! Udp.Bind(self, new InetSocketAddress("localhost", 9125))

  override def receive = {
    case msg @ Udp.Bound(local) =>
      log.info(s"Bound: $msg")
      context.become(ready(sender()))
  }

  def ready(socket: ActorRef): Receive = {
    case msg @ Udp.Received(data, remote) =>
      log.info(s"Got a message: $msg")

    case msg @ Udp.Unbind  =>
      log.info(s"Got a message: $msg")
      socket ! Udp.Unbind

    case msg @ Udp.Unbound =>
      log.info(s"Got a message: $msg")
      context.stop(self)
  }
}

object UdpListen extends App {
  val system = ActorSystem()
  system.actorOf(Props[UdpListen])
}