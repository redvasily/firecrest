package firecrest.actors

import java.io.IOException
import javax.inject.Inject

import akka.actor.{Actor, ActorLogging, Props}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
import akka.stream.actor.{ActorSubscriber, ActorSubscriberMessage, MaxInFlightRequestStrategy}
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import firecrest.IndexNames
import org.elasticsearch.client.support.AbstractClient
import org.joda.time.DateTime

object EsActorSubscriber {
  case class Batch(id: Long, messages: Seq[String]) {
    override def toString(): String = {
      s"Batch{id=$id, length=${messages.size}, head=${messages.head}"

    }
  }
  case class Done()
}

class EsActorSubscriber @Inject() (client: AbstractClient, indexNames: IndexNames)
  extends ActorSubscriber with ActorLogging {

  import ActorSubscriberMessage._
  import EsActorSubscriber._

  var activeRequests = 0
  val maxConcurrentRequests = 8

  override val requestStrategy =
    new MaxInFlightRequestStrategy(max = maxConcurrentRequests) {
      override def inFlightInternally: Int = activeRequests
    }

  val objectMapper = new ObjectMapper()

  private val router = {
    val routees = Vector.fill(maxConcurrentRequests) {
      ActorRefRoutee(context.actorOf(
        Props.create(classOf[EsIndexWorker], "@timestamp", client, objectMapper, indexNames)))
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  override def receive = {
    case msg : OnNext =>
      val batch = msg.element.asInstanceOf[Batch]
      activeRequests += 1
      log.info(s"Sending a request to a worker: $batch. activeRequests: $activeRequests")
      router.route(batch, self)

    case _: Done =>
      activeRequests -= 1
      log.info(s"Received a worker response. activeRequests: $activeRequests")
  }
}

// FIXME: shouldn't this actor has a special "blocking" dispatcher
// FIXME: also we need to inject stuff directly instead of passing them through a parent
class EsIndexWorker(
  timestampField: String, client: AbstractClient, mapper: ObjectMapper, indexNames: IndexNames)
  extends Actor with ActorLogging {

  import EsActorSubscriber._

  private val timestampPath = "/" + timestampField

  @throws[Exception](classOf[Exception])
  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    log.error(reason, "Restarting: {}", message)
    super.preRestart(reason, message)
  }

  override def receive = {
    case batch: Batch =>

      log.info(s"Processing a batch: $batch")

      try {
        val bulkRequest = client.prepareBulk()

        for (msg <- batch.messages) {
          indexName(msg) match {
            case Some(index) =>
              bulkRequest.add(client
                .prepareIndex(index, "message")
                .setSource(msg))

            case None =>
              log.warning(s"Unable to compute index name for '{}'", msg)
          }
        }
        val bulkResponse = bulkRequest.get()

        if (bulkResponse.hasFailures) {
          log.error(s"Elasticsearch bulk request failures" +
            s": ${bulkResponse.buildFailureMessage()}")
        }
      } finally {
        sender ! Done()
      }
  }

  def indexName(message: String): Option[String] = {
    val dateTimeText: Option[String] = try {
      val root = mapper.readTree(message)
      Some(root.at(timestampPath).asText())
    } catch {
      case _: JsonProcessingException | _: IOException =>
        None
    }
    val timestamp = dateTimeText flatMap {text =>
      try {
        Some(DateTime.parse(text))
      } catch {
        case _: IllegalArgumentException =>
          None
      }}
    timestamp.map(indexNames.indexName)
  }
}
