package io.bluejay.actors

import java.net.InetSocketAddress

import akka.actor.{Props, Actor}
import akka.event.Logging
import akka.io.{IO, Tcp}
import akka.util.ByteString

import scala.annotation.tailrec

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
  val log = Logging(context.system, this)
  var buffer = ByteString.empty

  def receive = {
    case Received(data) =>
      //      sender() ! Write(data)
      log.info("Received: {}", data.utf8String)
      val (lines, remainder) = splitLines(data)
      // append first line to the existing buffer, emmit all of the lines

      buffer = remainder

    case PeerClosed =>
      log.info("PeerClosed")
      context stop self
  }

  private def splitLines(data: ByteString): (Seq[String], ByteString) = {
    splitLines(data, Vector.empty[String])
  }

  @tailrec
  private def splitLines(data: ByteString, acc: Vector[String]): (Seq[String], ByteString) = {
    data.indexOf('\n') match {
      case -1 =>
        (acc, data)

      case index =>
        val (head, tail) = data.splitAt(index)
        splitLines(tail, acc :+ head.utf8String)
    }
  }
}