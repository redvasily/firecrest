package firecrest

import akka.util.ByteString
import org.elasticsearch.common.xcontent.XContentFactory
import org.joda.time.DateTime

object MetricsMessage {
  def fromGraphite(hostname: String, graphite: GraphiteMessage): MetricsMessage = {
    MetricsMessage(
      hostname = hostname,
      metricPath = graphite.metricPath,
      metricValue = graphite.metricValue,
      timestamp = graphite.timestamp)
  }
}

case class MetricsMessage(hostname: String,
                          metricPath: String,
                          metricValue: Option[Double],
                          timestamp: DateTime) {
  def formatJson(): ByteString = {
    val value = metricValue match {
      case Some(v) => v
      case _ => null
    }
    ByteString(
      XContentFactory.jsonBuilder()
        .startObject()
        .field("@timestamp", timestamp)
        .field("@version", 1)
        .field("metric", metricPath)
        .field("value", value)
        .field("HOSTNAME", hostname)
        .endObject()
        .string())
  }

}
