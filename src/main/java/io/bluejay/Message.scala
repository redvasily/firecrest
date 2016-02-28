package io.bluejay

import com.github.nscala_time.time.Imports._

object Message {
  val defaultDateTime = new DateTime(0)
  val defaultSource = "unknown"
  val defaultNode = "unknown"
}

case class Message(source: String = Message.defaultSource,
                   node: String = Message.defaultNode,
                   message: String,
                   severity: String = "info",
                   datetime: DateTime = Message.defaultDateTime) {
  override def toString = s"${this.getClass.getSimpleName}{" +
    s"source='$source', " +
    s"node='$node', " +
    s"message='$message', " +
    s"severity='$severity', " +
    s"datetime=$datetime}"
}