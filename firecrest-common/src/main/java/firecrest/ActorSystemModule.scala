package firecrest

import akka.actor.ActorSystem
import com.google.inject.{AbstractModule, Provides}

class ActorSystemModule(name: String) extends AbstractModule {
  @Provides def actorSystem(): ActorSystem = {
    ActorSystem(name)
  }

  override def configure(): Unit = {}
}
