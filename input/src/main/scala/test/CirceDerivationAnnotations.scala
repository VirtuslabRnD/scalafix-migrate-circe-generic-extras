/*
rule = CirceGenericExtrasMigration
*/
import io.circe.derivation.annotations._
import io.circe.{Codec, Encoder, Decoder}

object CirceDerivationAnnotations {
  object jsonCodec {
    @JsonCodec() case class CClass0()
    @JsonCodec() case class CClass1(name: String)
    @JsonCodec() case class CClass1Generic1[T](v: T)
    @JsonCodec() case class CClass2Generic2[A, B](a: A, b: B)
    @JsonCodec() sealed trait T1 { def name: String }
    @JsonCodec() case class T1Impl(name: String, count: Int) extends T1

    implicitly[Codec[CClass0]]
    implicitly[Codec[CClass1]]
    implicitly[Codec[CClass1Generic1[Int]]]
    implicitly[Codec[CClass2Generic2[String, CClass0]]]
    implicitly[Codec[T1]]
    implicitly[Codec[T1Impl]]
  }

  object jsonExplicitCodec {
    @JsonCodec(Configuration.default) case class CClass0()
    @JsonCodec(Configuration.default) case class CClass1(name: String)
    @JsonCodec(Configuration.default) case class CClass1Generic1[T](v: T)
    @JsonCodec(Configuration.default) case class CClass2Generic2[A, B](a: A, b: B)
    @JsonCodec(Configuration.default) sealed trait T1 { def name: String }
    @JsonCodec(Configuration.default) case class T1Impl(name: String, count: Int) extends T1

    implicitly[Codec[CClass0]]
    implicitly[Encoder[CClass1]]
    implicitly[Decoder[CClass1Generic1[Int]]]
    implicitly[Codec[CClass2Generic2[String, CClass0]]]
    implicitly[Encoder[T1]]
    implicitly[Encoder[T1Impl]]
  }

  object jsonCodecWithJsonKey {
    @JsonCodec() case class CClass0()
    @JsonCodec() case class CClass1(@JsonKey("id") name: String)
    @JsonCodec() case class CClass1Generic1[T](@JsonKey("firstValue")v: T)
    @JsonCodec() case class CClass2Generic2[A, B](@JsonKey("first")a: A, @JsonKey("secondValue")b: B)
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
