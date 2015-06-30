package io.finch.codec

import com.twitter.concurrent.Spool
import com.twitter.io.Buf
import com.twitter.util.Future

trait StreamDecoder[A] extends Decoder[A] {
  def decodeStream(s: Spool[Buf]): Future[Spool[A]]
}

object StreamDecoder {
  type Aux[A, C <: String] = StreamDecoder[A] { type As = C }
}

trait StreamDecoderProvider[T[_], C <: String] {
  implicit def provideStreamDecoder[A](implicit instance: T[A]): StreamDecoder.Aux[A, C]
}
