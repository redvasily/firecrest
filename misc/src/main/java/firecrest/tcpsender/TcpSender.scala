package firecrest.tcpsender

import java.net.InetSocketAddress

import akka.actor._
import akka.io.Tcp.{ConnectionClosed, CommandFailed, Write}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.{SerializationFeature, ObjectMapper}
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.joda.time.DateTime

import scala.concurrent.duration._

object TcpSender extends App {

  case class LogEntry(@JsonProperty("@timestamp") timestamp: DateTime,
                      @JsonProperty("@version") version: Int = 1,
                      @JsonProperty("HOSTNAME") hostname: String = "myhostname",
                      level: String = "INFO",
                      @JsonProperty("level_value") levelValue: Int = 20000,
                      @JsonProperty("logger_name") loggerName: String = "com.example.Stuff",
                      message: String = "",
                      @JsonProperty("thread_name") threadName: String = "main")

  println("Started")
  val system = ActorSystem()
  val remote = new InetSocketAddress("localhost", 9125)
  system.actorOf(Props.create(classOf[Client], remote), "top")
  println("Main done")
}

class Client(remote: InetSocketAddress) extends Actor with ActorLogging {

  import Tcp._
  import context.system
  import context._

  IO(Tcp) ! Connect(remote)

  system.scheduler.scheduleOnce(1000 millis, self, "tick")

  override def receive = {
    case CommandFailed(_: Connect) =>
      log.error("Connect failed")
      context.stop(self)
      system.terminate()

    case "tick" =>
      system.scheduler.scheduleOnce(1000 millis, self, "tick")
      log.info("tick")

    case c @ Connected(remote, local) =>
      log.info("Connected: {}", c)
      val connection = sender()
      val handler = context.actorOf(Props.create(classOf[ConnectionHandler], connection),
        "connection")
      context.watch(handler)
      connection ! Register(handler)

    case _: Terminated =>
      log.info("Terminated!")
      system.terminate()

    case whatever =>
      log.info(s"Got a whatever message: ${whatever}")
  }
}

class ConnectionHandler(connection: ActorRef) extends Actor with ActorLogging {

  import context._

  var counter = 0

  val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)
  mapper.registerModule(new JodaModule())
  mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

  system.scheduler.scheduleOnce(1000 millis, self, "tick")

  override def receive = {
    case "tick" =>
      system.scheduler.scheduleOnce(1000 millis, self, "tick")
      val logEntry = TcpSender.LogEntry(
        timestamp = DateTime.now(), message = s"Message $counter")
      val message: String = mapper.writeValueAsString(logEntry)
      log.info(s"Sending $message")
      connection ! Write(ByteString(message + "\n"))
      counter += 1

    case CommandFailed(w: Write) =>
      log.error("Write failed")
      context.stop(self)

    case _: ConnectionClosed =>
      log.warning("Connection closed")
      context.stop(self)
  }
}