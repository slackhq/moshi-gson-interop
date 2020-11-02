package com.slack.moshi.interop.gson

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** A simple functional interface for indicating when a class should be serialized with Moshi. */
public fun interface MoshiClassChecker {
  /**
   * Returns true if [rawType] should be serialized with Moshi or false if it should be serialized
   * with Gson.
   */
  public fun shouldUseMoshi(rawType: Class<*>): Boolean
}

/** Checks if a class is a built-in type (i.e. primitives or String) that Moshi natively supports. */
public object BuiltInsMoshiClassChecker : MoshiClassChecker {
  override fun shouldUseMoshi(rawType: Class<*>): Boolean {
    return rawType in MOSHI_BUILTIN_TYPES
  }

  override fun toString(): String = "BuiltInsMoshiClassChecker"
}

/** Checks if a class is annotated with Moshi's [JsonClass]. */
public object JsonClassMoshiClassChecker : MoshiClassChecker {
  override fun shouldUseMoshi(rawType: Class<*>): Boolean {
    return rawType.isAnnotationPresent(JsonClass::class.java)
  }

  override fun toString(): String = "JsonClassMoshiClassChecker"
}

/**
 * Checks enum classes with customizable heuristics. If [defaultToMoshi] is true, it will use Moshi
 * if no disqualifying heuristics are encountered. If it is false, Gson will be used.
 *
 * Logic flow:
 * * `defaultToMoshi = true` -> checks if it has [@SerializedName][SerializedName]-annotated members.
 * * `defaultToMoshi = false` -> checks if it has [@Json][Json]-annotated members.
 *
 * You usually want to default to Moshi unless you have some custom handling for enums in GSON.
 */
public class EnumMoshiClassChecker(private val defaultToMoshi: Boolean = true) : MoshiClassChecker {
  private val annotationToCheck = if (defaultToMoshi) SerializedName::class.java else Json::class.java
  override fun shouldUseMoshi(rawType: Class<*>): Boolean {
    if (rawType.isEnum) {
      @Suppress("UNCHECKED_CAST")
      val constants: Array<out Enum<*>> = rawType.enumConstants as Array<out Enum<*>>
      for (constant in constants) {
        if (rawType.getField(constant.name).isAnnotationPresent(annotationToCheck)) {
          // Return early
          return false
        }
      }
    }
    return defaultToMoshi
  }

  override fun toString(): String = "EnumMoshiClassChecker(defaultToMoshi=${defaultToMoshi})"
}

internal object StandardMoshiCheckers : MoshiClassChecker {
  private val defaultEnumChecker = EnumMoshiClassChecker(defaultToMoshi = true)
  override fun shouldUseMoshi(rawType: Class<*>): Boolean {
    return when {
      // Moshi can handle all these natively
      BuiltInsMoshiClassChecker.shouldUseMoshi(rawType) -> true
      // It's a moshi type, let Moshi handle it
      JsonClassMoshiClassChecker.shouldUseMoshi(rawType) -> true
      defaultEnumChecker.shouldUseMoshi(rawType) -> true
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