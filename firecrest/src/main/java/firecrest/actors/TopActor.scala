package firecrest.actors

import javax.inject.Inject

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, ActorLogging, OneForOneStrategy}
import akkaguiceutils.GuiceUtils
import firecrest.FirecrestConfiguration

class TopActor @Inject() (config: FirecrestConfiguration) extends Actor
  with ActorLogging
  with GuiceUtils {

  override val supervisorStrategy =
    OneForOneStrategy() {
      case error: Throwable => {
        log.warning("A child error: {}", error)
        Restart
      }
    }

  import context._

  private val indexer = if (!config.enableIndexer) {
    log.warning("INDEXER IS DISABLED")
    Some(system.actorOf(props(classOf[IndexerActor]), "indexer"))
  } else {
    None
  }

  private val listenerSupervisor = actorOf(props(classOf[ListenerSupervisorActor]),
    "listener-supervisor")
  private val indexScan = actorOf(props(classOf[IndexScanActor]), "index-scan")


  override def receive: Receive = {
    case message =>
      log.info("Unexpected: {}", message)
  }
}
