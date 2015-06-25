package io.finch.demo.petstore

import _root_.argonaut._, Argonaut._

case class Pet(
  id: Option[Long],
  name: String,
  photoUrls: Seq[String],
  category: Category,
  tags: Seq[Tag],
  status: Status
)

object Pet {
  implicit val petEncode: EncodeJson[Pet] =
    jencode6L { (p: Pet) =>
      (p.id, p.name, p.photoUrls, p.category, p.tags, p.status)
    }("id", "name", "photoUrls", "category", "tags", "status")
}

sealed trait Status {
  def code: String
}

case object Available extends Status {
  def code: String = "available"
}

case object Pending extends Status {
  def code: String = "pending"
}

case object Sold extends Status {
  def code: String = "sold"
}

object Status {
  def fromString(s: String): Status = s match {
    case "available" => Available
    case "pending" => Pending
    case "sold" => Sold
  }

  val statusEncode: EncodeJson[Status] =
    EncodeJson.jencode1[Status, String](_.code)

  val statusDecode: DecodeJson[Status] =
    DecodeJson { c =>
      c.as[String].flatMap[Status] {
        case "available" => DecodeResult.ok(Available)
        case "pending" => DecodeResult.ok(Pending)
        case "sold" => DecodeResult.ok(Sold)
        case other => DecodeResult.fail(s"Unknown status: $other", c.history)
      }
    }

  implicit val statusCodec: CodecJson[Status] =
    CodecJson.derived(statusEncode, statusDecode)
}

case class Category(id: Long, name: String)

object Category {
  implicit val categoryCodec: CodecJson[Category] =
    casecodec2(Category.apply, Category.unapply)("id", "name")
}

case class Tag(id: Long, name: String)

object Tag {
  implicit val tagCodec: CodecJson[Tag] =
    casecodec2(Tag.apply, Tag.unapply)("id", "name")
}

case class UploadResponse(
  code: Long,
  responseType: String,
  message: String
)

object UploadResponse {
  implicit val uploadResponseCodec: CodecJson[UploadResponse] =
    casecodec3(UploadResponse.apply, UploadResponse.unapply)("code", "type", "message")
}