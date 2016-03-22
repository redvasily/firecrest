package firecrest.actors

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import akka.io.{IO, Tcp}
import akka.util.ByteString

class TcpListener(receiver: ActorRef) extends Actor {
  import Tcp._
  import context.system

  val log = Logging(context.system, this)

  IO(Tcp) ! Bind(self, new InetSocketAddress("localhost", 9125))

  override def receive = {
    case b @ Bound(localAddress) =>
      log.info("Bound: {}", b)

    case c @ CommandFailed(_: Bind) =>
      log.error("CommandFailed: {}", c)
      context.stop(self)

    case c @ Connected(remote, local) =>
      log.info("Connected: {}", c)
      val handler = context.actorOf(Props(classOf[ConnectionHandler], receiver))
      val connection = sender()
      connection ! Register(handler)
  }
}

class ConnectionHandler(receiver: ActorRef) extends Actor {
  import Tcp._
  import firecrest.util.ByteStringUtils.splitLines

  val log = Logging(context.system, this)
  var buffer = ByteString.empty

  def receive = {
    case Received(data) =>
      //      sender() ! Write(data)
      val (lines, remainder) = splitLines(data)

      lines.size match {
        case 0 =>
          // there were no new lines
          buffer ++= remainder
        case _ =>
          // there were some lines
          receiver ! buffer ++ lines.head
          lines.tail foreach { line => receiver ! line }
          buffer = remainder
      }

    case PeerClosed =>
      log.info("PeerClosed")
      context stop self
  }
}