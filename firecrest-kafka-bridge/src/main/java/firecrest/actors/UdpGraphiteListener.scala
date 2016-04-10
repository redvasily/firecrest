package firecrest.actors

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.io.{IO, Udp}
import akka.util.ByteString
import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import firecrest.BridgeConfiguration
import org.elasticsearch.common.xcontent.XContentFactory
import org.joda.time.DateTime

object UdpGraphiteListener {
  trait Factory {
    def create(receiver: ActorRef): UdpGraphiteListener
  }

  case class GraphiteMessage(metricPath: String,
                             metricValue: Double,
                             timestamp: DateTime) {

    override def toString = s"" +
      s"GraphiteMessage{metricPath=$metricPath, " +
      s"metricValue=$metricValue, " +
      s"timestamp=$timestamp}"
  }
}

class UdpGraphiteListener @Inject() (config: BridgeConfiguration,
                                     @Assisted receiver: ActorRef)
  extends Actor with ActorLogging {

  import UdpGraphiteListener._
  import context.system

  IO(Udp) ! Udp.Bind(
    self, new InetSocketAddress(config.bindHost, config.udpGraphitePort))

  var socket: Option[ActorRef] = None

  override def receive = {
    case msg @ Udp.Bound(local) =>
      log.info(s"Bound: $msg")
      socket = Some(sender())
      context.become(ready(sender()))
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason, message)
    socket.foreach(_ ! Udp.Unbind)
  }

  def ready(socket: ActorRef): Receive = {
    case received @ Udp.Received(data, remote) =>
      println(s"Received ${data.utf8String.trim} from $remote")
      parseMessage(data) match {
        case Some(graphiteMessage) =>
          log.info(s"Processing a following graphite message: $graphiteMessage")
          val formatted = formatMessage(graphiteMessage, remote.getHostName)
          log.info(s"Formatted message: $formatted")
          receiver ! formatted
        case _ =>
          log.info("Error parsing a message")
      }

    case msg @ Udp.Unbind =>
      println(s"Udp.Unbind: $msg")
      socket ! Udp.Unbind

    case msg @ Udp.Unbound =>
      println(s"Udp.Unbound: $msg")
      throw new RuntimeException("Witness me!")
  }

  def parseDouble(s: String) = try { Some(s.toDouble) } catch { case _ => None }

  def parseLong(s: String) = try { Some(s.toLong) } catch { case _ => None }

  private def parseMessage(data: ByteString): Option[GraphiteMessage] = {
    val dataString = data.utf8String
    val parts = dataString.split(' ')
    log.info(s"Parts: ${parts.toVector}")
    if (parts.length == 3) {
      val metric = parts(0)
      val value = parseDouble(parts(1))
      val timestamp = parseLong(parts(2).trim)
      log.info(s"metric: $metric value: $value timestamp: $timestamp")
      (value, timestamp) match {
        case (Some(valueDouble), Some(timestampLong)) =>
          Some(GraphiteMessage(
            metricPath = metric,
            metricValue = valueDouble,
            timestamp = new DateTime(timestampLong * 1000L)
          ))
        case _ =>
          None
      }
    } else {
      None
    }
  }

  private def formatMessage(graphiteMessage: GraphiteMessage,
                            hostname: String): ByteString = {
    ByteString(
      XContentFactory.jsonBuilder()
        .startObject()
        .field("@timestamp", graphiteMessage.timestamp)
        .field("@version", 1)
        .field("metric", graphiteMessage.metricPath)
        .field("value", graphiteMessage.metricValue)
        .field("HOSTNAME", hostname)
        .endObject()
        .string())
  }
}
