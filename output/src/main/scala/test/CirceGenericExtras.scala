import io.circe.{Codec, Encoder, Decoder}
import io.circe.derivation.{ Configuration, ConfiguredCodec, ConfiguredDecoder, ConfiguredEncoder }

object CirceGenericExtras {
  implicit val defaultConfig: Configuration = Configuration.default

  object configuredJsonCodec {
    case class CClass0() derives ConfiguredCodec
    case class CClass1(name: String) derives ConfiguredEncoder
    case class CClass1Generic1[T](v: T)
    object CClass1Generic1 { given [T: Decoder]: ConfiguredDecoder[CClass1Generic1[T]] = ConfiguredDecoder.derived }
    case class CClass2Generic2[A, B](a: A, b: B)
    object CClass2Generic2 { given [A: Encoder: Decoder, B: Encoder: Decoder]: ConfiguredCodec[CClass2Generic2[A, B]] = ConfiguredCodec.derived }
    sealed trait T1 derives ConfiguredCodec { def name: String }
    case class T1Impl(name: String, count: Int) extends T1 derives ConfiguredCodec

    implicitly[Codec[CClass0]]
    implicitly[Encoder[CClass1]]
    implicitly[Decoder[CClass1Generic1[Int]]]
    // implicitly[Codec[CClass2Generic2[String, CClass0]]]
    implicitly[Codec[T1]]
    implicitly[Codec[T1Impl]]
  }

  object jsonCodecWithJsonKey {
    case class CClass0() derives ConfiguredCodec
    case class CClass1(name: String)
    object CClass1 {
  given ConfiguredCodec[CClass1] = ConfiguredCodec.derived(using summon[Configuration].withTransformConstructorNames {
    case "name" =>
      "id"
    case name =>
      summon[Configuration].transformConstructorNames(name)
  })
}
    case class CClass1Generic1[T](v: T)
    object CClass1Generic1 {
  given [T: Encoder: Decoder]: ConfiguredCodec[CClass1Generic1[T]] = ConfiguredCodec.derived(using summon[Configuration].withTransformConstructorNames {
    case "v" =>
      "firstValue"
    case name =>
      summon[Configuration].transformConstructorNames(name)
  })
}
    case class CClass2Generic2[A, B](a: A, b: B)
    object CClass2Generic2 {
  given [A: Encoder: Decoder, B: Encoder: Decoder]: ConfiguredCodec[CClass2Generic2[A, B]] = ConfiguredCodec.derived(using summon[Configuration].withTransformConstructorNames {
    case "a" =>
      "first"
    case "b" =>
      "secondValue"
    case name =>
      summon[Configuration].transformConstructorNames(name)
  })
}
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
