package firecrest.actors

import java.io.{BufferedReader, InputStreamReader}
import java.util.stream.Collectors
import javax.inject.Inject

import akka.actor.{Actor, ActorLogging}
import org.elasticsearch.client.support.AbstractClient

import scala.concurrent.duration._

object TemplateUploadActor {
  case object Upload
}

class TemplateUploadActor @Inject() (elasticSearchClient: AbstractClient)
  extends Actor with ActorLogging {

  import TemplateUploadActor._
  import context._

  override def preStart(): Unit = {
    super.preStart()
    system.scheduler.scheduleOnce(1 second, self, Upload)
  }

  override def postRestart(reason: Throwable) = {}

  override def receive = {
    case Upload =>
      log.info("Uploading index template")
      system.scheduler.scheduleOnce(10 minutes, self, Upload)

      val istream = ClassLoader.getSystemResourceAsStream("index-template.json")
      val buffer = new BufferedReader(new InputStreamReader(istream))
      val content = buffer.lines().collect(Collectors.joining())

      val indicesAdminClient = elasticSearchClient.admin().indices()
      val result = indicesAdminClient.preparePutTemplate("firecrest")
        .setSource(content)
        .get()
      if (!result.isAcknowledged) {
        log.warning("Index upload is not acknowledged")
      }
  }
}
