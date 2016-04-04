package firecrest

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.scala._
import com.fasterxml.jackson.module.scala.deser.{UntypedObjectDeserializerModule, ScalaNumberDeserializersModule}
import com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule
import com.fasterxml.jackson.module.scala.modifiers.EitherModule
import io.dropwizard.setup.{Bootstrap, Environment}
import io.dropwizard.{Application, Configuration}
import ru.vyarus.dropwizard.guice.GuiceBundle
import ru.vyarus.dropwizard.guice.module.installer.feature.ManagedInstaller

class DropwizardScalaModule
  extends JacksonModule
    with IteratorModule
    with EnumerationModule
    with OptionModule
    with SeqModule
    with IterableModule
    with TupleModule
    with MapModule
    with SetModule
    with UntypedObjectDeserializerModule
    with EitherModule
{
  override def getModuleName = "DropwizardScalaModule"
}

object DropwizardScalaModule extends DropwizardScalaModule


case class KafkaConfig(@JsonProperty(value = "host", required = true)
                       host: String,
                       @JsonProperty(value = "port", required = true)
                       port: Int)

class BridgeConfiguration(@JsonProperty(value = "tcpMessagePort", required = true)
                          val tcpMessagePort: Int,
                          @JsonProperty(value="kafka", required = true)
                          val kafka: KafkaConfig)
  extends Configuration {}

class Bridge extends Application[BridgeConfiguration] {
  override def getName: String = "firecrest-bridge"

  override def initialize(bootstrap: Bootstrap[BridgeConfiguration]): Unit = {
    super.initialize(bootstrap)

    bootstrap.getObjectMapper.registerModule(DropwizardScalaModule)

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
