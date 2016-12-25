package firecrest

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

class IndexNames {

  private val prefix = "log-"
  private val indexNameFormatter = DateTimeFormat.forPattern("YYYY-MM-dd")

  def indexName(timestamp: DateTime): String = {
    prefix + indexNameFormatter.print(timestamp)
  }

  def parseIndexName(indexName: String): Option[DateTime] = {
    val prefixStr = indexName.substring(0, prefix.length)
    if (prefixStr == prefix) {
      val timestampStr = indexName.substring(prefix.length)
      try {
        Some(DateTime.parse(timestampStr))
      } catch {
        case _: IllegalArgumentException =>
          None
      }
    } else {
      None
    }
  }

  def isValidName(indexName: String): Boolean =
    parseIndexName(indexName) match {
      case Some(_) => true
      case _ => false
    }
}
