package test.semiauto.deprecated

import io.circe._
import io.circe.derivation.Configuration
import io.circe.generic.semiauto._

object config {
  implicit val default: Configuration = Configuration.default
}
import config.default

case class Foo(a: Int, b: String, c: Long)
object Foo {
  implicit val circeCodec: Codec[Foo] = deriveCodec
}

case class Bar(a: Int, b: String, c: Long)
object Bar {
  implicit val circeEncoder: Encoder[Bar] = deriveEncoder
  implicit val circeDecoder: Decoder[Bar] = deriveDecoder
}
