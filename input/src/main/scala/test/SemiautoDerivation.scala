/*
rules = [CirceGenericExtrasMigration]
 */
package test.semiauto

import io.circe._
import io.circe.generic.extras.semiauto._
import io.circe.generic.extras.Configuration

object config {
  implicit val default: Configuration = Configuration.default
}
import config.default

case class Foo(a: Int, b: String, c: Long)
object Foo {
  implicit val circeCodec: Codec[Foo] = deriveConfiguredCodec
}

case class Bar(a: Int, b: String, c: Long)
object Bar {
  implicit val circeEncoder: Encoder[Bar] = deriveConfiguredEncoder
  implicit val circeDecoder: Decoder[Bar] = deriveConfiguredDecoder
}
