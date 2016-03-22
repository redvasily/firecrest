package firecrest

import io.dropwizard.setup.{Bootstrap, Environment}
import io.dropwizard.{Application, Configuration}
import ru.vyarus.dropwizard.guice.GuiceBundle
import ru.vyarus.dropwizard.guice.module.installer.feature.ManagedInstaller


class BridgeConfiguration extends Configuration {}

class Bridge extends Application[BridgeConfiguration] {
  override def getName: String = "firecrest-bridge"

  override def initialize(bootstrap: Bootstrap[BridgeConfiguration]): Unit = {
    super.initialize(bootstrap)

    bootstrap.addBundle(
      GuiceBundle.builder[BridgeConfiguration]()
        .installers(classOf[ManagedInstaller])
        .modules(new ActorSystemModule(getName))
        .extensions(classOf[BridgeApplication])
        .build())
  }

  override def run(t: BridgeConfiguration, environment: Environment): Unit = {}
}

object Bridge extends App {
  new Bridge().run(args: _*)
}
