package io.finch.petstore

import com.twitter.finagle.Httpx
import com.twitter.util.{Future, Await}
import io.finch.argonaut._
import io.finch.petstore.endpoint._

/**
 * PetstoreApp runs the PetstoreAPI service. It is the hub where all the endpoints that give users access to API
 * methods are connected to the service itself, which is launched on port :8080.
 */
class PetstoreApp {
  val db = new PetstoreDb()
  db.addPet(Pet(None, "Sadaharu", Nil, Some(Category(1, "inugami")), Some(Nil), Some(Available)))
  db.addPet(Pet(None, "Despereaux", Nil, Some(Category(1, "mouse")), Some(Nil), Some(Available)))
  db.addPet(Pet(None, "Alexander", Nil, Some(Category(1, "mouse")), Some(Nil), Some(Pending)))
  db.addPet(Pet(None, "Wilbur", Nil, Some(Category(1, "pig")), Some(Nil), Some(Adopted)))
  db.addPet(Pet(None, "Cheshire Cat", Nil, Some(Category(1, "cat")), Some(Nil), Some(Available)))
  db.addPet(Pet(None, "Crookshanks", Nil, Some(Category(1, "cat")), Some(Nil), Some(Available)))

  // val service = (getPetEndpt(db) :+: addPetEndpt(db) :+: updatePetEndpt(db) :+: getAllPetsEndpt(db) :+:
  //     getPetsByStatusEndpt(db) :+: findPetsByTagEndpt(db) :+: deletePetEndpt(db) :+: updatePetViaFormEndpt(db) :+:
  //     uploadImageEndpt(db) :+: getInventoryEndpt(db) :+: addOrderEndpt(db) :+: deleteOrderEndpt(db) :+:
  //     findOrderEndpt(db) :+: addUserEndpt(db) :+: addUsersViaList(db) :+: addUsersViaArray(db) :+: getUserEndpt(db) :+:
  //     updateUserEndpt(db)).toService

  val service = (getPetEndpt(db) :+: addPetEndpt(db) :+: updatePetEndpt(db) :+: getAllPetsEndpt(db) :+: 
    getPetsByStatusEndpt(db) :+: findPetsByTagEndpt(db) :+: deletePetEndpt(db) :+: updatePetViaFormEndpt(db) :+:
    uploadImageEndpt(db) :+: getInventoryEndpt(db)).toService

// val service = (updatePetEndpt(db) :+: getPetEndpt(db) :+: uploadImageEndpt(db) :+: addUsersViaList(db)).toService

  val server = Httpx.serve(":8080", service) //creates service

  Await.ready(server)

  def close(): Future[Unit] = {
    Await.ready(server.close())
  }
}

/**
 * Launches the PetstoreAPI service when the system is ready.
 */
object PetstoreApp extends PetstoreApp with App {
  Await.ready(server)
}
