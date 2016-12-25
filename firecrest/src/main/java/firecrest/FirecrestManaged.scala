package firecrest

import javax.inject.Inject

import akka.actor.ActorSystem
import akkaguiceutils.GuiceExtension
import firecrest.actors.TopActor
import io.dropwizard.lifecycle.Managed
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration._

class FirecrestManaged @Inject()(
  system: ActorSystem,
  config: FirecrestConfiguration) extends Managed {

  private val log = LoggerFactory.getLogger(getClass)

  override def stop(): Unit = {
    log.info("Stop")
    system.terminate()
    val res = Await.result(system.whenTerminated, 1000.millis)
  }

  override def start(): Unit = {
    log.info("Start")
    system.actorOf(GuiceExtension.get(system).props(classOf[TopActor]), "top")
  }
}
