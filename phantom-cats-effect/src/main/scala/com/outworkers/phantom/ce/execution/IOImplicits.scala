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
package com.outworkers.phantom.ce.execution

import cats.effect.IO
import com.outworkers.phantom.builder.query.execution.{
  FutureMonad,
  PromiseInterface
}

import scala.concurrent.ExecutionContext

object IOImplicits {

  implicit val ioInterface: PromiseInterface[IO, IO] = IOPromiseInterface

  implicit val ioMonad: FutureMonad[IO] = new FutureMonad[IO] {

    override def flatMap[A, B](source: IO[A])(fn: (A) => IO[B])(
        implicit ctx: ExecutionContext
    ): IO[B] = (source flatMap fn).evalOn(ctx)

    override def map[A, B](source: IO[A])(f: (A) => B)(
        implicit ctx: ExecutionContext
    ): IO[B] = (source map f).evalOn(ctx)

    override def pure[A](source: A): IO[A] = IO.pure(source)
  }
}
