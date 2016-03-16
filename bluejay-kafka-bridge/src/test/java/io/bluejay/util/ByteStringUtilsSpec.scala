package io.bluejay.util

import akka.util.ByteString
import org.scalatest.prop.PropertyChecks
import org.scalatest.{PropSpec, Matchers}


class ByteStringUtilsSpec extends PropSpec with PropertyChecks with Matchers {
  def splitLines(data: ByteString) = ByteStringUtils.splitLines(data)
  val newLine = ByteString(10)

  implicit override val generatorDrivenConfig = PropertyCheckConfig(
    minSuccessful = 1000,
    minSize = 1)

  def concat(lines: Seq[ByteString], rest: ByteString): ByteString = {
    (lines :+ rest).reduce((a, b) => a ++ newLine ++ b)
  }

  def mapInput(in: Vector[Byte]): ByteString = {
    val c = in.map(b => if (b >=0) 'x'.toByte else '\n'.toByte)
    ByteString(c.toArray)
  }

  property("Concatenated result is equal to the input") {
    forAll { (in: Vector[Byte]) =>
      val input = mapInput(in)
      val (lines, rest) = splitLines(input)
      val expected = concat(lines, rest)
      expected shouldBe input
    }
  }

  property("No newlines in the parts") {
    forAll { (in: Vector[Byte]) =>
      val input = mapInput(in)
      val (lines, rest) = splitLines(input)
      lines.foreach { l =>
        l.indexOf('\n'.toByte) shouldBe -1
      }
      rest.indexOf('\n'.toByte) shouldBe -1
    }
  }

  property("The output is the same as the input if you ignore the newlines") {
    forAll { (in: Vector[Byte]) =>
      val input = mapInput(in)
      val (lines, rest) = splitLines(input)
      val noNewlinesInput = input.filter(_ != '\n'.toByte)
      val noNewlinesResult = (lines :+ rest).reduce(_ ++ _).filter(_ != '\n'.toByte)
      noNewlinesResult shouldBe noNewlinesInput
    }
  }
}
