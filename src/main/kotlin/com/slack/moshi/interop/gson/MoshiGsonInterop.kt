/*
 * Copyright (C) 2020 Slack Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.slack.moshi.interop.gson

import com.google.gson.Gson
import com.squareup.moshi.Moshi
import kotlin.DeprecationLevel.ERROR

/**
 * Connects this [Moshi] instance to a [Gson] instance for interop. This should be called with the
 * final versions of the input instances and then the returned instances should be used.
 */
public fun Moshi.interopWith(gson: Gson): Pair<Moshi, Gson> {
  return InteropBuilder(this, gson).build()
}

/**
 * Connects this [Moshi] instance to a [Gson] instance for interop. This should be called with the
 * final versions of the input instances and then the returned instances should be used.
 */
@Deprecated(
  message = "Use interopBuilder",
  replaceWith = ReplaceWith("interopBuilder(gson).addClassChecker(moshiClassChecker)"),
  level = ERROR
)
public fun Moshi.interopWith(
  gson: Gson,
  moshiClassChecker: MoshiClassChecker,
): Pair<Moshi, Gson> {
  val builder = InteropBuilder(this, gson)
    .addClassChecker(moshiClassChecker)
  return builder.build()
}

/**
 * Returns an [InteropBuilder] to connect this [Moshi] instance to a [Gson] instance for interop.
 * This should be called with the final versions of the input instances and then the returned
 * instances should be used.
 */
public fun Moshi.interopBuilder(gson: Gson): InteropBuilder = InteropBuilder(this, gson)
