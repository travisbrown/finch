package io.finch.demo.petstore

import com.twitter.util.Await
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
    Await.ready(db.addPet(sue.copy(id = None)))
  }

  "The Petstore DB" should "allow pet lookup by id" in new DbContext {
    assert(Await.result(db.getPet(0)) === Some(rover))
  }

  it should "allow adding pets" in new DbContext {
    check { (pet: Pet) =>
      val result = for {
        petId <- db.addPet(pet)
        newPet <- db.getPet(petId)
      } yield newPet === Some(pet.copy(id = Some(petId)))

      Await.result(result)
    }
  }

  it should "fail appropriately for pet ids that don't exist" in new DbContext {
    assert(Await.result(db.getPet(1001)) === None)
  }
}
