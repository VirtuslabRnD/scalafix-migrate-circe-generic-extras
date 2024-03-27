package io.circe.generic.extras.test

import cats.kernel.Eq
import io.circe.{ Decoder, Encoder }
import io.circe.literal._
import io.circe.testing.CodecTests
import org.scalacheck.{ Arbitrary, Gen }
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Prop.forAll
import io.circe.generic.extras.test.CirceSuite
import io.circe.derivation.{ Configuration, ConfiguredCodec }

object ConfiguredJsonCodecWithKeySuite {
  implicit val customConfig: Configuration =
    Configuration.default.withSnakeCaseMemberNames.withDefaults.withDiscriminator("type").withSnakeCaseConstructorNames

  sealed trait ConfigExampleBase derives ConfiguredCodec
  case class ConfigExampleFoo(thisIsAField: String, a: Int = 0, b: Double) extends ConfigExampleBase

  object ConfigExampleFoo {
    implicit val eqConfigExampleFoo: Eq[ConfigExampleFoo] = Eq.fromUniversalEquals
    val genConfigExampleFoo: Gen[ConfigExampleFoo] = for {
      thisIsAField <- arbitrary[String]
      a <- arbitrary[Int]
      b <- arbitrary[Double]
    } yield ConfigExampleFoo(thisIsAField, a, b)
    implicit val arbitraryConfigExampleFoo: Arbitrary[ConfigExampleFoo] = Arbitrary(genConfigExampleFoo)
    given ConfiguredCodec[ConfigExampleFoo] = ConfiguredCodec.derived(using summon[Configuration].withTransformMemberNames {
  case "b" =>
    "myField"
  case name =>
    summon[Configuration].transformMemberNames(name)
})
  }

  object ConfigExampleBase {
    implicit val eqConfigExampleBase: Eq[ConfigExampleBase] = Eq.fromUniversalEquals
    val genConfigExampleBase: Gen[ConfigExampleBase] =
      ConfigExampleFoo.genConfigExampleFoo
    implicit val arbitraryConfigExampleBase: Arbitrary[ConfigExampleBase] = Arbitrary(genConfigExampleBase)
  }
}

class ConfiguredJsonCodecWithKeySuite extends CirceSuite {
  import ConfiguredJsonCodecWithKeySuite._

  checkAll("Codec[ConfigExampleBase]", CodecTests[ConfigExampleBase].codec)

  property("ConfiguredJsonCodec should support key annotation and configuration") {
    forAll { (f: String, b: Double) =>
      val foo: ConfigExampleBase = ConfigExampleFoo(f, 0, b)
      val json = json"""{ "type": "config_example_foo", "this_is_a_field": $f, "myField": $b}"""
      val expected = json"""{ "type": "config_example_foo", "this_is_a_field": $f, "a": 0, "myField": $b}"""

      assert(Encoder[ConfigExampleBase].apply(foo) === expected)
      assert(Decoder[ConfigExampleBase].decodeJson(json) === Right(foo))
    }
  }
}
