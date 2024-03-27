/*
rule = CirceGenericExtrasMigration
*/
import io.circe.generic.extras._
import io.circe.derivation.annotations.{ JsonCodec }
import io.circe.{Codec, Encoder, Decoder}

object CirceGenericExtras {
  implicit val defaultConfig: Configuration = Configuration.default

  object configuredJsonCodec {
    @ConfiguredJsonCodec() case class CClass0()
    @ConfiguredJsonCodec(encodeOnly = true) case class CClass1(name: String)
    @ConfiguredJsonCodec(decodeOnly = true) case class CClass1Generic1[T](v: T)
    @ConfiguredJsonCodec() case class CClass2Generic2[A, B](a: A, b: B)
    @ConfiguredJsonCodec() sealed trait T1 { def name: String }
    @ConfiguredJsonCodec() case class T1Impl(name: String, count: Int) extends T1

    implicitly[Codec[CClass0]]
    implicitly[Encoder[CClass1]]
    implicitly[Decoder[CClass1Generic1[Int]]]
    // implicitly[Codec[CClass2Generic2[String, CClass0]]]
    implicitly[Codec[T1]]
    implicitly[Codec[T1Impl]]
  }

  object jsonCodecWithJsonKey {
    @ConfiguredJsonCodec() case class CClass0()
    @ConfiguredJsonCodec() case class CClass1(@JsonKey("id") name: String)
    @ConfiguredJsonCodec() case class CClass1Generic1[T](@JsonKey("firstValue")v: T)
    @ConfiguredJsonCodec() case class CClass2Generic2[A, B](@JsonKey("first")a: A, @JsonKey("secondValue")b: B)
    // @ConfiguredJsonCodec() sealed trait T1 { @JsonKey("memberName") def name: String }
    // @ConfiguredJsonCodec() case class T1Impl(name: String, count: Int) extends T1

    implicitly[Codec[CClass0]]
    implicitly[Encoder[CClass1]]
    implicitly[Decoder[CClass1Generic1[Int]]]
    // implicitly[Codec[CClass2Generic2[String, CClass0]]]
    // implicitly[Encoder[T1]]
    // implicitly[Encoder[T1Impl]]
  }
}
