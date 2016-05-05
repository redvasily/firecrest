package firecrest.guice

import javax.inject.Singleton

import com.google.inject.{Provides, AbstractModule}
import firecrest.{ElasticSearchConfig, KafkaConfiguration, BridgeConfiguration}

class BridgeConfigModule extends AbstractModule {

  @Provides
  @Singleton
  def kafkaConfig(config: BridgeConfiguration): KafkaConfiguration = {
    config.kafka
  }

  @Provides
  @Singleton
  def elasticSearchConfig(config: BridgeConfiguration): ElasticSearchConfig = {
    config.elasticSearch
  }

  override def configure(): Unit = {}
}
