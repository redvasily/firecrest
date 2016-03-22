package firecrest.actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.io.Tcp
import akka.pattern.ask
import akka.util.{ByteString, Timeout}
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

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
      helper ! Send(ByteString("foo"))
      Thread.sleep(100)
      lines(helper).size shouldBe 0
  }

  "some lines on the input" should "produce some lines" in withHelper {
    helper =>
      val x = ByteString("foo\n")
      val y = ByteString("foo")
      helper ! Send(x)
      Thread.sleep(100)
      val l = lines(helper)
      l.size shouldBe 1
      l shouldBe Vector(ByteString("foo"))
  }
}

case class Send(data: ByteString)

class Helper extends Actor {
  val child = context.actorOf(Props(classOf[ConnectionHandler], self))
  var lines = Vector.empty[ByteString]

  override def receive = {
    case Send(data) =>
      child ! new Tcp.Received(data)
    case line: ByteString =>
      lines :+= line
    case "lines" =>
      sender ! lines
  }
}
