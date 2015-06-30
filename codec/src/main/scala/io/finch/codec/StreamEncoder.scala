package io.finch.codec

import com.twitter.concurrent.Spool
import com.twitter.io.Buf

trait StreamEncoder[A] extends Encoder[A] {
  def encodeStream(s: Spool[A]): Spool[Buf]
}

object StreamEncoder {
  type Aux[A, C <: String] = StreamEncoder[A] { type As = C }
}

trait StreamEncoderProvider[T[_]] extends WithContentType {
  implicit def provideStreamEncoder[A](implicit instance: T[A]): StreamEncoder.Aux[A, As]
}
