package firecrest.actors

import java.net.InetSocketAddress
import javax.inject.Inject

import akka.actor.{Props, Actor, ActorLogging, ActorRef}
import akka.io.Tcp._
import akka.io.{Tcp, IO}
import akka.util.ByteString
import com.google.inject.assistedinject.Assisted
import firecrest.{MetricsMessage, GraphiteMessage, BridgeConfiguration}
import firecrest.util.ByteStringUtils._

object TcpGraphiteListener {
  trait Factory {
    def create(receiver: ActorRef): TcpGraphiteListener
  }
}

class TcpGraphiteListener @Inject() (config: BridgeConfiguration,
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
      log.info("Got some data")
      val (lines, remainder) = splitLines(data)
      lines.size match {
        case 0 =>
          // there were no new lines
          buffer ++= remainder
        case _ =>
          val firstLine = buffer ++ lines.head
          val allLines = firstLine +: lines.tail
          val graphiteMessages = allLines.flatMap(GraphiteMessage.parse(_))
          val metricsMessages = graphiteMessages
            .map(MetricsMessage.fromGraphite(hostname, _))
          log.info(s"Messages: $metricsMessages")
          metricsMessages.foreach(receiver ! _.formatJson())
      }
    case PeerClosed =>
      log.info("PeerClosed")
      context stop self
  }
}
