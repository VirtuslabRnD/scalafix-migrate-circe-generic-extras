/*
rule = CirceGenericExtrasMigration
*/
package io.circe.generic.extras.test

import cats.kernel.Eq
import io.circe.{ Decoder, Encoder }
import io.circe.literal._
import io.circe.testing.CodecTests
import io.circe.generic.extras.test.CirceSuite
import org.scalacheck.Prop.forAll
import org.scalacheck.{ Arbitrary, Gen }
import io.circe.generic.extras.{Configuration, ConfiguredJsonCodec}
import scala.Console.in

object ConfiguredJsonCodecSuite {
  implicit val customConfig: Configuration =
    Configuration.default.withSnakeCaseMemberNames.withDefaults.withDiscriminator("type").withSnakeCaseConstructorNames

  @ConfiguredJsonCodec
  sealed trait ConfigExampleBase
  case class ConfigExampleFoo(thisIsAField: String, a: Int = 0, b: Double) extends ConfigExampleBase

  object ConfigExampleFoo {
    implicit val eqConfigExampleFoo: Eq[ConfigExampleFoo] = Eq.fromUniversalEquals
    val genConfigExampleFoo: Gen[ConfigExampleFoo] = for {
      thisIsAField <- Arbitrary.arbitrary[String]
      a <- Arbitrary.arbitrary[Int]
      b <- Arbitrary.arbitrary[Double]
    } yield ConfigExampleFoo(thisIsAField, a, b)
    implicit val arbitraryConfigExampleFoo: Arbitrary[ConfigExampleFoo] = Arbitrary(genConfigExampleFoo)
  }

  object ConfigExampleBase {
    implicit val eqConfigExampleBase: Eq[ConfigExampleBase] = Eq.fromUniversalEquals
    val genConfigExampleBase: Gen[ConfigExampleBase] =
      ConfigExampleFoo.genConfigExampleFoo
    implicit val arbitraryConfigExampleBase: Arbitrary[ConfigExampleBase] = Arbitrary(genConfigExampleBase)
  }

  @ConfiguredJsonCodec private[circe] final case class AccessModifier(a: Int)

  private[circe] object AccessModifier {
    implicit def eqAccessModifier: Eq[AccessModifier] = Eq.fromUniversalEquals
    implicit def arbitraryAccessModifier: Arbitrary[AccessModifier] =
      Arbitrary(
        for {
          a <- Arbitrary.arbitrary[Int]
        } yield AccessModifier(a)
      )
  }

  @ConfiguredJsonCodec case class GenericExample[A](a: A, b: Int)
  object GenericExample {
    implicit def eqGenericExample[A: Eq]: Eq[GenericExample[A]] = Eq.instance {
      case (GenericExample(a1, b1), GenericExample(a2, b2)) => Eq[A].eqv(a1, a2) && b1 == b2
    }

    implicit def arbitraryGenericExample[A: Arbitrary]: Arbitrary[GenericExample[A]] =
      Arbitrary(
        for {
          a <- Arbitrary.arbitrary[A]
          b <- Arbitrary.arbitrary[Int]
        } yield GenericExample(a, b)
      )
  }
}

class ConfiguredJsonCodecSuite extends CirceSuite {
  import ConfiguredJsonCodecSuite._

  checkAll("Codec[ConfigExampleBase]", CodecTests[ConfigExampleBase].codec)
  checkAll("Codec[AccessModifier]", CodecTests[AccessModifier].codec)
  checkAll("Codec[GenericExample[Int]]", CodecTests[GenericExample[Int]].codec)

  test("ConfiguredJsonCodec should support configuration") {
    forAll { (f: String, b: Double) =>
      val foo: ConfigExampleBase = ConfigExampleFoo(f, 0, b)
      val json = json"""{ "type": "config_example_foo", "this_is_a_field": $f, "b": $b}"""
      val expected = json"""{ "type": "config_example_foo", "this_is_a_field": $f, "a": 0, "b": $b}"""

      assert(Encoder[ConfigExampleBase].apply(foo) === expected)
      assert(Decoder[ConfigExampleBase].decodeJson(json) === Right(foo))
    }
  }

  test("@ConfiguredJsonCodec(encodeOnly = true) should only provide Encoder instances") {
    @ConfiguredJsonCodec(encodeOnly = true) case class CaseClassEncodeOnly(foo: String, bar: Int)
    Encoder[CaseClassEncodeOnly]
    Encoder.AsObject[CaseClassEncodeOnly]
  }

  test("@ConfiguredJsonCodec(decodeOnly = true) should provide Decoder instances") {
    @ConfiguredJsonCodec(decodeOnly = true) case class CaseClassDecodeOnly(foo: String, bar: Int)
    Decoder[CaseClassDecodeOnly]
  }

  test("@ConfiguredJsonCodec(encodeOnly = true) should only provide Encoder instances for generic case classes") {
    @ConfiguredJsonCodec(encodeOnly = true) case class CaseClassEncodeOnly[A](foo: A, bar: Int)
    Encoder[CaseClassEncodeOnly[Int]]
    Encoder.AsObject[CaseClassEncodeOnly[Int]]
  }

  test("@ConfiguredJsonCodec(decodeOnly = true) should provide Decoder instances for generic case classes") {
    @ConfiguredJsonCodec(decodeOnly = true) case class CaseClassDecodeOnly[A](foo: A, bar: Int)
    Decoder[CaseClassDecodeOnly[Int]]
  }
}
