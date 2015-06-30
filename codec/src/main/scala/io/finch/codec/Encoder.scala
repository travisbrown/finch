package io.finch.codec

import com.twitter.io.Buf
import java.nio.charset.Charset
import shapeless.Witness

/**
 * An abstraction that is responsible for encoding type `A`.
 */
trait Encoder[A] extends WithContentType {
  def apply(a: A): Buf
}

object Encoder {
  type Aux[A, C <: String] = Encoder[A] { type As = C }

  /**
   * This is a convenience class that lets us work around the fact that Scala
   * doesn't support partial application of type parameters.
   */
  class Builder[C <: String](implicit w: Witness.Aux[C]) {
    /**
     * Convenience method for creating new [[EncodeResponse]] instances that
     * treat [[com.twitter.io.Buf]] contents.
     */
    def fromBuf[A](f: A => Buf): Aux[A, C] =
      new Encoder[A] {
        type As = C

        def apply(a: A): Buf = f(a)
        val contentType: String = w.value
      }

    /**
     * Convenience method for creating new [[EncodeResponse]] instances that
     * treat `String` contents.
     */
    def fromString[A](f: A => String): Aux[A, C] = fromBuf(a => Buf.Utf8(f(a)))
  }

  /**
   * Convenience method for creating new [[EncodeResponse]] instances.
   */
  def apply[C <: String](implicit w: Witness.Aux[C]): Builder[C] = new Builder[C]
}

trait EncoderProvider[T[_]] extends WithContentType {
  implicit def provideEncoder[A](implicit instance: T[A]): Encoder.Aux[A, As]
}

trait BasicEncoders {
  /**
   * Allows to pass raw strings to a [[ResponseBuilder]].
   */
  implicit val stringEncoder: TextEncoder[String] =
    Encoder("text/plain").fromString[String](identity)

  /**
   * Allows to pass `Buf` to a [[ResponseBuilder]].
   */
  implicit val bufEncoder: BinaryEncoder[Buf] =
    Encoder("application/octet-stream").fromBuf(identity)
}

