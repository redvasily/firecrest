package firecrest.guice

import javax.inject.Singleton

import com.google.inject.{AbstractModule, Provides}
import firecrest.config.{ElasticSearchConfig, KafkaConfiguration}
import firecrest.FirecrestConfiguration

class ConfigModule extends AbstractModule {

  @Provides
  @Singleton
  def kafkaConfig(config: FirecrestConfiguration): KafkaConfiguration = {
    config.kafka
  }

  @Provides
  @Singleton
  def elasticSearchConfig(config: FirecrestConfiguration): ElasticSearchConfig = {
    config.elasticSearch
  }

  override def configure(): Unit = {}
}
