package io.finch.request

import com.twitter.util.{Future, Try}
import io.catbird.util._

import scala.reflect.ClassTag

/**
 * Trait with low-priority implicits to avoid conflicts that would arise from adding implicits that would work with
 * any type in the same scope as implicits for concrete types.
 *
 * Implicits defined in super-types have lower priority than those defined in a sub-type. Therefore we define low-
 * priority implicits here and mix this trait into the package object.
 */
trait LowPriorityRequestReaderImplicits {

  /**
   * Creates a [[io.finch.request.DecodeMagnet DecodeMagnet]] from
   * [[io.finch.request.DecodeAnyRequest DecodeAnyRequest]].
   */
  implicit def decodeRequestFromAnyDecode[A](
    implicit d: DecodeAnyRequest, tag: ClassTag[A]
  ): DecodeRequest[A] = new DecodeRequest[A] {
    def apply(req: String): Try[A] = d(req)(tag)
  }
}
