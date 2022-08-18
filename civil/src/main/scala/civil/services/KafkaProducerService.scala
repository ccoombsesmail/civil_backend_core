package civil.services

import civil.config.Config
import zio._
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.Serde


trait KafkaProducerService {
  def publish[A](message: A, userId: String, serde: Serde[Any, A]): Unit
}


case class KafkaProducerServiceLive() extends KafkaProducerService {
  val producerSettings: ProducerSettings =
    ProducerSettings(List(Config().getString("kafka.bootstrap.servers")))
      .withProperty("security.protocol", Config().getString("kafka.security.protocol"))
      .withProperty("sasl.jaas.config", Config().getString("kafka.sasl.jaas.config"))
      .withProperty("sasl.mechanism", Config().getString("kafka.sasl.mechanism"))
      .withProperty("client.dns.lookup", Config().getString("kafka.client.dns.lookup"))
      .withProperty("acks", Config().getString("kafka.acks"))

  val producer = ZLayer.fromManaged(Producer.make(producerSettings))
  val runtime = zio.Runtime.default

  override def publish[A](message: A, userId: String, serde: Serde[Any, A]): Unit = {
    println("Publishing!@#!@#!@#!@#$!@#$!@#$")
    val producerEffect = {
      Producer.produce(
        "notifications",
        userId,
        message,
        Serde.string,
        serde
      )
    }
    runtime.unsafeRun(for {
      _ <- producerEffect.provideSomeLayer(producer).mapError(e => println("ERROR", e))
    } yield ())

  }
}

object KafkaProducerService {


}

