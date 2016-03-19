package io.bluejay

import javax.inject.Inject

import akka.actor.{ActorRef, Props, ActorSystem}
import akka.stream.{ClosedShape, OverflowStrategy, ActorMaterializer}
import akka.stream.scaladsl.{Sink, GraphDSL, RunnableGraph, Source}
import io.bluejay.actors.{TcpListener, DummyActor}
import io.dropwizard.lifecycle.Managed
import org.slf4j.LoggerFactory
import scala.concurrent.duration._

import scala.concurrent.Await

class BridgeApplication @Inject() (system: ActorSystem) extends Managed {
  val logger = LoggerFactory.getLogger(getClass)
  var actor = Option.empty[ActorRef]

  def buildGraph(actorSystem: ActorSystem): ActorRef = {
    implicit val system = actorSystem
    implicit val materializer = ActorMaterializer()

    val source: Source[Nothing, ActorRef] =
      Source.actorRef(16, OverflowStrategy.dropNew)

    val g: RunnableGraph[ActorRef] = RunnableGraph.fromGraph(GraphDSL.create(source) {
      implicit builder =>
        src =>
          import GraphDSL.Implicits._
          val sink = Sink.foreach(println)

          src.out ~> sink

          ClosedShape
    })

    val r: ActorRef = g.run()
    r
  }

  override def stop(): Unit = {
    logger.info("Stop")
//    actor.foreach(system.stop)
    system.terminate()
    val res = Await.result(system.whenTerminated, 1000 millis)
    logger.info("{}", res)
  }

  override def start(): Unit = {
    logger.info("Start")

    actor = Some(system.actorOf(Props[DummyActor], "ticker"))
    val graphSource = buildGraph(system)
    system.actorOf(Props(classOf[TcpListener], graphSource), "listener")
  }
}
