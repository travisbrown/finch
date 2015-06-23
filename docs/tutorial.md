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


### Serving simple services

Starting an HTTP server just takes a couple of lines. If you start a REPL with
`sbt core/console`, for example, you can write the following:

```scala
import com.twitter.finagle.Httpx
import com.twitter.util.Await
import io.finch.request._, io.finch.route._

val server = Httpx.serve(":8088", (Get /> "Hello world").toService)
```

This starts a server on your local port 8088 that responds to `GET` requests for
`/` with "Hello world" (if you get an "Address already in use" error from the
line above, you can change the port number to something else).

You can confirm that the server is running [in your
browser](http://localhost:8088/) or with a tool like curl:

```bash
$ curl -i http://localhost:8088/
HTTP/1.1 200 OK
Content-Type: text/plain;charset=utf-8
Content-Length: 11

Hello world
```

This works because `Get /> "Hello world"` is a `Router[String]`, and Finch knows
how to serialize a string to plain text. We can also write more interesting
routers and serve them (after stopping our current server):

```scala
Await.ready(server.close())

val server = Httpx.serve(":8088", (Get / int /> ("hey" * _)).toService)
```

Now [`http://localhost:8088/4`](http://localhost:8088/4) will show "heyheyheyhey",
since the `int` matches the `4` in the URL, and the `("hey" * _)` function takes
the matched integer and repeats "hey" that many times.

If we want to return more interesting responses, we can close this REPL and open
a new one in the `finch-argonaut` project with `sbt argonaut/console`. Now we can
return anything that Argonaut knows how to encode as JSON:

```scala
import com.twitter.finagle.Httpx
import com.twitter.util.Await
import io.finch.request._, io.finch.route._, io.finch.argonaut._

def intToMap(i: Int) = Map("a" -> i, "b" -> (i + 1))

val server = Httpx.serve(":8088", (Get / int /> intToMap).toService)
```

Now [`http://localhost:8088/4`](http://localhost:8088/4) shows `{"a":4,"b":5}`,
and the content type is `application/json`:

```bash
$ curl -i http://localhost:8088/4
HTTP/1.1 200 OK
Content-Type: application/json;charset=utf-8
Content-Length: 13

{"a":4,"b":5}
```

We can also serve our own types if we have an Argonaut `EncodeJson` instance for
them:

```scala
Await.ready(server.close())

import _root_.argonaut._, Argonaut._

case class Cat(name: String, breed: String, age: Int)

implicit val catEncoder: EncodeJson[Cat] = jencode3L(
  (cat: Cat) => (cat.name, cat.breed, cat.age)
)("name", "breed", "age")

val router = Get / "cat" /> Cat("Bob", "Persian", 7)
val server = Httpx.serve(":8088", router.toService)
```

Now [`http://localhost:8088/cat`](http://localhost:8088/cat) will give us back a
JSON object representing Bob:

```json
{"name":"Bob","breed":"Persian","age":7}
```

We can combine routers that return the same type with `|`:

```scala
Await.ready(server.close())

val catsRouter =
  Get / "bob" /> Cat("Bob", "Persian", 7) | Get / "sue" /> Cat("Sue", "Tabby", 5)

val server = Httpx.serve(":8088", catsRouter.toService)
```

And then:

```bash
$ curl -i http://localhost:8088/bob
HTTP/1.1 200 OK
Content-Type: application/json;charset=utf-8
Content-Length: 40

{"name":"Bob","breed":"Persian","age":7}
```

And:

```bash
$ curl -i http://localhost:8088/sue
HTTP/1.1 200 OK
Content-Type: application/json;charset=utf-8
Content-Length: 38

{"name":"Sue","breed":"Tabby","age":5}
```

For routers that return different types we need to use `:+:` instead of `|`:

```scala
Await.ready(server.close())

case class Dog(name: String, breed: String, age: Int)

implicit val dogEncoder: EncodeJson[Dog] = jencode3L(
  (dog: Dog) => (dog.name, dog.breed, dog.age)
)("name", "breed", "age")

val petsRouter =
  Get / "bob" /> Cat("Bob", "Persian", 7) :+: Get / "spot" /> Dog("Spot", "Terrier", 8)

val server = Httpx.serve(":8088", petsRouter.toService)
```

And then:

```bash
$ curl -i http://localhost:8088/spot
HTTP/1.1 200 OK
Content-Type: application/json;charset=utf-8
Content-Length: 41

{"name":"Spot","breed":"Terrier","age":8}
```

We can combine and serve any types with `:+:` as long as Finch knows how to
serve them.
