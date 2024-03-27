import io.circe.{Codec, Encoder, Decoder}
import io.circe.derivation.{ Configuration, ConfiguredCodec }

object CirceDerivationAnnotations {
  object jsonCodec {
    case class CClass0() derives Codec.AsObject
    case class CClass1(name: String) derives Codec.AsObject
    case class CClass1Generic1[T](v: T)
    object CClass1Generic1 { given [T: Encoder: Decoder]: Codec[CClass1Generic1[T]] = Codec.AsObject.derived }
    case class CClass2Generic2[A, B](a: A, b: B)
    object CClass2Generic2 { given [A: Encoder: Decoder, B: Encoder: Decoder]: Codec[CClass2Generic2[A, B]] = Codec.AsObject.derived }
    sealed trait T1 derives Codec.AsObject { def name: String }
    case class T1Impl(name: String, count: Int) extends T1 derives Codec.AsObject

    implicitly[Codec[CClass0]]
    implicitly[Codec[CClass1]]
    implicitly[Codec[CClass1Generic1[Int]]]
    implicitly[Codec[CClass2Generic2[String, CClass0]]]
    implicitly[Codec[T1]]
    implicitly[Codec[T1Impl]]
  }

  object jsonExplicitCodec {
    case class CClass0()
    object CClass0 { given Codec[CClass0] = ConfiguredCodec.derived(using Configuration.default) }
    case class CClass1(name: String)
    object CClass1 { given Codec[CClass1] = ConfiguredCodec.derived(using Configuration.default) }
    case class CClass1Generic1[T](v: T)
    object CClass1Generic1 { given [T: Encoder: Decoder]: Codec[CClass1Generic1[T]] = ConfiguredCodec.derived(using Configuration.default) }
    case class CClass2Generic2[A, B](a: A, b: B)
    object CClass2Generic2 { given [A: Encoder: Decoder, B: Encoder: Decoder]: Codec[CClass2Generic2[A, B]] = ConfiguredCodec.derived(using Configuration.default) }
    sealed trait T1 { def name: String }
    object T1 { given Codec[T1] = ConfiguredCodec.derived(using Configuration.default) }
    case class T1Impl(name: String, count: Int) extends T1
    object T1Impl { given Codec[T1Impl] = ConfiguredCodec.derived(using Configuration.default) }

    implicitly[Codec[CClass0]]
    implicitly[Encoder[CClass1]]
    implicitly[Decoder[CClass1Generic1[Int]]]
    implicitly[Codec[CClass2Generic2[String, CClass0]]]
    implicitly[Encoder[T1]]
    implicitly[Encoder[T1Impl]]
  }

  object jsonCodecWithJsonKey {
    case class CClass0() derives Codec.AsObject
    case class CClass1(name: String)
    object CClass1 {
  given Codec[CClass1] = ConfiguredCodec.derived(using Configuration.default.withTransformMemberNames {
    case "name" =>
      "id"
    case name =>
      Configuration.default.transformMemberNames(name)
  })
}
    case class CClass1Generic1[T](v: T)
    object CClass1Generic1 {
  given [T: Encoder: Decoder]: Codec[CClass1Generic1[T]] = ConfiguredCodec.derived(using Configuration.default.withTransformMemberNames {
    case "v" =>
      "firstValue"
    case name =>
      Configuration.default.transformMemberNames(name)
  })
}
    case class CClass2Generic2[A, B](a: A, b: B)
    object CClass2Generic2 {
  given [A: Encoder: Decoder, B: Encoder: Decoder]: Codec[CClass2Generic2[A, B]] = ConfiguredCodec.derived(using Configuration.default.withTransformMemberNames {
    case "a" =>
      "first"
    case "b" =>
      "secondValue"
    case name =>
      Configuration.default.transformMemberNames(name)
  })
}
    // @JsonCodec() sealed trait T1 { @JsonKey("memberName") def name: String }
    // @JsonCodec() case class T1Impl(name: String, count: Int) extends T1

    implicitly[Codec[CClass0]]
    implicitly[Encoder[CClass1]]
    implicitly[Decoder[CClass1Generic1[Int]]]
    implicitly[Codec[CClass2Generic2[String, CClass0]]]
    // implicitly[Encoder[T1]]
    // implicitly[Encoder[T1Impl]]
  }
}
