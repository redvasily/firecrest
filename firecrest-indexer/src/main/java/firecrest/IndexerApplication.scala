package firecrest

import javax.inject.Inject

import akka.actor.{ActorSystem, Props}
import firecrest.actors.KafkaInputActor
import io.dropwizard.lifecycle.Managed
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration._

class IndexerApplication @Inject()(system: ActorSystem) extends Managed {

  val logger = LoggerFactory.getLogger(getClass)

  override def stop(): Unit = {
    logger.info("Stop")
    //    actor.foreach(system.stop)
    system.terminate()
    val res = Await.result(system.whenTerminated, 1000 millis)
    logger.info("{}", res)
  }

  override def start(): Unit = {
    logger.info("Start")
    system.actorOf(Props[KafkaInputActor], "kafka-input")
  }
}
