/*
 * Copyright 2013 - 2020 Outworkers Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.outworkers.phantom.ce

import com.outworkers.phantom.PhantomSuite
import cats.effect.IO
import cats.effect.unsafe.IORuntime
import cats.syntax.option._

import scala.util.{Failure, Success, Try}

trait IOSuite extends PhantomSuite {

  private implicit val runtime: IORuntime = IORuntime.global

  implicit def IOFutureConcept[T](f: IO[T]): FutureConcept[T] = new FutureConcept[T] {
    override def eitherValue: Option[Either[Throwable, T]] =
      Try(f.unsafeRunSync()) match {
        case Success(r) => Right(r).some
        case Failure(t) => Left(t).some
      }

    override def isExpired: Boolean = false

    override def isCanceled: Boolean = false
  }
}