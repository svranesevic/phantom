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

import cats.effect._
import com.datastax.driver.core.{Session, Statement}
import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture}
import com.outworkers.phantom.ResultSet
import com.outworkers.phantom.builder.query.execution.{FutureMonad, GuavaAdapter, PromiseInterface}
import com.outworkers.phantom.connectors.SessionAugmenterImplicits

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Promise}

object IOPromiseInterface extends PromiseInterface[IO, IO]{

  override def empty[T]: IO[T] = IO.fromFuture(IO(Promise.apply[T].future))

  override def apply[T](value: T): IO[T] = IO.pure(value)

  override def become[T](source: IO[T], value: IO[T]): IO[T] =
    IO.raiseError(new Exception("This method call has been hit: become"))

  override def adapter(
    implicit monad: FutureMonad[IO]
  ): GuavaAdapter[IO] = new GuavaAdapter[IO] with SessionAugmenterImplicits {
    override def fromGuava[T](source: ListenableFuture[T])(
      implicit executor: ExecutionContext
    ): IO[T] =
      IO.async[T] { cb =>
        val callback = new FutureCallback[T] {
          def onSuccess(result: T): Unit = {
            cb(Right(result))
          }

          def onFailure(err: Throwable): Unit = {
            cb(Left(err))
          }
        }

        IO
          .delay(Futures.addCallback(source, callback, executor.asInstanceOf[ExecutionContextExecutor]))
          .as(None)
      }

    override def fromGuava(in: Statement)(
      implicit session: Session,
      ctx: ExecutionContext
    ): IO[ResultSet] =
      fromGuava(session.executeAsync(in)).map(res => ResultSet(res, session.protocolVersion))
  }

  override def future[T](source: IO[T]): IO[T] = source

  override def failed[T](exception: Throwable): IO[T] = IO.raiseError(exception)
}
