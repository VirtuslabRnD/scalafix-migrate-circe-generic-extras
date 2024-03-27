package io.circe.generic.extras.test

import cats.kernel.Eq
import io.circe.{ Decoder, Encoder }
import io.circe.literal._
import io.circe.testing.CodecTests
import io.circe.generic.extras.test.CirceSuite
import org.scalacheck.Prop.forAll
import org.scalacheck.{ Arbitrary, Gen }
import scala.Console.in
import io.circe.derivation.{ Configuration, ConfiguredCodec, ConfiguredDecoder, ConfiguredEncoder }

object ConfiguredJsonCodecSuite {
  implicit val customConfig: Configuration =
    Configuration.default.withSnakeCaseMemberNames.withDefaults.withDiscriminator("type").withSnakeCaseConstructorNames

  sealed trait ConfigExampleBase derives ConfiguredCodec
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

  private[circe] final case class AccessModifier(a: Int) derives ConfiguredCodec

  private[circe] object AccessModifier {
    implicit def eqAccessModifier: Eq[AccessModifier] = Eq.fromUniversalEquals
    implicit def arbitraryAccessModifier: Arbitrary[AccessModifier] =
      Arbitrary(
        for {
          a <- Arbitrary.arbitrary[Int]
        } yield AccessModifier(a)
      )
  }

  case class GenericExample[A](a: A, b: Int)
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
    given [A: Encoder: Decoder]: ConfiguredCodec[GenericExample[A]] = ConfiguredCodec.derived
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
    case class CaseClassEncodeOnly(foo: String, bar: Int) derives ConfiguredEncoder
    Encoder[CaseClassEncodeOnly]
    Encoder.AsObject[CaseClassEncodeOnly]
  }

  test("@ConfiguredJsonCodec(decodeOnly = true) should provide Decoder instances") {
    case class CaseClassDecodeOnly(foo: String, bar: Int) derives ConfiguredDecoder
    Decoder[CaseClassDecodeOnly]
  }

  test("@ConfiguredJsonCodec(encodeOnly = true) should only provide Encoder instances for generic case classes") {
    case class CaseClassEncodeOnly[A](foo: A, bar: Int)
    object CaseClassEncodeOnly { given [A: Encoder]: ConfiguredEncoder[CaseClassEncodeOnly[A]] = ConfiguredEncoder.derived }
    Encoder[CaseClassEncodeOnly[Int]]
    Encoder.AsObject[CaseClassEncodeOnly[Int]]
  }

  test("@ConfiguredJsonCodec(decodeOnly = true) should provide Decoder instances for generic case classes") {
    case class CaseClassDecodeOnly[A](foo: A, bar: Int)
    object CaseClassDecodeOnly { given [A: Decoder]: ConfiguredDecoder[CaseClassDecodeOnly[A]] = ConfiguredDecoder.derived }
    Decoder[CaseClassDecodeOnly[Int]]
  }
}
