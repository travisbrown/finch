package io.finch.demo.petstore

import _root_.argonaut._, Argonaut._
import io.finch.demo.petstore._
import org.scalacheck.Prop.BooleanOperators
import org.scalatest.{Matchers, FlatSpec}
import org.scalatest.prop.Checkers

class CategorySpec extends FlatSpec with Matchers with Checkers {
  "The Category codec" should "correctly decode JSON" in {
    check { (id: Long, name: String) =>
    	(!name.contains("\"")) ==> {
        val json = s"""{ "id": $id, "name": "$name" }"""

        Parse.decodeOption[Category](json) === Some(Category(id, name))
      }
    }
  }

  it should "round-trip Category" in {
    check { (id: Long, name: String) =>
    	(!name.contains("\"")) ==> {
        val category = Category(id, name)

        Parse.decodeOption[Category](category.asJson.nospaces) === Some(category)
      }
    }
  }
}
