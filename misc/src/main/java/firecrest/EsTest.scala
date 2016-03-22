package firecrest

import java.io.ByteArrayOutputStream
import java.net.InetAddress

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import firecrest.Message
import org.elasticsearch.action.bulk.BulkRequestBuilder
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress

object EsTest extends App {
  new EsTest().run()
}

class EsTest() {
  private val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)
  mapper.registerModule(new JodaModule())

  val settings = Settings.settingsBuilder()
    .put("cluster.name", "myClusterName")
    .build()

  val client: TransportClient = TransportClient.builder().build()
    .addTransportAddress(
      new InetSocketTransportAddress(
        InetAddress.getByName("localhost"),
        9300))

  def serialize(message: Message): Array[Byte] = {
    val out = new ByteArrayOutputStream()
    mapper.writeValue(out, message)
    out.toByteArray
  }

  def run(): Unit = {
    val bulkRequest: BulkRequestBuilder = client.prepareBulk()

    for (i <- 0 to 100) {
      println(i)
      bulkRequest.add(client.prepareIndex("messages", "message")
        .setSource(serialize(Message(message = s"Message $i"))))
    }
    val bulkResponse = bulkRequest.get()

    if (bulkResponse.hasFailures) {
      println(s"Has failures: ${bulkResponse.buildFailureMessage()}")
    } else {
      println(s"There was no failures")
    }

    Thread.sleep(5000)
    client.close()
    println(s"Client closed")
  }

}
