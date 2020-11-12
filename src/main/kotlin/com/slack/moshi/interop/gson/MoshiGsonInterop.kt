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

/**
 * Connects this [Moshi] instance to a [Gson] instance for interop. This should be called with the
 * final versions of the input instances and then the returned instances should be used.
 */
public fun Moshi.interopWith(gson: Gson): MoshiGsonInterop {
  return InteropBuilder(this, gson).build()
}

/**
 * Returns an [InteropBuilder] to connect this [Moshi] instance to a [Gson] instance for interop.
 * This should be called with the final versions of the input instances and then the returned
 * instances should be used.
 */
public fun Moshi.interopBuilder(gson: Gson): InteropBuilder = InteropBuilder(this, gson)

/** Represents an interop'd pair of [moshi] and [gson] instances. */
public interface MoshiGsonInterop {
  public val moshi: Moshi
  public val gson: Gson
}

public operator fun MoshiGsonInterop.component1(): Moshi = moshi
public operator fun MoshiGsonInterop.component2(): Gson = gson
