
package test

import io.circe.Codec
import io.circe.syntax.EncoderOps
import java.time.Instant
import io.circe.{ Decoder, Encoder }
import io.circe.derivation.{ Configuration, ConfiguredCodec }

object Example2 extends App {

  // 1. deriveUnwrappedCodec
  case class ThingId(value: String) extends AnyVal
  object ThingId {
    implicit val codec: Codec[ThingId] =
      Codec.from(summon[Decoder[String]].map(ThingId(_)), summon[Encoder[String]].contramap(_.value))
  }

  // 2. @JsonKey annotation
  case class ExternalServiceRequest(id: ThingId, snakeCaseField: String, achievedAt: Instant)
  object ExternalServiceRequest {
    implicit val config: Configuration = Configuration.default
    given ConfiguredCodec[ExternalServiceRequest] = ConfiguredCodec.derived(using summon[Configuration].withTransformMemberNames {
  case "snakeCaseField" =>
    "snake_case_field"
  case "achievedAt" =>
    "acheivedAt"
  case name =>
    summon[Configuration].transformMemberNames(name)
})
  }

  println(
    "Request example: " + ExternalServiceRequest(
      ThingId("abc123"),
      "foo",
      Instant.ofEpochMilli(1711505683600L)
    ).asJson
  )

  // 3. Sum types with a discriminator
  object Fruit {
    implicit val configuration: Configuration =
      Configuration.default.withScreamingSnakeCaseConstructorNames
        .withDiscriminator("fruitType")

    case class Banana(curvature: Double) extends Fruit
    case class Apple(diameter: Double, variety: String) extends Fruit
    case object Grape extends Fruit
  }
  sealed trait Fruit derives ConfiguredCodec
  import Fruit.{Apple, Banana, Grape}

  val fruits: List[Fruit] = List(Banana(3.14), Apple(0.07, "Fuji"), Grape)
  println("Fruits: " + fruits.asJson)
}