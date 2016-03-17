package io.bluejay.util.actors

import akka.actor.{ActorSystem, ActorRef, Props, Actor}
import akka.actor.Actor.Receive
import scala.concurrent.{Await, Future}
import akka.pattern.ask
import scala.concurrent.duration._
import akka.io.Tcp
import akka.util.{Timeout, ByteString}
import io.bluejay.actors.{TcpListener, ConnectionHandler}
import org.scalatest.{BeforeAndAfter, FlatSpec, FunSuite, Matchers}

class ConnnectionHandlerTest extends FlatSpec with Matchers {

  def withHelper(testCode: ActorRef => Any): Unit = {
    val system = ActorSystem("test")
    val helper = system.actorOf(Props[Helper])
    try {
      testCode(helper)
    } finally {
      system.terminate()
    }
  }

  def lines(helper: ActorRef): Seq[ByteString] = {
    implicit val timeout = Timeout(5 seconds)
    Await.result(helper ? "lines", timeout.duration)
      .asInstanceOf[Seq[ByteString]]
  }

  "no new lines on the input" should "produce no output" in withHelper {
    helper =>
      helper ! ByteString("foo")
      Thread.sleep(1000)
      lines(helper).size shouldBe 0
  }

  "some lines on the input" should "produce some lines" in withHelper {
    helper =>
      helper ! ByteString("foo\n")
      Thread.sleep(1000)
      val l = lines(helper)
      l.size shouldBe 1
      l shouldBe Vector(ByteString("foo"))
  }
}

class Helper extends Actor {
  import Tcp._

  var lines = Vector.empty[TcpListener.Line]
  val child = context.actorOf(Props[ConnectionHandler])

  override def receive = {
    case data: ByteString =>
      child ! new Tcp.Received(data)
    case line: TcpListener.Line =>
      lines :+= line
    case "lines" =>
      sender ! lines
  }
}
