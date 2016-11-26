package firecrest

import com.fasterxml.jackson.annotation.JsonProperty
import firecrest.config.{ElasticSearchConfig, KafkaConfiguration}
import firecrest.guice.{ActorSystemModule, BridgeActorsModule, ConfigModule, ElasticSearchClientModule}
import io.dropwizard.setup.{Bootstrap, Environment}
import io.dropwizard.{Application, Configuration}
import ru.vyarus.dropwizard.guice.GuiceBundle
import ru.vyarus.dropwizard.guice.module.installer.feature.ManagedInstaller

class FirecrestConfiguration(
  @JsonProperty(value = "kafka", required = true)
  val kafka: KafkaConfiguration,

  @JsonProperty(value = "elasticSearch", required = true)
  val elasticSearch: ElasticSearchConfig,

  @JsonProperty(value = "bindHost", required = true)
  val bindHost: String,

  @JsonProperty(value = "tcpMessagePort", required = true)
  val tcpMessagePort: Int,

  @JsonProperty(value = "udpGraphitePort", required = true)
  val udpGraphitePort: Int,

  @JsonProperty(value = "tcpGraphitePort", required = true)
  val tcpGraphitePort: Int)

  extends Configuration {}

class FirecrestApplication extends Application[FirecrestConfiguration] {
  override def getName: String = "firecrest"

  override def initialize(bootstrap: Bootstrap[FirecrestConfiguration]): Unit = {
    super.initialize(bootstrap)

    bootstrap.getObjectMapper.registerModule(DropwizardScalaModule)

    bootstrap.addBundle(
      GuiceBundle.builder[FirecrestConfiguration]()
        .installers(classOf[ManagedInstaller])
        .modules(
          new ActorSystemModule(getName),
          new ConfigModule(),
          new BridgeActorsModule(),
          new ElasticSearchClientModule())
        .extensions(
          classOf[BridgeApplication],
          classOf[IndexerApplication])
        .build())
  }

  override def run(configuration: FirecrestConfiguration, environment: Environment) = {}
}

object FirecrestApplication extends App {
  new FirecrestApplication().run(args: _*)
}
