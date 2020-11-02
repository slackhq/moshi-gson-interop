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
import com.google.gson.annotations.SerializedName
import com.squareup.moshi.JsonClass
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

/** A simple functional interface for indicating when a class should be serialized with Moshi. */
public fun interface MoshiClassChecker {
  /**
   * Returns true if [rawType] should be serialized with Moshi or false if it should be serialized
   * with Gson.
   */
  public fun shouldUseMoshi(rawType: Class<*>): Boolean
}

/**
 * The default [MoshiClassChecker] behavior. This checks a few things when deciding:
 * * Is it a "built in" type like primitives or String? -> Moshi
 * * Is it annotated with [@JsonClass][JsonClass]? -> Moshi
 * * Is it an enum with no [@SerializedName][SerializedName]-annotated members? -> Moshi
 * * Else -> Gson
 */
public object DefaultMoshiClassChecker : MoshiClassChecker {
  override fun shouldUseMoshi(rawType: Class<*>): Boolean {
    return when {
      // Moshi can handle all these natively
      rawType in MOSHI_BUILTIN_TYPES -> true
      // It's a moshi type, let Moshi handle it
      rawType.isAnnotationPresent(JsonClass::class.java) -> true
      rawType.isEnum -> {
        // If it has no Gson @SerializedName annotations, we can use Moshi
        @Suppress("UNCHECKED_CAST")
        val constants: Array<out Enum<*>> = rawType.enumConstants as Array<out Enum<*>>
        for (constant in constants) {
          if (rawType.getField(constant.name).isAnnotationPresent(SerializedName::class.java)) {
            // Return early
            return false
          }
        }
        true
      }
      else -> false
    }
  }
}

private val MOSHI_BUILTIN_TYPES = setOf(
  Boolean::class.javaPrimitiveType,
  Boolean::class.javaObjectType,
  Byte::class.javaPrimitiveType,
  Byte::class.javaObjectType,
  Char::class.javaPrimitiveType,
  Character::class.javaObjectType,
  Double::class.javaPrimitiveType,
  Double::class.javaObjectType,
  Float::class.javaPrimitiveType,
  Float::class.javaObjectType,
  Int::class.javaPrimitiveType,
  Integer::class.javaObjectType,
  Long::class.javaPrimitiveType,
  Long::class.javaObjectType,
  Short::class.javaPrimitiveType,
  Short::class.javaObjectType,
  Void::class.javaPrimitiveType,
  Void::class.javaObjectType,
  String::class.java,
  Any::class.java
)
