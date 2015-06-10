## Getting started with Finch

This document contains some self-contained examples demonstrating how to get
started with Finch. All of the commands below can be pasted into the Scala REPL
that you get when you run `sbt console`.

### Request readers and the reader monad

First let's define a couple of HTTP requests. Note that you'll need to type
`:paste` into the Scala console in order to get `createPet` to parse correctly.

```scala
import com.twitter.finagle.httpx.{Request, RequestBuilder}
import com.twitter.io.Buf

val getByTagsRequest =
  Request("http://localhost/pet/findByTags", "tags" -> "cat,dog")

val createPetRequest = RequestBuilder().url("http://localhost/pet/findByTags")
  .setHeader("Content-Type", "application/json")
  .setHeader("Accept", "application/json")
  .buildPut(
    Buf.Utf8(
      """
        {
          "id": 0,
          "category": { "id": 0, "name": "dog" },
          "name": "Boatswain",
          "photoUrls": [],
          "tags": [ { "id": 1, "name": "dog" } ],
          "status": "available"
        }
      """
    )
  )
```

Next we'll make some Scala case classes to hold pet info:

```scala
case class Category(id: Long, name: String)
case class Tag(id: Long, name: String)

sealed trait Status
case object Available extends Status
case object HasAHome extends Status

case class Pet(
  id: Long,
  name: String,
  photoUrls: Seq[String],
  category: Category,
  tags: Seq[Tag],
  status: Status
)
```

We'll also define some [Argonaut](http://argonaut.io/) codecs that
describe how to convert JSON into our case classes.

```scala
import _root_.argonaut._, Argonaut._

implicit val categoryDecode: DecodeJson[Category] =
  jdecode2L(Category.apply)("id", "name")

implicit val tagDecode: DecodeJson[Tag] =
  jdecode2L(Tag.apply)("id", "name")

implicit val statusDecode: DecodeJson[Status] =
  DecodeJson { c =>
    c.as[String].flatMap[Status] {
      case "available" => DecodeResult.ok(Available)
      case "unavailable" => DecodeResult.ok(HasAHome)
      case other => DecodeResult.fail(s"Unknown status: $other", c.history)
    }
  }

implicit val petDecode: DecodeJson[Pet] =
  jdecode6L(Pet.apply)("id", "name", "photoUrls", "category", "tags", "status")
```

Now we can create some request readers to try out on these requests:

```scala
import io.finch.request._

val petTagsReader: RequestReader[Seq[String]] = param("tags").map { tags =>
  tags.split(",").map(_.trim)
}

val createdPetReader: RequestReader[Pet] = body.as[Pet]
```

Now these two readers are essentially just functions from `Request` to
`Future[Seq[String]]` and `Future[Pet]`, and we can try them out by applying
them to our requests:

```scala
val successfulPetTagsResult = petTagsReader(getByTagsRequest)
val unsuccessfulPetTagsResult = petTagsReader(createPetRequest)

val successfulCreatePetResult = createdPetReader(createPetRequest)
val unsuccessfulCreatePetResult = createdPetReader(getByTagsRequest)
```

These values are futures—that is, they represent an asynchronous computation
that may not be finished yet. We can get the value of the successful readers in
the REPL with `Await`:

```scala
import com.twitter.util.Await

val tags = Await.result(successfulPetTagsResult)
val pet = Await.result(successfulCreatePetResult)
```

Which shows us the following:

```scala
scala> val tags = Await.result(successfulPetTagsResult)
tags: Seq[String] = ArraySeq(cat, dog)

scala> val pet = Await.result(successfulCreatePetResult)
pet: Pet = Pet(0,Boatswain,Vector(),Category(0,dog),Vector(Tag(1,dog)),Available)
```

And we can print the error messages for the failed readers with `onFailure`:

```scala
unsuccessfulPetTagsResult.onFailure(println)
unsuccessfulCreatePetResult.onFailure(println)
```

Which prints this:

```scala
scala> unsuccessfulPetTagsResult.onFailure(println)
io.finch.request.NotPresent: Required param 'tags' not present in the request.
res8: com.twitter.util.Future[Seq[String]] = ...

scala> unsuccessfulCreatePetResult.onFailure(println)
io.finch.request.NotPresent: Required body not present in the request.
res9: com.twitter.util.Future[Pet] = ...
```

So our `petTagsReader` and `createdPetReader` are two pieces that we can use
when writing a web service that processes incoming HTTP requests like the
examples we defined above.
