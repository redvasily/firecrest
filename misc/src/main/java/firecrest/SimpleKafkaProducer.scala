package firecrest

import org.apache.kafka.clients.producer.{ProducerRecord, KafkaProducer, Producer}
import java.util.Properties

object PropertyBuilder {

  /**
    * Build a java Properties instance from a Scala Map
    *
    * @param properties An immutable map of properties
    */
  def buildFromMap(properties: Map[String, Any]) =
    (new Properties /: properties) {
      case (a, (k, v)) =>
        a.put(k, v.asInstanceOf[AnyRef])
        a
    }
}

object SimpleKafkaProducer {
  def main(args: Array[String]) {

    println("Starting producer")

    //  var props = new Properties()
    //  props.put("bootstrap.servers", "localhost:9092")
    //  props.put("acks", "all")
    //  props.put("retries", 0)
    //  props.put("batch.size", 16384)
    //  props.put("linger.ms", 1)
    //  props.put("buffer.memory", 33554432)
    //  props.put(
    //    "key.serializer",
    //    "org.apache.kafka.common.serialization.StringSerializer")
    //  props.put(
    //    "value.serializer",
    //    "org.apache.kafka.common.serialization.StringSerializer")

    val props = PropertyBuilder.buildFromMap(Map[String, Any](
      "bootstrap.servers" -> "localhost:9092",
      "acks" -> "all",
      "retries" -> 0,
      "batch.size" -> 16384,
      "linger.ms" -> 1,
      "buffer.memory" -> 33554432,
      "key.serializer" -> "org.apache.kafka.common.serialization.StringSerializer",
      "value.serializer" -> "org.apache.kafka.common.serialization.StringSerializer"
//      "retries" -> 65536
    ))

    val producer = new KafkaProducer[String, String](props)
    var n = 1

    while (true) {
      Thread.sleep(1000)
      val message = s"Message $n"
      println(s"Sending: $message")
      producer.send(new ProducerRecord("firecrest-test", message))
      n += 1
    }
  }

}
