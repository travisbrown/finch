package io.finch.petstore

import com.twitter.util.{Await, Future}

import scala.collection.mutable
//import scala.concurrent.Await

class PetstoreDb {
  private[this] val pets = mutable.Map.empty[Long, Pet]
  private[this] val tags = mutable.Map.empty[Long, Tag]
  private[this] val cat = mutable.Map.empty[Long, Category]
  private[this] val orders = mutable.Map.empty[Long, Order]
  private[this] val photos = mutable.Map.empty[Long, Array[Byte]]
  private[this] val users = mutable.Map.empty[Long, User]

  /**
   * GET: Finds a Pet object by its ID.
   *
   * @param id The ID of the [[Pet]] we're looking for
   * @return The [[Pet]] object
   */
  def getPet(id: Long): Future[Pet] = Future(
    pets.synchronized {
      pets.getOrElse(id, throw MissingPet("Your pet doesn't exist! :("))
    }
  )

  /**
   * Helper method for addPet: Adds a tag to the tag map
   * @param inputTag The tag we want to add
   * @return The tag just added
   */
  private def addTag(inputTag: Tag): Future[Tag] =
    tags.synchronized {
      inputTag.id match{
        case x: Long => Future.exception(InvalidInput("New tag should not contain an id"))
        case _ => tags.synchronized{
          val genId = if (tags.isEmpty) 0 else tags.keys.max + 1
          tags(genId) = inputTag.copy(id = genId)
          Future(tags(genId))
        }
      }
    }

  /**
   * POST: Adds a [[Pet]] to the database, validating that the ID is empty.
   *
   * @param inputPet the new pet
   * @return the id of the new pet
   */
  def addPet(inputPet: Pet): Future[Long] =
    inputPet.id match {
      case Some(_) => Future.exception(InvalidInput("New pet should not contain an id"))
      case None => pets.synchronized {
        val id = if (pets.isEmpty) 0 else pets.keys.max + 1
        pets(id) = inputPet.copy(id = Some(id))

        inputPet.tags match{
          case Some(tagList) => tagList.map(addTag(_))
          case None => None
        }

        Future.value(id)
      }
    }

  //PUT: Update existing pet given a pet object
  //@return: updated pet

  /**
   * PUT: Updates an existing [[Pet]], while validating that a current version of
   * the [[Pet]] exists (a.k.a. an existing [[Pet]] has the same id as inputPet).
   * @param inputPet The [[Pet]] we want to replace the current [[Pet]] with. Must be passed with an ID.
   * @return
   */
  def updatePet(inputPet: Pet): Future[Pet] = inputPet.id match {
    case Some(id) =>
      if(pets.contains(id)) pets.synchronized{
        pets(id) = inputPet
        Future.value(inputPet)
      } else {
        Future.exception(MissingPet("Invalid id: doesn't exist"))
      }
    case None => Future.exception(MissingIdentifier(s"Missing id for pet: $inputPet"))
    //    case None => Future.exception(MissingIdentifier(s"Missing id for pet: $inputPet"))
  }

  //Helper Method: Get all the pets in the database
  def allPets: Future[List[Pet]] = Future.value(
    pets.synchronized(pets.toList.sortBy(_._1).map(_._2))
  )

  //GET: find pets by status
  def getPetsByStatus(s: Status): Future[Seq[Pet]] = {
    val allMatchesFut = for{
      petList <- allPets //List[Pet]
      allBool = petList.map(_.status)
    } yield petList.filter(_.status.map(_.code.equals(s.code)).getOrElse(false))
   allMatchesFut
  }

  //GET: find pets by tags
  //Muliple tags can be provided with comma seperated strings.
  def findPetsByTag(findTags: Seq[String]): Future[Seq[Pet]] = {
    val matchPets = for {
      p <- pets.values
      tagList <- p.tags
      pTagStr = tagList.map(_.name)
      if(findTags.forall(pTagStr.contains))
    } yield p
    Future(matchPets.toSeq.sortBy(_.id))
  }

  //DELETE
  def deletePet(id: Long): Future[Boolean] = Future.value(
    pets.synchronized {
      if (id == None) false else{
        if (pets.contains(id)) {
          pets.remove(id)
          true
        } else false
      }
    }
  )

  //POST: Update a pet in the store with form data
  def updatePetViaForm(petId: Long, n: Option[String], s: Option[Status]): Future[Pet] = {
      if(pets.contains(petId)) pets.synchronized{
        if (s != None) {pets(petId) = pets(petId).copy(status = s)}
        if (n != None) {pets(petId) = pets(petId).copy(name = n.get)}
//        pets(petId) = pets(petId).copy(name = n, status = Some(s))
        Future.value(pets(petId))
      } else Future.exception(MissingPet("Invalid id: doesn't exist"))
    }

  //POST: Upload an image
  def addImage(petId: Long, data: Array[Byte]): Future[String] =
    pets.synchronized {
      for {
        pet <- getPet(petId)
        photoId = photos.synchronized {
          val nextId = if (photos.isEmpty) 0 else photos.keys.max + 1
          photos(nextId) = data
          nextId
        }
        url = s"/photos/$photoId"
        _ <- updatePet(pet.copy(photoUrls = pet.photoUrls :+ url))
      } yield url
    }

  //+++++++++++++++++++++++++++++STORE METHODS BEGIN HERE+++++++++++++++++++++++++++++++++++++++++

  //GET: Returns the current inventory
  def getInventory: Future[Map[Status, Int]] = Future.value(
    pets.synchronized {
      pets.groupBy(_._2.status).flatMap {
        case (Some(status), keyVal) => Some(status -> keyVal.size)
        case (None, _) => None
      }
    }
  )

  //POST: Place an order for a pet
  def addOrder(order: Order): Future[Long] =
    orders.synchronized {
      order.id match{
        case Some(_) => Future.exception(InvalidInput("New order should not contain an id"))
        case None => orders.synchronized{
          val genId = if (orders.isEmpty) 0 else orders.keys.max + 1
          orders(genId) = order.copy(id = Option(genId))
          Future(genId)
        }
      }
    }

  //DELETE: Delete purchase order by ID
  def deleteOrder(id: Long): Future[Boolean] = Future.value(
    orders.synchronized{
      if (orders.contains(id)) {
        orders.remove(id)
        true
      } else false
    }
  )

  //GET: Find purchase order by ID
  def findOrder(id: Long): Future[Order] = Future.value(
    orders.synchronized{
      orders.getOrElse(id, throw OrderNotFound("Your order doesn't exist! :("))
    }
  )

  //============================STORE METHODS END HERE================================================

  //+++++++++++++++++++++++++++++USER METHODS BEGIN HERE+++++++++++++++++++++++++++++++++++++++++

  //POST: Create user
  def addUser(newGuy: User): Future[String] =
    users.synchronized {
      val inputName: String = newGuy.username
      if(users.values.exists(_.username == inputName)) throw RedundantUsername(s"Username $inputName is already taken.")
      else{
        newGuy.id match{
          case Some(_) => Future.exception(InvalidInput("New user should not contain an id"))
          case None => users.synchronized{
            val genId = if (users.isEmpty) 0 else users.keys.max + 1
            users(genId) = newGuy.copy(id = Option(genId))
            Future(newGuy.username)
//            Future(genId)
          }
        }
      }
    }

  //POST: Create list of users with given input array
  //In: List of user objects
//  def addUsersViaArray(addAll: Seq[User]): Future[Seq[String]] = {
//    val added: Seq[Future[Long]] = addAll.map(addUser)
//    val allNames: Seq[String] = addAll.map(_.username)
//    Future.value(allNames)
////    val allIds: Seq[Long] = for{
////      item: Future[Long] <- added
////      num: Long <- item
////    } yield num
////    Future.value(allIds)
//  }

  //POST: Create list of users with given input list
//  def addUsersViaList(addAll: Seq[User]): Future[Seq[String]] = {
//    addAll.map(addUser(_))
//    Future.value(addAll)

//    val added: Seq[Future[Long]] = addAll.map(addUser(_))
//    val allIds: Seq[Long] = for{
//      item: Future[Long] <- added
//      num: Long <- item
//    } yield num
//    Future.value(allIds)

//    val added: Seq[Future[Long]] = addAll.map(addUser)
//    val allNames: Seq[String] = addAll.map(_.username)
//    Future.value(allNames)
//  }

  //GET: Logs user into system
  /*
   ======  ===    ||====     ===
     ||  ||   ||  ||    || ||   ||
     ||  ||   ||  ||    || ||   ||
     ||    ===    ||====     ====
    */

  //GET: Logs out current logged in user session
  /*
   ======  ===    ||====     ===
     ||  ||   ||  ||    || ||   ||
     ||  ||   ||  ||    || ||   ||
     ||    ===    ||====     ====
    */

  //DELETE: Delete user
  /*
   ======  ===    ||====     ===
     ||  ||   ||  ||    || ||   ||
     ||  ||   ||  ||    || ||   ||
     ||    ===    ||====     ====
    */

  //GET: Get user by username, assume all usernames are unique
  def getUser(name: String): Future[User] = Future.value(
    users.synchronized{
      val pickMeIter: Iterable[User] = for{
        u <- users.values
        if u.username.equals(name)
      } yield u
//      pickMeIter.toSeq.take(1)
      if (pickMeIter.size == 0) throw MissingUser("This user doesn't exist!")  else pickMeIter.toSeq(0)
    }
  )

  //Helper method for testing
  def userExists(name: String): Future[Boolean] = Future.value(
    users.values.exists(_.username == name)
  )

  //PUT: Update user
  //You CANNOT change the username, this is how we find the original user to modify
  def updateUser(betterUser: User): Future[User] = Future.value(
    users.synchronized{
      val realId: Long = Await.result(getUser(betterUser.username)).id.getOrElse(
        throw MissingIdentifier("This user doesn't have an id."))
      users(realId) = betterUser.copy(id = Some(realId))
      users(realId)
    }
  )

  //============================USER METHODS END HERE================================================

}

