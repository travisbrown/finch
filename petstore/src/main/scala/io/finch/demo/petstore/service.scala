package io.finch.demo.petstore

import _root_.argonaut._, Argonaut._
import com.twitter.finagle.Httpx
import com.twitter.util.{ Await, Future }
import io.finch._
import io.finch.argonaut._
import io.finch.request._
import io.finch.route._

object PetstoreApp extends App {
  val db = new PetstoreDb()
  db.addPet(Pet(None, "Rover", Nil, Category(1, "dog"), Nil, Available))
  db.addPet(Pet(None, "Jack", Nil, Category(1, "dog"), Nil, Available))
  db.addPet(Pet(None, "Sue", Nil, Category(1, "dog"), Nil, Sold))

  val name: RequestReader[String] = param("name")
  val status: RequestReader[Status] = param("status").map(Status.fromString)

  def failIfEmpty(o: Option[Pet]): Future[Pet] = o match {
    case Some(pet) => Future.value(pet)
    case None => Future.exception(MissingPet("No pet!"))
  }

  val pet = Post / "pet" / long /> { (id: Long) =>
    (name :: status).asTuple.embedFlatMap {
      case (n, s) => println(s"$n, $s, $id"); for {
        maybePet <- db.getPet(id)
        pet      <- failIfEmpty(maybePet)
        res <- db.updatePet(pet.copy(name = n, status = s))
      } yield res
    }
  }

	val store = Get / "store" / "inventory" /> (
		db.statusCodes.map {
	  	_.map { case (k, v) => (k.code, v) }
	  }
	)

	val server = Httpx.serve(":8080", (store :+: pet).toService)

  def close() = {
    Await.ready(server.close())
  }
}
