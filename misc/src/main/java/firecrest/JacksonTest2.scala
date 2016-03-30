package firecrest

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.{SerializationFeature, ObjectMapper}
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.joda.time.DateTime


case class LogEntry(@JsonProperty("@timestamp") timestamp: DateTime,
                    @JsonProperty("whatever") message: String)

object JacksonTest2 extends App {
  println("Started")

  val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)
  mapper.registerModule(new JodaModule())
  mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

  val msg = LogEntry(
    timestamp = DateTime.now(),
    message = "Howdy"
  )

  println(mapper.writeValueAsString(msg))
}

