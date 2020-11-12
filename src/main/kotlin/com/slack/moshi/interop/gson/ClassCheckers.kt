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

import com.google.gson.annotations.SerializedName
import com.slack.moshi.interop.gson.Serializer.GSON
import com.slack.moshi.interop.gson.Serializer.MOSHI
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** A simple functional interface for indicating when a class should be serialized with Moshi. */
public fun interface ClassChecker {
  /**
   * Returns whichever [Serializer] should be used for [rawType] or null if it has no opinion.
   */
  public fun serializerFor(rawType: Class<*>): Serializer?
}

/** Indicates a serializer to use, either [MOSHI] or [GSON]. */
public enum class Serializer {
  MOSHI, GSON
}

/** Checks if a class is a built-in type (i.e. primitives or String) that Moshi natively supports. */
public object BuiltInsClassChecker : ClassChecker {
  override fun serializerFor(rawType: Class<*>): Serializer? {
    return if (rawType in MOSHI_BUILTIN_TYPES) MOSHI else null
  }

  override fun toString(): String = "BuiltInsClassChecker"
}

/** Checks if a class is annotated with Moshi's [JsonClass]. */
public object JsonClassClassChecker : ClassChecker {
  override fun serializerFor(rawType: Class<*>): Serializer? {
    return if (rawType.isAnnotationPresent(JsonClass::class.java)) MOSHI else null
  }

  override fun toString(): String = "JsonClassClassChecker"
}

/**
 * Checks enum classes with customizable heuristics.
 *
 * Logic flow:
 * * Check if it has [@SerializedName][SerializedName]-annotated members. Use [GSON] if so.
 * * Check if it has [@Json][Json]-annotated members. Use [MOSHI] if so.
 * * Else [defaultSerializer].
 *
 * You usually want to default to Moshi unless you have some custom handling for enums in GSON.
 */
public class EnumClassChecker(
  private val defaultSerializer: Serializer = MOSHI,
  private val logger: ((String) -> Unit)? = null
) : ClassChecker {
  @Suppress("ReturnCount")
  override fun serializerFor(rawType: Class<*>): Serializer? {
    if (rawType.isEnum) {
      @Suppress("UNCHECKED_CAST")
      val constants: Array<out Enum<*>> = rawType.enumConstants as Array<out Enum<*>>
      for (constant in constants) {
        val field = rawType.getField(constant.name)
        if (field.isAnnotationPresent(SerializedName::class.java)) {
          logger?.invoke("🧠 Picking GSON for enum $rawType based on @SerializedName-annotated member $field.")
          return GSON
        } else if (field.isAnnotationPresent(Json::class.java)) {
          logger?.invoke("🧠 Picking Moshi for enum $rawType based on @Json-annotated member $field.")
          return MOSHI
        }
      }
      logger?.invoke("🧠 Defaulting to $defaultSerializer for enum $rawType.")
      return defaultSerializer
    }
    return null
  }

  override fun toString(): String = "EnumClassChecker(defaultSerializer=$defaultSerializer)"
}

internal class StandardClassCheckers(
  private val defaultSerializer: Serializer?,
  private val logger: ((String) -> Unit)?
) : ClassChecker {

  private val defaultEnumChecker = EnumClassChecker(defaultSerializer = MOSHI, logger = logger)

  @Suppress("ReturnCount")
  override fun serializerFor(rawType: Class<*>): Serializer {
    BuiltInsClassChecker.serializerFor(rawType)?.let {
      logger?.invoke("🧠 Picking $it for built-in type $rawType")
      return it
    }
    JsonClassClassChecker.serializerFor(rawType)?.let {
      logger?.invoke("🧠 Picking $it for @JsonClass-annotated type $rawType")
      return it
    }
    defaultEnumChecker.serializerFor(rawType)?.let {
      return it
    }

    // Nothing else worked.
    logger?.invoke("🧠 No decision for $rawType, defaulting to $defaultSerializer")
    return defaultSerializer ?: error("No serializer found for $rawType")
  }

  override fun toString(): String {
    return "StandardMoshiCheckers"
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
