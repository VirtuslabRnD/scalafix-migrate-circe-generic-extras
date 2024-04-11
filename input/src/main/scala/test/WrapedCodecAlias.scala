/*
rules = [CirceGenericExtrasMigration]
*/
package test

import io.circe.generic.extras.semiauto.deriveUnwrappedCodec
import io.circe.generic.extras.semiauto.{deriveUnwrappedEncoder, deriveUnwrappedDecoder}
import io.circe.{Codec => CirceCodec}
import io.circe.{Encoder => CirceEncoder, Decoder => CirceDecoder}

case class Limit(value: Int) extends AnyVal
object Limit {
  implicit val circeCodec: CirceCodec[Limit] = deriveUnwrappedCodec
}

case class Offset(value: Int) extends AnyVal
object Offset {
  implicit val circeEncoder: CirceEncoder[Offset] = deriveUnwrappedEncoder
  implicit val circeDecoder: CirceDecoder[Offset] = deriveUnwrappedDecoder
}