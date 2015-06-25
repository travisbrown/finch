package io.finch.demo.petstore

import com.twitter.util.Await
import io.finch.demo.petstore._
import org.scalacheck.Prop.BooleanOperators
import org.scalatest.{Matchers, FlatSpec}
import org.scalatest.prop.Checkers

class PetstoreDbSpec extends FlatSpec with Matchers with Checkers {
  val rover = Pet(Some(0), "Rover", Nil, Category(1, "dog"), Nil, Available)
  val jack = Pet(Some(1), "Jack", Nil, Category(1, "dog"), Nil, Available)
  val sue = Pet(None, "Sue", Nil, Category(1, "dog"), Nil, Sold)

  trait DbContext {
    val db = new PetstoreDb()
    Await.ready(db.addPet(rover.copy(id = None)))
    Await.ready(db.addPet(jack.copy(id = None)))
  }

  "The Petstore DB" should "allow pet lookup by id" in new DbContext {
    assert(Await.result(db.getPet(0)) === Some(rover))
  }

  it should "allow adding pets" in new DbContext {
    val result = for {
      sueId <- db.addPet(sue)
      newSue <- db.getPet(sueId)
    } yield newSue === Some(sue.copy(id = Some(sueId)))

    assert(Await.result(result))
  }

  it should "fail appropriately for pet ids that don't exist" in new DbContext {
    assert(Await.result(db.getPet(1001)) === None)
  }
}
