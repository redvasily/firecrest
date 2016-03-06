package io.bluejay

import java.io.ByteArrayOutputStream

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule

object JsonTest extends App {
  println("Started")

  val msg = Message(message = "Some message")

  val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)
  mapper.registerModule(new JodaModule())

  val out = new ByteArrayOutputStream()

  mapper.writeValue(out, msg)
  println(s"json: ${out.toString}")
  println(msg)

  println("Done")
}
