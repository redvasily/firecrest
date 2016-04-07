package firecrest.guice

import java.net.InetAddress
import javax.inject.Inject

import com.google.inject.AbstractModule
import firecrest.ElasticSearchConfig
import org.elasticsearch.client.support.AbstractClient
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress

class ElasticSearchClientModule @Inject() (esConfig: ElasticSearchConfig)
  extends AbstractModule {

  override def configure(): Unit = {
    val esClient: TransportClient = TransportClient.builder().build()
      .addTransportAddress(
        new InetSocketTransportAddress(
          InetAddress.getByName(esConfig.host),
          esConfig.port))

    bind(classOf[AbstractClient]).toInstance(esClient)
  }
}
