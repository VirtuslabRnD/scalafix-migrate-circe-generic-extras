package test

import enumeratum.EnumEntry.UpperSnakecase
import enumeratum.{CirceEnum, EnumEntry}
import io.circe.Decoder.Result
import io.circe.syntax.EncoderOps
import io.circe.Codec
import io.circe.derivation.{ Configuration, ConfiguredCodec, ConfiguredDecoder, ConfiguredEncoder }
import io.circe.literal.json

object Example1 extends App {

  // 1. CirceGenericExtrasMigration
  case class Person(name: String, age: Int) derives Codec.AsObject
  println("Person: " + Person("Bob", 42).asJson)

  case class Address(line: String) derives Codec.AsObject

  implicit val config: Configuration = Configuration.default
  case class Foo(v: Int) derives ConfiguredCodec
  case class FooRead(foo: Foo) derives ConfiguredDecoder
  case class FooWrite(foo: Foo) derives ConfiguredEncoder

  // 2. CirceEnumeratumMigration
  sealed trait Color extends EnumEntry with UpperSnakecase
  object Color extends enumeratum.Enum[Color] with CirceEnum[Color] {
    case object Red   extends Color
    case object Green extends Color
    case object Blue  extends Color
    val values = findValues
  }

  val color: Color = Color.Red
  println("Color: " + color.asJson)

  // 3. CirceLiteralMigration
  val decodedPerson: Result[Person] = json"""{
        "name" : "Sam",
        "age" : 70
      }""".as[Person]
  println("Decoded: " + decodedPerson)
}
