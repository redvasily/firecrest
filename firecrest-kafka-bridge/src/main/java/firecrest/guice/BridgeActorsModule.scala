package firecrest.guice

import com.google.inject.AbstractModule
import com.google.inject.assistedinject.FactoryModuleBuilder
import firecrest.actors.{UdpGraphiteListener, TcpListener}

class BridgeActorsModule extends AbstractModule {
  override def configure(): Unit = {
    install(new FactoryModuleBuilder()
      .build(classOf[TcpListener.Factory]))
    install(new FactoryModuleBuilder()
      .build(classOf[UdpGraphiteListener.Factory]))
  }
}
