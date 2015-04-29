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
 * Ben Whitehead
 * Ryan Plessner
 * Pedro Viegas
 * Jens Halm
 */

package io.finch.request

import cats.data.Kleisli
import com.twitter.util.{Throw, Return, Future}
import io.catbird.util._
import io.finch._
import io.finch.request.items._

/**
 * A polymorphic request reader (a reader monad) that reads a [[Future]] of `A` from the request of type `R`.
 */
class PRequestReaderOps[R, A](rr: PRequestReader[R, A]) { self =>

  /**
   * Lifts this request reader into one that always succeeds, with an empty option representing failure.
   */
  def lift: PRequestReader[R, Option[A]] = Kleisli.kleisli(req => rr.run(req).liftToTry.map(_.toOption))

  /**
   * Validates the result of this request reader using a `predicate`. The rule is used for error reporting.
   *
   * @param rule text describing the rule being validated
   * @param predicate returns true if the data is valid
   *
   * @return a request reader that will return the value of this reader if it is valid.
   *         Otherwise the future fails with a [[io.finch.request.NotValid NotValid]] error.
   */
  def should(rule: String)(predicate: A => Boolean): PRequestReader[R, A] = rr.flatMapK { a =>
    if (predicate(a)) a.toFuture
    else NotValid("should " + rule).toFutureException[A]
  }

  /**
   * Validates the result of this request reader using a `predicate`. The rule is used for error reporting.
   *
   * @param rule text describing the rule being validated
   * @param predicate returns false if the data is valid
   *
   * @return a request reader that will return the value of this reader if it is valid.
   *         Otherwise the future fails with a [[io.finch.request.NotValid NotValid]] error.
   */
  def shouldNot(rule: String)(predicate: A => Boolean): PRequestReader[R, A] = should(s"not $rule.")(x => !predicate(x))

  /**
   * Validates the result of this request reader using a predefined `rule`. This method allows for rules to be reused
   * across multiple request readers.
   *
   * @param rule the predefined [[io.finch.request.ValidationRule ValidationRule]] that will return true if the data is
   *             valid
   *
   * @return a request reader that will return the value of this reader if it is valid.
   *         Otherwise the future fails with a [[io.finch.request.NotValid NotValid]] error.
   */
  def should(rule: ValidationRule[A]): PRequestReader[R, A] = should(rule.description)(rule.apply)

  /**
   * Validates the result of this request reader using a predefined `rule`. This method allows for rules to be reused
   * across multiple request readers.
   *
   * @param rule the predefined [[io.finch.request.ValidationRule ValidationRule]] that will return false if the data is
   *             valid
   *
   * @return a request reader that will return the value of this reader if it is valid.
   *         Otherwise the future fails with a [[io.finch.request.NotValid NotValid]] error.
   */
  def shouldNot(rule: ValidationRule[A]): PRequestReader[R, A] = shouldNot(rule.description)(rule.apply)
}

/**
 * Convenience methods for creating new reader instances.
 */
object RequestReader {

  /**
   * Creates a new [[io.finch.request.RequestReader RequestReader]] that always succeeds, producing the specified value.
   *
   * @param value the value the new reader should produce
   * @return a new reader that always succeeds, producing the specified value
   */
  def value[A](value: A): RequestReader[A] = const[A](value.toFuture)

  /**
   * Creates a new [[io.finch.request.RequestReader RequestReader]] that always fails, producing the specified
   * exception.
   *
   * @param exc the exception the new reader should produce
   * @return a new reader that always fails, producing the specified exception
   */
  def exception[A](exc: Throwable): RequestReader[A] = const[A](exc.toFutureException[A])

  /**
   * Creates a new [[io.finch.request.RequestReader RequestReader]] that always produces the specified value. It will
   * succeed if the given `Future` succeeds and fail if the `Future` fails.
   *
   * @param value the value the new reader should produce
   * @return a new reader that always produces the specified value
   */
  def const[A](value: Future[A]): RequestReader[A] = Kleisli.kleisli(_ => value)

  /**
   * Creates a new [[io.finch.request.RequestReader RequestReader]] that reads the result from the request.
   *
   * @param f the function to apply to the request
   * @return a new reader that reads the result from the request
   */
  def apply[R, A](f: R => A): PRequestReader[R, A] = Kleisli.kleisli(f(_).toFuture)
}
