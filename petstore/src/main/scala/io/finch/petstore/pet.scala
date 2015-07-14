package io.finch.petstore

import _root_.argonaut._
import argonaut.Argonaut._

/**
 * Represents Pets in the Petstore. Each Pet has a unique ID that should not be known by
 * the user at the time of its creation.
 * @param id The pet's auto-generated, unique ID.
 * @param name (Required) The pet's name.
 * @param photoUrls (Required) A sequence of URLs that lead to uploaded photos of the pet.
 * @param category The type of pet (cat, dragon, fish, etc.)
 * @param tags Tags that describe this pet.
 * @param status (Available, Pending, or Adopted)
 */
case class Pet(
    id: Option[Long],
    name: String,
    photoUrls: Seq[String],
    category: Option[Category],
    tags: Option[Seq[Tag]],
    status: Option[Status] //available, pending, adopted
    )

/**
 * Provides a codec for decoding and encoding Pet objects.
 */
object Pet {
  implicit val petCodec: CodecJson[Pet] = //instance of a type class
    casecodec6(Pet.apply, Pet.unapply)("id", "name", "photoUrls", "category", "tags", "status")
}

/*
STATUS THINGS BEGIN HERE+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 */

/**
 * Represents the general status of a Pet. This should either be available, pending, or adopted.
 */
sealed trait Status {
  /**
   * @return The string representing the value of this status.
   */
  def code: String
}

/**
 * The status of a pet when it is available for adoption.
 */
case object Available extends Status {
  /**
   * The string representing the value of this status.
   * @return "available"
   */
  def code: String = "available"
}

/**
 * The status of a pet when it is pending for adoption, and currently unavailable for purchase.
 */
case object Pending extends Status {
  /**
   * The string representing the value of this status.
   * @return "pending"
   */
  def code: String = "pending"
}

/**
 * The status of a pet when it has been adopted.
 */
case object Adopted extends Status {
  /**
   * The string representing the value of this status.
   * @return "adopted"
   */
  def code: String = "adopted"
}

/**
 * Provides encoding and decoding methods for Status objects. When given a string other than
 * "available," "pending," or "adopted," it fails to decode the string to a Status object.
 */
object Status {
  /**
   * Maps strings to their corresponding Status objects
   * @param s String to be converted to a Status object.
   * @return Status object corresponding to s.
   */
  def fromString(s: String): Status = s match {
    case "available" => Available
    case "pending" => Pending
    case "adopted" => Adopted
  }

  /**
   * Encoding method that takes a Status and returns the corresponding string value in JSON.
   */
  val statusEncode: EncodeJson[Status] =
    EncodeJson.jencode1[Status, String](_.code)

  /**
   * Decoding method that takes JSON and gives back its corresponding Status.
   * If the given string is not one of the three valid statuses, the system will fail.
   */
  val statusDecode: DecodeJson[Status] =
    DecodeJson { c =>
      c.as[String].flatMap[Status] {
        case "available" => DecodeResult.ok(Available)
        case "pending" => DecodeResult.ok(Pending)
        case "adopted" => DecodeResult.ok(Adopted)
        case other => DecodeResult.fail(s"Unknown status: $other", c.history)
      }
    }

  /**
   * Creates the codec for the Status object.
   */
  implicit val statusCodec: CodecJson[Status] =
    CodecJson.derived(statusEncode, statusDecode)
}

/*
STATUS THINGS END HERE========================================================
 */

/*
CATEGORY THINGS BEGIN HERE+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 */

/**
 * Represents a Category object for pets. This is the type of animal a pet is.
 * @param id The unique, autogenerated ID of this Category.
 * @param name The name of this Category.
 */
case class Category(id: Option[Long], name: String)

/**
 * Provides encoding and decoding methods for Category objects.
 */
object Category {
  /**
   * Creates the codec for Category objects.
   */
  implicit val categoryCodec: CodecJson[Category] =
    casecodec2(Category.apply, Category.unapply)("id", "name")
}
/*
CATEGORY THINGS END HERE========================================================
 */

/*
TAG THINGS BEGIN HERE+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 */

/**
 * Represents a Tag for pets. Tags cannot be passed with user-made, IDs.
 * @param id The unique, autogenerated ID of this Tag.
 * @param name The name of this Tag.
 */
case class Tag(id: Option[Long], name: String)

/**
 * Represents a Tag object for pets.
 */
object Tag {
  /**
   * Creates the codec for converting Tags from and to JSON.
   */
  implicit val tagCodec: CodecJson[Tag] =
    casecodec2(Tag.apply, Tag.unapply)("id", "name")
}
/*
TAG THINGS END HERE========================================================
 */

/*
INVENTORY BEGIN HERE+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 */

case class Inventory(available: Int, pending: Int, adopted: Int)

/**
 * Provides a codec for encoding and decoding Order objects.
 */
object Inventory {
  /**
   * Encoding method that takes a Status and returns the corresponding string value in JSON.
   */
  val inventoryEncode: EncodeJson[Inventory] =
    jencode3L((i:Inventory) => (i.available, i.pending, i.adopted))("available", "pending", "adopted")

  /**
   * Provides the encode/decode codec for Inventory objects.
   */
  implicit val inventoryCodec: CodecJson[Inventory] =
    CodecJson(
      (i: Inventory) =>
        ("available" := i.available) ->: ("pending" := i.pending) ->: ("adopted" := i.adopted) ->:
        jEmptyObject,
      c => for {
        available <- (c --\ "available").as[Int]
        pending <- (c --\ "pending").as[Int]
        adopted <- (c --\ "adopted").as[Int]
      } yield Inventory(available, pending, adopted))
}
/*
INVENTORY END HERE========================================================
 */

/*
ORDERSTATUS THINGS BEGIN HERE+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 */

/**
 * Represents the status of a particular order of pets. Can be "placed," "approved," or "delivered."
 */
sealed trait OrderStatus {
  /**
   * @return The string representation of the OrderStatus.
   */
  def code: String
}

/**
 * The status of an order after it has been placed.
 */
case object Placed extends OrderStatus {
  /**
   * The string representation of the OrderStatus.
   * @return "placed"
   */
  def code: String = "placed"
}

/**
 * The status of an order after it has been approved by the store.
 */
case object Approved extends OrderStatus {
  /**
   * The string representation of the OrderStatus.
   * @return "approved"
   */
  def code: String = "approved"
}

/**
 * The status of an order after it has been delivered and completed.
 */
case object Delivered extends OrderStatus {
  /**
   * The string representation of the OrderStatus.
   * @return "delivered"
   */
  def code: String = "delivered"
}

/**
 * Provides encode and decode methods for OrderStatus objects.
 * If asked to decode a string other than "placed," "approved," or "delivered" the
 * system will fail.
 */
object OrderStatus {
  /**
   * Coverts a given string into its corresponding OrderStatus object.
   * @param s The string to be converted.
   * @return OrderStatus object corresponding to s.
   */
  def fromString(s: String): OrderStatus = s match {
    case "placed" => Placed
    case "approved" => Approved
    case "delivered" => Delivered
  }

  /**
   * Encodes a given OrderStatus into JSON.
   */
  val orderStatusEncode: EncodeJson[OrderStatus] =
    EncodeJson.jencode1[OrderStatus, String](_.code)

  /**
   * Decodes a given piece of JSON into an OrderStatus object.
   * If the given string does not match any of the three valid OrderStatuses,
   * the system will fail.
   */
  val orderStatusDecode: DecodeJson[OrderStatus] =
    DecodeJson { c =>
      c.as[String].flatMap[OrderStatus] {
        case "placed" => DecodeResult.ok(Placed)
        case "approved" => DecodeResult.ok(Approved)
        case "delivered" => DecodeResult.ok(Delivered)
        case other => DecodeResult.fail(s"Unknown status: $other", c.history)
      }
    }

  /**
   * Creates a codec for OrderStatus objects.
   */
  implicit val orderStatusCodec: CodecJson[OrderStatus] =
    CodecJson.derived(orderStatusEncode, orderStatusDecode)
}

/*
ORDERSTATUS THINGS END HERE========================================================
 */

/*
ORDER THINGS BEGIN HERE+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 */

/**
 * Represents an order to the petstore.
 * @param id The unique, autogenerated ID of the order. The user should not give an ID.
 * @param petId The ID of the pet being ordered.
 * @param quantity The number of pets being ordered.
 * @param shipDate The date the order will be shipped by.
 * @param status The status of the order.
 * @param complete Whether the order has been fulfilled.
 */
case class Order(
    id: Option[Long],
    petId: Option[Long],
    quantity: Option[Long],
    shipDate: Option[String],
    status: Option[OrderStatus], //placed, approved, delivered
    complete: Option[Boolean]
    )

/**
 * Provides a codec for encoding and decoding Order objects.
 */
object Order {
  implicit val orderCodec: CodecJson[Order] =
    casecodec6(Order.apply, Order.unapply)("id", "petId", "quantity", "shipDate", "status", "complete")
}
/*
ORDER THINGS END HERE========================================================
 */



/*
USER THINGS BEGIN HERE+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 */
/**
 * Represents a User in the system, who can interact with the petstore and purchase
 * available [[Pet]] objects.
 * @param id Unique, autogenerated ID of the User
 * @param username (Required)
 * @param firstName
 * @param lastName
 * @param email
 * @param password (Required)
 * @param phone
 */
case class User(
    id: Option[Long],
    username: String,
    firstName: Option[String],
    lastName: Option[String],
    email: Option[String],
    password: String,
    phone: Option[String]
    )

/**
 * Companion object to the User class.
 */
object User{
  /**
   * Creates the encode/decode codec for the User object.
   */
  implicit val userCodec: CodecJson[User] =
    casecodec7(User.apply, User.unapply)("id", "username", "firstName", "lastName", "email", "password", "phone")
}

/*
USER THINGS END HERE========================================================
 */

