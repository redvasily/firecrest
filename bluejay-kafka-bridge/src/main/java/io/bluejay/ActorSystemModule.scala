package io.bluejay

import javax.inject.Inject

import akka.actor.ActorSystem
import com.google.inject.{Provides, AbstractModule}

class ActorSystemModule(name: String) extends AbstractModule {
  @Provides def actorSystem(): ActorSystem = {
    ActorSystem(name)
  }

  override def configure(): Unit = {}
}
