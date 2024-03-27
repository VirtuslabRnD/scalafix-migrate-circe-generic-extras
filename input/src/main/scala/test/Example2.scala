/*
rules = [CirceGenericExtrasMigration, CirceLiteralMigration]
*/
package test

import io.circe.Codec
import io.circe.generic.extras.{Configuration, ConfiguredJsonCodec, JsonKey}
import io.circe.syntax.EncoderOps
import java.time.Instant

object CirceUsage extends App {

  // 1. deriveUnwrappedCodec
  case class ThingId(value: String) extends AnyVal
  object ThingId {
    implicit val codec: Codec[ThingId] =
      io.circe.generic.extras.semiauto.deriveUnwrappedCodec
  }

  // 2. @JsonKey annotation
  @ConfiguredJsonCodec
  case class ExternalServiceRequest(
      id: ThingId,
      @JsonKey("snake_case_field") snakeCaseField: String,
      @JsonKey("acheivedAt") achievedAt: Instant // Do not inherit misspelling
  )
  object ExternalServiceRequest {
    implicit val config: Configuration = Configuration.default
  }

  println(
    "Request example: " + ExternalServiceRequest(
      ThingId("abc123"),
      "foo",
      Instant.now()
    ).asJson
  )

  // 3. Sum types with a discriminator
  @ConfiguredJsonCodec
  sealed trait Fruit
  object Fruit {
    implicit val configuration: Configuration =
      Configuration.default.withScreamingSnakeCaseConstructorNames
        .withDiscriminator("fruitType")

    case class Banana(curvature: Double) extends Fruit
    case class Apple(diameter: Double, variety: String) extends Fruit
    case object Grape extends Fruit
  }
  import Fruit.{Apple, Banana, Grape}

  val fruits: List[Fruit] = List(Banana(3.14), Apple(0.07, "Fuji"), Grape)
  println("Fruits: " + fruits.asJson)
}