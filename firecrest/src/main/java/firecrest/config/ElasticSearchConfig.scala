package firecrest.config

import com.fasterxml.jackson.annotation.JsonProperty


case class ElasticSearchConfig(@JsonProperty(value = "host", required = true)
                               host: String,

                               @JsonProperty(value = "port", required = true)
                               port: Int)