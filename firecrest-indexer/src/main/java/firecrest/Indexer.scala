package firecrest

import com.fasterxml.jackson.annotation.JsonProperty
import firecrest.guice.{ElasticSearchClientModule, IndexerConfigModule}
import io.dropwizard.setup.{Bootstrap, Environment}
import io.dropwizard.{Application, Configuration}
import ru.vyarus.dropwizard.guice.GuiceBundle
import ru.vyarus.dropwizard.guice.module.installer.feature.ManagedInstaller

class IndexerConfiguration(@JsonProperty(value = "kafka", required = true)
                           val kafka: KafkaConfiguration,

                           @JsonProperty(value = "elasticSearch", required = true)
                           val elasticSearch: ElasticSearchConfig)
  extends Configuration {}

class Indexer extends Application[IndexerConfiguration] {
  override def getName = "firecrest-indexer"

  override def initialize(bootstrap: Bootstrap[IndexerConfiguration]): Unit = {
    super.initialize(bootstrap)

    bootstrap.getObjectMapper.registerModule(DropwizardScalaModule)

    bootstrap.addBundle(
      GuiceBundle.builder[IndexerConfiguration]()
        .installers(classOf[ManagedInstaller])
        .modules(
          new ActorSystemModule(getName),
          new IndexerConfigModule(),
          new ElasticSearchClientModule())
        .extensions(classOf[IndexerApplication])
        .build())
  }

  override def run(configuration: IndexerConfiguration,
                   environment: Environment): Unit = {}
}

object Indexer extends App {
  new Indexer().run(args: _*)
}