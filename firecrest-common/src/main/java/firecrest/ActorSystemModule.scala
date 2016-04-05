package firecrest

import akka.actor.ActorSystem
import com.google.inject.{Injector, AbstractModule, Provides}
import akkaguiceutils.GuiceExtensionId.GuiceExtProvider

class ActorSystemModule(name: String) extends AbstractModule {
  @Provides def actorSystem(injector: Injector): ActorSystem = {
    val actorSystem = ActorSystem(name)
    GuiceExtProvider.get(actorSystem).initialize(injector)
    actorSystem
  }

  override def configure(): Unit = {}
}
