/*
rules = [CirceGenericExtrasMigration, CirceLiteralMigration]
*/
package test

import enumeratum.EnumEntry.UpperSnakecase
import enumeratum.{CirceEnum, EnumEntry}
import io.circe.Decoder.Result
import io.circe.derivation.annotations.JsonCodec
import io.circe.derivation.annotations.{JsonCodec => JsonCodecAlias}
import io.circe.generic.extras.{ConfiguredJsonCodec, Configuration}
import io.circe.literal.JsonStringContext
import io.circe.syntax.EncoderOps

object Example1 extends App {

  // 1. CirceGenericExtrasMigration
  @JsonCodec
  case class Person(name: String, age: Int)
  println("Person: " + Person("Bob", 42).asJson)

  @JsonCodecAlias
  case class Address(line: String)

  implicit val config: Configuration = Configuration.default
  @ConfiguredJsonCodec
  case class Foo(v: Int)
  @ConfiguredJsonCodec(decodeOnly = true)
  case class FooRead(foo: Foo)
  @ConfiguredJsonCodec(encodeOnly = true)
  case class FooWrite(foo: Foo)

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
