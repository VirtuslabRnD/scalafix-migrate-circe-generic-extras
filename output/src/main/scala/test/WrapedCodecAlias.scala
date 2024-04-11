package test

import io.circe.{Codec => CirceCodec}
import io.circe.{Encoder => CirceEncoder, Decoder => CirceDecoder}

case class Limit(value: Int) extends AnyVal
object Limit {
  implicit val circeCodec: CirceCodec[Limit] = CirceCodec.from(summon[CirceDecoder[Int]].map(Limit(_)), summon[CirceEncoder[Int]].contramap(_.value))
}

case class Offset(value: Int) extends AnyVal
object Offset {
  implicit val circeEncoder: CirceEncoder[Offset] = summon[CirceEncoder[Int]].contramap(_.value)
  implicit val circeDecoder: CirceDecoder[Offset] = summon[CirceDecoder[Int]].map(Offset(_))
}