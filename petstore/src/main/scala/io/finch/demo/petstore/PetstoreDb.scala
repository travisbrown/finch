package io.finch.demo.petstore

import com.twitter.util.Future
import scala.collection.mutable

class PetstoreDb {
  private[this] val pets = mutable.Map.empty[Long, Pet]
  def getPet(id: Long): Future[Option[Pet]] = Future.value(
    pets.synchronized(pets.get(id))
  )

  def addPet(pet: Pet): Future[Long] = Future.value(
    pets.synchronized {
      val id = if (pets.isEmpty) 0 else pets.keys.max + 1
      pets(id) = pet.copy(id = Some(id))
      id
    }
  )

  def updatePet(pet: Pet): Future[Unit] = pet.id match {
    case Some(id) => Future.value(
      pets.synchronized(pets(id) = pet)
    )
    case None => Future.exception(MissingIdentifier(s"Missing id for pet: $pet"))
  }

  def allPets: Future[List[Pet]] = Future.value(
    pets.synchronized(pets.toList.sortBy(_._1).map(_._2))
  )

  def deletePet(id: Long): Future[Boolean] = Future.value(
    pets.synchronized {
      if (pets.contains(id)) {
        pets.remove(id)
        true
      } else false
    }
  )

  def statusCodes: Future[Map[Status, Int]] = Future.value(
    pets.synchronized {
      pets.groupBy(_._2.status).map {
        case (status, kvs) => (status, kvs.size) 
      }
    }
  )
}
