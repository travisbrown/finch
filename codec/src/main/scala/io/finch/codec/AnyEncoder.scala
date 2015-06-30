package io.finch.codec

import com.twitter.io.Buf

/**
 * An abstraction that is responsible for encoding the response of a generic
 * type.
 */
trait AnyEncoder extends WithContentType {
  def apply[A](rep: A): Buf
}

object AnyEncoder {
  type Aux[C <: String] = AnyEncoder { type As = C }

  implicit def toConcreteEncoder[A, C <: String](implicit
    ae: AnyEncoder.Aux[C]
  ): Encoder.Aux[A, C] = new Encoder[A] {
    type As = C

    def apply(a: A): Buf = ae(a)
    def contentType: String = ae.contentType
  }
}

trait AnyEncoderProvider extends WithContentType {
  implicit def anyEncoder: AnyEncoder.Aux[As]
}
