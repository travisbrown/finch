/*
 * Copyright 2014, by Vladimir Kostyukov and Contributors.
 *
 * This file is a part of a Finch library that may be found at
 *
 *      https://github.com/finagle/finch
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributor(s):
 * Jens Halm
 */
package io.finch.request

import io.catbird.util._
import io.finch.HttpRequest
import org.scalatest.{FlatSpec, Matchers}

import com.twitter.finagle.httpx.Request
import com.twitter.util.{Await, Throw, Try}
import items._

class ApplicativeRequestReaderSpec extends FlatSpec with Matchers {

  case class MyReq(http: HttpRequest, i: Int)
  implicit val reqEv: MyReq %> HttpRequest = View(_.http)

  val reader: RequestReader[(Int, Double, Int)] = (
    param("a").as[Int] ::
    param("b").as[Double] ::
    param("c").as[Int]
  ).asTuple
  
  it should "produce two ParamNotFound errors if two parameters are missing" in {
    val request = Request("b" -> "7.7")
    Await.result(reader.run(request).liftToTry) shouldBe Throw(RequestErrors(Seq(
      NotPresent,
      NotPresent
    )))
  }
  
  it should "parse all integers and doubles" in {
    val request = Request("a"->"9", "b"->"7.7", "c"->"5")
    Await.result(reader.run(request)) shouldBe ((9, 7.7, 5))
  }

  it should "be polymorphic in terms of request type" in {
    val i: PRequestReader[MyReq, Int] = RequestReader(_.i)
    val a = (i :: param("a")) ~> ((_: Int) + (_: String))
    val b = for {
      ii <- i
      aa <- param("a")
    } yield aa + ii

    Await.result(a.run(MyReq(Request("a" -> "foo"), 10))) shouldBe "10foo"
    Await.result(b.run(MyReq(Request("a" -> "foo"), 10))) shouldBe "foo10"
  }

  it should "compiles with both implicits Generic and DecodeRequest in the scope" in {
    case class MyString(s: String)
    implicit val decodeMyString: DecodeRequest[MyString] =
      DecodeRequest { s => Try(MyString(s)) }

    val foo: RequestReader[MyString] = param("a").as[MyString]
    Await.result(foo.run(Request("a" -> "foo"))) shouldBe MyString("foo")

    case class MyInt(i: Int)
    implicit val decodeMyInt: DecodeRequest[MyInt] =
      DecodeRequest { s => Try(MyInt(s.toInt)) }

    val bar: RequestReader[MyInt] = param("a").as[MyInt]
    Await.result(bar.run(Request("a" -> "100"))) shouldBe MyInt(100)
  }
}
