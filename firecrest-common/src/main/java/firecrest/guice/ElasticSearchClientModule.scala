package firecrest.guice

import java.net.InetAddress
import javax.inject.Singleton

import com.google.inject.{AbstractModule, Provides}
import firecrest.ElasticSearchConfig
import org.elasticsearch.client.support.AbstractClient
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress

class ElasticSearchClientModule extends AbstractModule {

  @Provides
  @Singleton
  def client(esConfig: ElasticSearchConfig): AbstractClient = {
    val esClient: TransportClient = TransportClient.builder().build()
      .addTransportAddress(
        new InetSocketTransportAddress(
          InetAddress.getByName(esConfig.host),
          esConfig.port))
    esClient
  }

  override def configure(): Unit = {}
}
