package firecrest

import javax.inject.Inject

import akka.actor.{ActorRef, ActorSystem, Props}
import akkaguiceutils.GuiceExtension
import firecrest.actors.SupervisorActor
import io.dropwizard.lifecycle.Managed
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration._

class BridgeApplication @Inject() (system: ActorSystem) extends Managed {
  val logger = LoggerFactory.getLogger(getClass)
  var actor = Option.empty[ActorRef]

  override def stop(): Unit = {
    logger.info("Stop")
    system.terminate()
    val res = Await.result(system.whenTerminated, 1 second)
    logger.info("{}", res)
  }

  override def start(): Unit = {
    logger.info("Start")
    system.actorOf(
      GuiceExtension.get(system).props(classOf[SupervisorActor]),
      "supervisor")
  }
}
