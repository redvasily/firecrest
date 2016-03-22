package firecrest

import java.net.InetAddress

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import com.github.nscala_time.time.Imports._
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress

object Firecrest extends App {
  println("Started")

  implicit val system = ActorSystem("firecrest")
  val materializer = ActorMaterializer()

  val client: TransportClient = TransportClient.builder().build()
    .addTransportAddress(
      new InetSocketTransportAddress(
        InetAddress.getByName("localhost"),
        9300))

  val source = Graph.build(client).run(materializer)

  for (i <- 1 to 10) {
    source ! Message(message = s"Message $i", datetime = DateTime.now())
    Thread.sleep(300)
  }

  Thread.sleep(3000)
  system.terminate()

  client.close()

//  val msg = Message(message = "Hello", datetime = DateTime.now())
//  println(msg)
//  pprint.pprintln(msg)
}
