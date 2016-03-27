package firecrest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.datatype.joda.deser.DateTimeDeserializer
import org.joda.time.DateTime

object JacksonTest extends App {
  println("Started")

  val mapper = new ObjectMapper()
  mapper.registerModule(new JodaModule())

  val json =
    """
      |{
      |    "@timestamp": "2016-03-27T19:39:55.276+02:00",
      |    "@version": 1,
      |    "HOSTNAME": "lumpy",
      |    "level": "INFO",
      |    "level_value": 20000,
      |    "logger_name": "io.bluejay.App",
      |    "message": "Finished: wtf",
      |    "thread_name": "main"
      |}
    """.stripMargin

  val root = mapper.readTree(json)

  println(root)
  val dateTimeText = root.at("/@timestamp").asText()
  println(dateTimeText)
  val timestamp = DateTime.parse(dateTimeText)
  println(timestamp)
  println(timestamp.year.get)
}
