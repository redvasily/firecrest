package firecrest.config

import com.fasterxml.jackson.annotation.JsonProperty

case class KafkaConfiguration(
  @JsonProperty(value = "host", required = true)
  host: String,

  @JsonProperty(value = "port", required = true)
  port: Int,

  @JsonProperty(value = "topic", required = true)
  topic: String = "firecrest-messages")