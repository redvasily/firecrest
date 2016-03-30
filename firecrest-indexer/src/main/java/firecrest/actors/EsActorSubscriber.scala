package firecrest.actors

import akka.actor.{Props, Actor, ActorLogging}
import akka.routing.{RoundRobinRoutingLogic, Router, ActorRefRoutee}
import akka.stream.actor.{ActorSubscriber, ActorSubscriberMessage, MaxInFlightRequestStrategy}
import akka.util.ByteString
import com.fasterxml.jackson.databind.ObjectMapper
import org.elasticsearch.client.support.AbstractClient
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

object EsActorSubscriber {
  case class Batch(messages: Seq[ByteString])
  case class Done()
}

class EsActorSubscriber(client: AbstractClient) extends ActorSubscriber with ActorLogging {

  import ActorSubscriberMessage._
  import EsActorSubscriber._

  var activeRequests = 0
  val maxConcurrentRequests = 8

  override val requestStrategy =
    new MaxInFlightRequestStrategy(max = maxConcurrentRequests) {
      override def inFlightInternally: Int = activeRequests
    }

  val router = {
    val routees = Vector.fill(maxConcurrentRequests) {
      ActorRefRoutee(context.actorOf(Props[EsIndexWorker]))
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  override def receive = {
    case msg @ OnNext(Batch(messages)) =>
      val batch = msg.element.asInstanceOf[Batch]
      activeRequests += 1
      log.info(s"Sending a request to a worker: $batch. activeRequests: $activeRequests")
      router.route(batch, self)

    case Done =>
      activeRequests -= 1
      log.info(s"Received a worker response. activeRequests: $activeRequests")
  }
}

class EsIndexWorker(timestampField: String, client: AbstractClient, mapper: ObjectMapper)
  extends Actor with ActorLogging {

  import EsActorSubscriber._

  val timestampPath = "/" + timestampField
  val indexNameFormatter = DateTimeFormat.forPattern("log-yyyy-mm-dd")

  @throws[Exception](classOf[Exception])
  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    log.error(reason, "Restarting: {}", message)
    super.preRestart(reason, message)
  }

  override def receive = {
    case Batch(messages) =>

      log.info("Processing a batch")

      try {
        val bulkRequest = client.prepareBulk()

        for (msg <- messages) {
          indexName(msg) match {
            case Some(index) =>
              bulkRequest.add(client
                .prepareIndex(index, "message")
                .setSource(msg.toByteBuffer))

            case None =>
              log.warning(s"Unable to compute index name for '${msg.utf8String}'")
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

  def indexName(message: ByteString): Option[String] = {
    val root = mapper.readTree(message.toArray)
    val dateTimeText = root.at(timestampPath).asText()
    val timestamp = try {
      Some(DateTime.parse(dateTimeText))
    } catch {
      case IllegalArgumentException =>
        None
    }
    timestamp.map(indexNameFormatter.print(_))
  }
}