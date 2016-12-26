package firecrest.actors

import javax.inject.Inject

import akka.actor.{Actor, ActorLogging}
import firecrest.{FirecrestConfiguration, IndexNames}
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.action.admin.indices.get.GetIndexRequest
import org.elasticsearch.client.support.AbstractClient
import org.joda.time.DateTime

import scala.concurrent.duration._

class IndexScanActor @Inject() (
  client: AbstractClient,
  indexNames: IndexNames,
  config: FirecrestConfiguration) extends Actor with ActorLogging {

  import context._

  case object Tick {}
  private val interval = 1000.millis
  private val keepIndices = org.joda.time.Duration.standardDays(config.keepData.toDays)

  private var tick = system.scheduler.scheduleOnce(interval, self, Tick)

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    tick.cancel()
    super.preRestart(reason, message)
  }

  override def receive = {
    case Tick =>
      tick.cancel()
      tick = system.scheduler.scheduleOnce(interval, self, Tick)

      val indices: Array[String] = client.admin()
        .indices()
        .getIndex(new GetIndexRequest())
        .actionGet()
        .getIndices()

      val now = DateTime.now()
      val oldIndices = indices.filter(indexNames.parseIndexName(_) match {
        case Some(date) => new org.joda.time.Duration(date, now).isLongerThan(keepIndices)
        case _ => false
      })

      if (!oldIndices.isEmpty) {
        log.info("Got indices that are too old: {}", oldIndices.to[Vector])
        val toDelete = oldIndices.head
        log.info("Deleting a following index: {}", toDelete)
        val response = client.admin().indices().delete(new DeleteIndexRequest(toDelete)).actionGet()
      }
  }
}
