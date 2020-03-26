/*
 * Copyright (c) 2020 MOLO17
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.molo17.couchbase.lite

import com.couchbase.lite.Query
import com.couchbase.lite.QueryChange
import com.couchbase.lite.ResultSet
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map

/**
 * Returns a [Flow] that emits the Query [ResultSet] every time the underlying
 * data set changes.
 *
 * If the query fails, the [Flow] throws an error.
 */
fun Query.asFlow(): Flow<ResultSet> = asQueryFlow().map { it.results }

/**
 * Returns a [Flow] that maps the Query [ResultSet] to instances of a class
 * that can be created using the given [factory] lambda.
 *
 * Example of usage:
 *
 * ```
 * class User(map: Map<String, Any?>) {
 *   val name: String by map
 *   val surname: String by map
 *   val age: Int by map
 * }
 *
 * val users: Flow<List<User>> = query.asObjectsFlow(::User)
 * ```
 *
 * Using Kotlin Map delegation for creating such instances is a great shorthand.
 *
 * @param factory the lambda used for creating object instances.
 */
fun <T : Any> Query.asObjectsFlow(
    factory: (Map<String, Any?>) -> T
): Flow<List<T>> = asQueryFlow().map { queryChange -> queryChange.results.toObjects(factory) }

///////////////////////////////////////////////////////////////////////////
// Private functions
///////////////////////////////////////////////////////////////////////////

private fun Query.asQueryFlow(): Flow<QueryChange> = callbackFlow {
    val token = addChangeListener { queryChange ->
        if (queryChange.error == null) {
            offer(queryChange)
        } else {
            throw queryChange.error
        }
    }
    awaitClose { removeChangeListener(token) }
    // Run the query.
    execute()
}