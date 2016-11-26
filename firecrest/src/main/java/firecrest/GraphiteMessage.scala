package firecrest

import akka.util.ByteString
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

object GraphiteMessage {
  private val log = LoggerFactory.getLogger(this.getClass)

  private def parseDouble(s: String) =
    try { Some(s.toDouble) } catch { case _: Throwable => None }

  private def parseLong(s: String) =
    try { Some(s.toLong) } catch { case _: Throwable => None }

  def parse(data: ByteString): Option[GraphiteMessage] = {
    val dataString = data.utf8String
    val parts = dataString.split(' ')
    if (parts.length == 3) {
      val metric = parts(0)
      val value = parseDouble(parts(1))
      val timestamp = parseLong(parts(2).trim)
      timestamp match {
        case Some(timestampLong) =>
          Some(GraphiteMessage(
            metricPath = metric,
            metricValue = value,
            timestamp = new DateTime(timestampLong * 1000L)
          ))
        case _ =>
          None
      }
    } else {
      None
    }
  }

}

case class GraphiteMessage(metricPath: String,
                           metricValue: Option[Double],
                           timestamp: DateTime) {

  override def toString = s"" +
    s"GraphiteMessage{metricPath=$metricPath, " +
    s"metricValue=$metricValue, " +
    s"timestamp=$timestamp}"
}