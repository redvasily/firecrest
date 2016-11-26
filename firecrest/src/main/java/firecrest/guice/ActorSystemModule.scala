package firecrest.guice

import akka.actor.ActorSystem
import akkaguiceutils.GuiceExtensionId.GuiceExtProvider
import com.google.inject.{AbstractModule, Injector, Provides}

class ActorSystemModule(name: String) extends AbstractModule {
  @Provides def actorSystem(injector: Injector): ActorSystem = {
    val actorSystem = ActorSystem(name)
    GuiceExtProvider.get(actorSystem).initialize(injector)
    actorSystem
  }

  override def configure(): Unit = {}
}
