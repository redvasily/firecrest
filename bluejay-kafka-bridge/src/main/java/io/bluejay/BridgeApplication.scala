package io.bluejay

import javax.inject.Inject

import akka.actor.{ActorRef, Props, ActorSystem}
import io.bluejay.actors.{TcpListener, DummyActor}
import io.dropwizard.lifecycle.Managed
import org.slf4j.LoggerFactory
import scala.concurrent.duration._

import scala.concurrent.Await

class BridgeApplication @Inject() (system: ActorSystem) extends Managed {
  val logger = LoggerFactory.getLogger(getClass)
  var actor = Option.empty[ActorRef]

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
    system.actorOf(Props[TcpListener], "listener")
  }
}
