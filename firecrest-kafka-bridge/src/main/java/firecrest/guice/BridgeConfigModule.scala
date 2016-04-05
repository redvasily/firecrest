package firecrest.guice

import javax.inject.Singleton

import com.google.inject.{Provides, AbstractModule}
import firecrest.{KafkaConfig, BridgeConfiguration}

class BridgeConfigModule extends AbstractModule {

  @Provides
  @Singleton
  def kafkaConfig(config: BridgeConfiguration): KafkaConfig = {
    config.kafka
  }

  override def configure(): Unit = {}
}
