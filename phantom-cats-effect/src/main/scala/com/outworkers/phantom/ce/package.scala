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
package com.outworkers.phantom

import cats.effect.IO
import com.outworkers.phantom.builder.ConsistencyBound
import com.outworkers.phantom.builder.query.{QueryOptions, RootQuery}
import com.outworkers.phantom.builder.query.engine.CQLQuery
import com.outworkers.phantom.builder.query.execution.{
  ExecutableCqlQuery,
  ExecutableStatements,
  QueryCollection,
  QueryInterface
}
import com.outworkers.phantom.ce.execution.IOImplicits.ioMonad
import com.outworkers.phantom.ce.execution.IOQueryContext

import scala.collection.compat._
import scala.concurrent.ExecutionContext

package object ce extends IOQueryContext with DefaultImports {

  implicit class ExecuteQueries[M[X] <: IterableOnce[X]](
      val qc: QueryCollection[M])
      extends AnyVal {
    def executable(): ExecutableStatements[IO, M] = {
      new ExecutableStatements[IO, M](qc)
    }

    def future(implicit session: Session,
               fbf: Factory[IO[ResultSet], M[IO[ResultSet]]],
               ebf: Factory[ResultSet, M[ResultSet]]): IO[M[ResultSet]] =
      executable().future()
  }

  /**
    * Method that allows executing a simple query straight from text, by-passing the entire mapping layer
    * but leveraging the execution layer.
    *
    * @param str     The input [[com.outworkers.phantom.builder.query.engine.CQLQuery]] to execute.
    * @param options The [[com.outworkers.phantom.builder.query.QueryOptions]] to pass alongside the query.
    * @return A IO wrapping a database result set.
    */
  def cql(
      str: CQLQuery,
      options: QueryOptions
  ): QueryInterface[IO] = new QueryInterface[IO]() {
    override def executableQuery: ExecutableCqlQuery =
      ExecutableCqlQuery(str, options, Nil)
  }

  /**
    * Method that allows executing a simple query straight from text, by-passing the entire mapping layer
    * but leveraging the execution layer.
    *
    * @param str     The input [[com.outworkers.phantom.builder.query.engine.CQLQuery]] to execute.
    * @param options The [[com.outworkers.phantom.builder.query.QueryOptions]] to pass alongside the query.
    * @return A IO wrapping a database result set.
    */
  def cql(
      str: String,
      options: QueryOptions = QueryOptions.empty
  ): QueryInterface[IO] = cql(CQLQuery(str), options)

  implicit def toIO[
      Table <: CassandraTable[Table, _],
      Record,
      Status <: ConsistencyBound
  ](query: RootQuery[Table, Record, Status])(
      implicit session: Session,
      ctx: ExecutionContext
  ): IO[ResultSet] =
    promiseInterface.adapter.fromGuava(query.executableQuery)(session, ctx)
}
