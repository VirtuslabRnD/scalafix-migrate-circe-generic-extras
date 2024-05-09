package test.semiauto

import io.circe._
import io.circe.derivation.Configuration

object config {
  implicit val default: Configuration = Configuration.default
}
import config.default

case class Foo(a: Int, b: String, c: Long)
object Foo {
  implicit val circeCodec: Codec[Foo] = Codec.derived
}

case class Bar(a: Int, b: String, c: Long)
object Bar {
  implicit val circeEncoder: Encoder[Bar] = Encoder.derived
  implicit val circeDecoder: Decoder[Bar] = Decoder.derivedConfigured
}
