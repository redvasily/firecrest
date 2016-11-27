package firecrest

import javax.inject.Inject

import akka.actor.ActorSystem
import akkaguiceutils.GuiceExtension
import firecrest.actors.{KafkaInputActor, SupervisorActor}
import io.dropwizard.lifecycle.Managed
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration._

class FirecrestManaged @Inject()(system: ActorSystem) extends Managed {

  val log = LoggerFactory.getLogger(getClass)

  override def stop(): Unit = {
    log.info("Stop")
    system.terminate()
    val res = Await.result(system.whenTerminated, 1000.millis)
  }

  override def start(): Unit = {
    log.info("Start")
    val guiceExtension = GuiceExtension.get(system)
    system.actorOf(
      guiceExtension.props(classOf[KafkaInputActor]),
      "kafka-input")
    system.actorOf(guiceExtension.props(classOf[SupervisorActor]),
      "supervisor")
  }
}
