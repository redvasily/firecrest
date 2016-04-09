package firecrest

import java.net.InetSocketAddress

import akka.actor.{Props, ActorSystem, ActorRef, Actor}
import akka.io.{Udp, IO}

object UdpReceive extends App {
  println("Yo")
  val system = ActorSystem()
  val actor = system.actorOf(Props[Listener])
  println("Main done")
}


class Listener extends Actor {
  println("Starting")

  import context.system

  IO(Udp) ! Udp.Bind(self, new InetSocketAddress("localhost", 6871))

  def receive = {
    case Udp.Bound(local) =>
      context.become(ready(sender()))
      println(s"Bound: $local")
  }

  def ready(socket: ActorRef): Receive = {
    case Udp.Received(data, remote) =>
      println(s"Received ${data.utf8String.trim} from $remote")
      println(s"${remote.getClass}")
      println(s"${remote.getHostName} ${remote.getHostString}")
    case Udp.Unbind =>
      println("Udp.Unbind")
      socket ! Udp.Unbind
    case Udp.Unbound =>
      println("Udp.Unbound")
      system.terminate()
  }
}