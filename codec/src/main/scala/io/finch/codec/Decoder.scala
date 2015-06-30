package io.finch.codec

import com.twitter.io.Buf
import com.twitter.util.Try
import shapeless.Witness

/**
 * An abstraction that is responsible for decoding a type `A`.
 */
trait Decoder[A] extends WithContentType {
  def apply(rep: Buf): Try[A]
  def decodeString(rep: String): Try[A] = apply(Buf.Utf8(rep))
}

object Decoder {
  type Aux[A, C <: String] = Decoder[A] { type As = C }

  /**
   * This is a convenience class that lets us work around the fact that Scala
   * doesn't support partial application of type parameters.
   */
  class Builder[C <: String](implicit w: Witness.Aux[C]) {
    /**
     * Convenience method for creating new [[DecodeResponse]] instances that
     * treat [[com.twitter.io.Buf]] contents.
     */
    def fromBuf[A](f: Buf => Try[A]): Aux[A, C] =
      new Decoder[A] {
        type As = C

        def apply(rep: Buf): Try[A] = f(rep)
        val contentType: String = w.value
      }

    /**
     * Convenience method for creating new [[DecodeResponse]] instances that
     * treat `String` contents.
     */
    def fromString[A](f: String => Try[A]): Aux[A, C] = fromBuf {
      case Buf.Utf8(s) => f(s)
    }
  }

  /**
   * Convenience method for creating new [[DecodeResponse]] instances.
   */
  def apply[C <: String](implicit w: Witness.Aux[C]): Builder[C] = new Builder[C]
}

trait DecoderProvider[T[_]] extends WithContentType {
  implicit def provideDecoder[A](implicit instance: T[A]): Decoder.Aux[A, As]
}
