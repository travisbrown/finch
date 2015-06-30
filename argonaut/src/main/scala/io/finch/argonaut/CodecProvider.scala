package io.finch.argonaut

import _root_.argonaut.{DecodeJson, EncodeJson, Json, Parse}
import com.twitter.concurrent.{Spool, SpoolSource}
import com.twitter.io.Buf
import com.twitter.util.{Future, Return, Throw, Try}
import io.finch.codec.{JsonType, StreamDecoder, StreamDecoderProvider, StreamEncoderProvider}
import jawn.{AsyncParser, Parser}
import jawn.support.argonaut.Parser.facade
import shapeless.Witness

case class DecodingError(message: String) extends Throwable(message)

object CodecProvider extends StreamDecoderProvider[DecodeJson, JsonType] { self =>

  implicit def provideStreamDecoder[A](implicit
    instance: DecodeJson[A]
  ): StreamDecoder.Aux[A, JsonType] = new StreamDecoder[A] {
    type As = JsonType

    val contentType: String = "application/json"

    def apply(rep: Buf): Try[A] = rep match {
      case Buf.Utf8(s) =>
        Parse.decodeEither(s).fold[Try[A]](
          error => Throw[A](DecodingError(error)),
          Return(_)
        )
    }

    def decodeStream(s: Spool[Buf]): Future[Spool[A]] = {
      val parser: AsyncParser[Json] = Parser.async(AsyncParser.UnwrapArray)(facade)
      val source: SpoolSource[A] = new SpoolSource[A]
      val spool: Future[Spool[A]] = source()

      s.foreachElem {
        case Some(buf) =>
          parser.absorb(Buf.ByteBuffer.Owned.extract(buf)).fold(
            source.raise,
            items =>
              items.foreach { item =>
                item.as[A].fold(
                  (s, _) => source.raise(DecodingError(s)),
                  source.offer
                )
              }
          )
        case None => source.close()
      }.flatMap { _ =>
        spool
      }
    }
  }
}