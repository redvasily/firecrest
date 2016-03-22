package firecrest.Indexer

import io.dropwizard.setup.{Environment, Bootstrap}
import io.dropwizard.{Application, Configuration}
import ru.vyarus.dropwizard.guice.GuiceBundle
import ru.vyarus.dropwizard.guice.module.installer.feature.ManagedInstaller

class IndexerConfiguration extends Configuration {}

class Indexer extends Application[IndexerConfiguration] {
  override def getName = "firecrest-indexer"

  override def initialize(bootstrap: Bootstrap[IndexerConfiguration]): Unit = {
    super.initialize(bootstrap)

    bootstrap.addBundle(
      GuiceBundle.builder[IndexerConfiguration]()
        .installers(classOf[ManagedInstaller])
        .build())
  }

  override def run(configuration: IndexerConfiguration,
                   environment: Environment): Unit = {}
}

object Indexer extends App {
  new Indexer().run(args: _*)
}