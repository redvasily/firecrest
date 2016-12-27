package firecrest.actors

import java.net.InetSocketAddress
import javax.inject.Inject

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.Tcp._
import akka.io.{IO, Tcp}
import akka.util.ByteString
import com.google.inject.assistedinject.Assisted
import firecrest.util.ByteStringUtils._
import firecrest.{FirecrestConfiguration, GraphiteMessage, MetricsMessage}

object TcpGraphiteListener {
  trait Factory {
    def create(receiver: ActorRef): TcpGraphiteListener
  }
}

class TcpGraphiteListener @Inject() (config: FirecrestConfiguration,
                                     @Assisted receiver: ActorRef)
  extends Actor with ActorLogging {

  import context.system

  IO(Tcp) ! Bind(self, new InetSocketAddress(config.bindHost, config.tcpGraphitePort))

  override def receive = {
    case b @ Bound(localAddress) =>
      log.info("Bound: {}", b)

    case c @ CommandFailed(_: Bind) =>
      log.error("CommandFailed: {}", c)
      context.stop(self)

    case c @ Connected(remote, local) =>
      log.info("Connected: {}", c)
      val hostname = remote.getHostName
      val handler = context.actorOf(
        Props(classOf[GraphiteConnectionHandler], hostname, receiver))
      val connection = sender()
      connection ! Register(handler)
  }
}

class GraphiteConnectionHandler(hostname: String, receiver: ActorRef)
  extends Actor with ActorLogging {

  var buffer = ByteString.empty

  override def receive = {
    case Received(data) =>
      log.debug("Received some data")
      val (lines, remainder) = splitLines(data)
      lines.size match {
        case 0 =>
          // there were no new lines
          buffer ++= remainder
        case _ =>
          val firstLine = buffer ++ lines.head
          val allLines = firstLine +: lines.tail
          val graphiteMessages = allLines.flatMap(GraphiteMessage.parse)
          val metricsMessages = graphiteMessages
            .map(MetricsMessage.fromGraphite(hostname, _))
          log.debug("Messages: {}", metricsMessages)
          log.info("Received {} messages", metricsMessages.size)
          metricsMessages.foreach(receiver ! _.formatJson())
      }
    case PeerClosed =>
      log.info("PeerClosed")
      context stop self
  }
}
