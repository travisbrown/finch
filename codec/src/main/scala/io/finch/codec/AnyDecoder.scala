package io.finch.codec

import com.twitter.io.Buf
import com.twitter.util.Try
import scala.reflect.ClassTag

trait AnyDecoder extends WithContentType {
  def apply[A: ClassTag](rep: Buf): Try[A]
  def decodeString[A: ClassTag](rep: String): Try[A] = apply(Buf.Utf8(rep))
}

object AnyDecoder {
  type Aux[C <: String] = AnyDecoder { type As = C }

  implicit def toConcreteDecoder[A: ClassTag, C <: String](implicit
    ad: AnyDecoder.Aux[C]
  ): Decoder.Aux[A, C] = new Decoder[A] {
    type As = C

    def apply(rep: Buf): Try[A] = ad(rep)
    def contentType: String = ad.contentType
  }
}

trait AnyDecoderProvider extends WithContentType {
  implicit def anyDecoder: AnyDecoder.Aux[As]
}
