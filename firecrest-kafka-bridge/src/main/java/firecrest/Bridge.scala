package firecrest

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.scala._
import com.fasterxml.jackson.module.scala.deser.{UntypedObjectDeserializerModule, ScalaNumberDeserializersModule}
import com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule
import com.fasterxml.jackson.module.scala.modifiers.EitherModule
import firecrest.guice.{BridgeConfigModule, BridgeActorsModule}
import io.dropwizard.setup.{Bootstrap, Environment}
import io.dropwizard.{Application, Configuration}
import ru.vyarus.dropwizard.guice.GuiceBundle
import ru.vyarus.dropwizard.guice.module.installer.feature.ManagedInstaller

class BridgeConfiguration(@JsonProperty(value = "bindHost", required = true)
                          val bindHost: String,

                          @JsonProperty(value = "tcpMessagePort", required = true)
                          val tcpMessagePort: Int,

                          @JsonProperty(value = "udpGraphitePort", required = true)
                          val udpGraphitePort: Int,

                          @JsonProperty(value = "tcpGraphitePort", required = true)
                          val tcpGraphitePort: Int,

                          @JsonProperty(value="kafka", required = true)
                          val kafka: KafkaConfiguration,

                          @JsonProperty(value = "elasticSearch", required = true)
                          val elasticSearch: ElasticSearchConfig)
  extends Configuration {}

class Bridge extends Application[BridgeConfiguration] {
  override def getName: String = "firecrest-bridge"

  override def initialize(bootstrap: Bootstrap[BridgeConfiguration]): Unit = {
    super.initialize(bootstrap)

    bootstrap.getObjectMapper.registerModule(DropwizardScalaModule)

    bootstrap.addBundle(
      GuiceBundle.builder[BridgeConfiguration]()
        .installers(classOf[ManagedInstaller])
        .modules(
          new ActorSystemModule(getName),
          new BridgeActorsModule(),
          new BridgeConfigModule())
        .extensions(classOf[BridgeApplication])
        .build())
  }

  override def run(t: BridgeConfiguration, environment: Environment): Unit = {}
}

object Bridge extends App {
  new Bridge().run(args: _*)
}
