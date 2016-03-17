package io.bluejay.actors

import java.net.InetSocketAddress

import akka.actor.{Props, Actor}
import akka.event.Logging
import akka.io.{IO, Tcp}
import akka.util.ByteString

import scala.annotation.tailrec

object TcpListener {
  case class Line(val data: ByteString)
}

class TcpListener extends Actor {
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
      val handler = context.actorOf(Props[ConnectionHandler])
      val connection = sender()
      connection ! Register(handler)
  }
}

class ConnectionHandler extends Actor {
  import Tcp._
  import TcpListener.Line
  import io.bluejay.util.ByteStringUtils.splitLines

  val log = Logging(context.system, this)
  var buffer = ByteString.empty

  def receive = {
    case Received(data) =>
      //      sender() ! Write(data)
      log.info("Received: {}", data.utf8String)
      val (lines, remainder) = splitLines(data)

      lines.size match {
        case 0 =>
          // there were no new lines
          buffer ++= remainder
        case _ =>
          // there were some lines
          context.parent ! Line(buffer ++ lines.head)
          lines.tail foreach { line => context.parent ! Line(line)}
          buffer = remainder
      }

    case PeerClosed =>
      log.info("PeerClosed")
      context stop self
  }
}