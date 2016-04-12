package firecrest.actors

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.io.{IO, Udp}
import akka.util.ByteString
import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import firecrest.{MetricsMessage, GraphiteMessage, BridgeConfiguration}
import org.elasticsearch.common.xcontent.XContentFactory
import org.joda.time.DateTime

object UdpGraphiteListener {
  trait Factory {
    def create(receiver: ActorRef): UdpGraphiteListener
  }
}

class UdpGraphiteListener @Inject() (config: BridgeConfiguration,
                                     @Assisted receiver: ActorRef)
  extends Actor with ActorLogging {

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
      GraphiteMessage.parse(data) match {
        case Some(graphiteMessage) =>
          log.info(s"Processing a following graphite message: $graphiteMessage")
          val metricsMessage = MetricsMessage.fromGraphite(
            remote.getHostName, graphiteMessage)
          val formatted = metricsMessage.formatJson()
          log.info(s"Formatted message: ${formatted.utf8String}")
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
}
