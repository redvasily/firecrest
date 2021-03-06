package firecrest.util

import akka.util.ByteString

import scala.annotation.tailrec

object ByteStringUtils {
  def splitLines(data: ByteString): (Seq[ByteString], ByteString) = {
    @tailrec
    def splitLinesAux(data: ByteString, acc: Seq[ByteString]): (Seq[ByteString], ByteString) =
      data.indexOf('\n') match {
        case -1 =>
          (acc, data)
        case index =>
          val (head, tail) = data.splitAt(index)
          splitLinesAux(tail.slice(1, tail.length), acc :+ head)
      }
    splitLinesAux(data, Vector.empty[ByteString])
  }
}