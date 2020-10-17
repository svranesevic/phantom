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
import cats.effect.unsafe.IORuntime
import com.outworkers.phantom.ops.QueryContext

import scala.concurrent.duration._
import scala.concurrent.TimeoutException

class IOQueryContext extends QueryContext[IO, IO, Duration](10.seconds)(
      IOImplicits.ioMonad,
      IOImplicits.ioInterface
    ) {

  implicit private val runtime: IORuntime = IORuntime.global

  override def blockAwait[T](f: IO[T], timeout: Duration): T = {
    f
      .unsafeRunTimed(FiniteDuration(timeout.length, timeout.unit))
      .getOrElse(throw new TimeoutException())
  }
}
