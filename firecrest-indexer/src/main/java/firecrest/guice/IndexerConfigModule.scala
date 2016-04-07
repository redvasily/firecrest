package firecrest.guice

import javax.inject.Singleton

import com.google.inject.{Provides, AbstractModule}
import firecrest.{ElasticSearchConfig, KafkaConfigIndexer, IndexerConfiguration}

class IndexerConfigModule extends AbstractModule {

  @Provides
  @Singleton
  def elasticSearchConfig(config: IndexerConfiguration): ElasticSearchConfig = {
    config.elasticSearch
  }

  @Provides
  @Singleton
  def kafkaConfig(config: IndexerConfiguration): KafkaConfigIndexer = {
    config.kafka
  }

  override def configure(): Unit = {}
}
