package firecrest

import java.io.ByteArrayOutputStream

import com.fasterxml.jackson.databind.ObjectMapper
import org.elasticsearch.client.support.AbstractClient

class EsSink(val objectMapper: ObjectMapper, val client: AbstractClient) {
  def index(messages: Seq[Message]): Unit = {

    val bulkRequest = messages.foldLeft(client.prepareBulk()) {
      (bulk, message) =>
        bulk.add(client
          .prepareIndex("messages", "message")
          .setSource(serialize(message)))
    }

    val bulkResponse = bulkRequest.get()

    if (bulkResponse.hasFailures) {
      println(s"FAILURES: ${bulkResponse.buildFailureMessage()}")
    }
  }

  private[this] def serialize(message: Message): Array[Byte] = {
    val out = new ByteArrayOutputStream()
    objectMapper.writeValue(out, message)
    out.toByteArray
  }
}
