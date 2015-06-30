package io.finch.codec

/**
 * An abstraction that is responsible for encoding the response of type `A`.
 */
trait WithContentType {
  type As <: String

  def contentType: String
}
