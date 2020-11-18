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
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.internal.bind.JsonTreeWriter
import com.google.gson.internal.bind.TypeAdapters
import com.google.gson.reflect.TypeToken
import com.slack.moshi.interop.gson.Serializer.GSON
import com.slack.moshi.interop.gson.Serializer.MOSHI
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import java.lang.reflect.Type

/**
 * A simple builder API for customizing interop logic between a given [moshi] and [gson] instance.
 */
public class InteropBuilder internal constructor(
  private val moshi: Moshi,
  private val gson: Gson,
) {
  public val checkers: MutableList<ClassChecker> = mutableListOf()

  // Since we assume this codebase is migrating from GSON, use it as a last resort.
  private var defaultSerializer: Serializer? = GSON

  private var logger: ((String) -> Unit)? = null

  /**
   * Sets the default serializer to use if no other checkers match. Use null to error in the event
   * of no matches.
   */
  public fun defaultSerializer(serializer: Serializer?): InteropBuilder = apply {
    this.defaultSerializer = serializer
  }

  /**
   * Optional callback for any internal logging. Useful for debugging purposes.
   */
  public fun logger(logger: ((String) -> Unit)?): InteropBuilder = apply {
    this.logger = logger
  }

  public fun addMoshiType(clazz: Class<*>): InteropBuilder = apply {
    checkers += TypeChecker(MOSHI, clazz.kotlin.javaObjectType)
  }

  public fun addGsonType(clazz: Class<*>): InteropBuilder = apply {
    checkers += TypeChecker(GSON, clazz.kotlin.javaObjectType)
  }

  public fun addMoshiFactory(factory: JsonAdapter.Factory): InteropBuilder = apply {
    checkers += FactoryChecker(MOSHI) { rawType ->
      factory.create(rawType, emptySet(), moshi) != null
    }
  }

  public fun addGsonFactory(factory: TypeAdapterFactory): InteropBuilder = apply {
    checkers += FactoryChecker(GSON) { rawType ->
      factory.create(gson, TypeToken.get(rawType)) != null
    }
  }

  public fun addClassChecker(classChecker: ClassChecker): InteropBuilder = apply {
    checkers += classChecker
  }

  public fun build(): MoshiGsonInterop {
    logger?.invoke("💡 Building moshi-gson interop with default serializer of $defaultSerializer")
    return MoshiGsonInteropImpl(
      seedMoshi = moshi,
      seedGson = gson,
      checkers = checkers + StandardClassCheckers(defaultSerializer, logger),
      logger = logger
    )
  }
}

public inline fun <reified T> InteropBuilder.addMoshiType(): InteropBuilder {
  return addMoshiType(T::class.java)
}

public inline fun <reified T> InteropBuilder.addGsonType(): InteropBuilder {
  return addGsonType(T::class.java)
}

private data class TypeChecker(
  private val serializer: Serializer,
  val clazz: Class<*>,
) : ClassChecker {
  override fun serializerFor(rawType: Class<*>): Serializer? {
    return serializer.takeIf { rawType.kotlin.javaObjectType == clazz }
  }
}

private data class FactoryChecker(
  private val serializer: Serializer,
  val body: (Class<*>) -> Boolean,
) : ClassChecker {
  override fun serializerFor(rawType: Class<*>): Serializer? {
    return serializer.takeIf { body(rawType) }
  }
}

private class MoshiGsonInteropImpl(
  seedMoshi: Moshi,
  seedGson: Gson,
  checkers: List<ClassChecker>,
  logger: ((String) -> Unit)?,
) : MoshiGsonInterop {

  override val moshi: Moshi = seedMoshi.newBuilder()
    .add(MoshiGsonInteropJsonAdapterFactory(this, checkers, logger))
    .build()

  override val gson: Gson = seedGson.newBuilder()
    .registerTypeAdapterFactory(MoshiGsonInteropTypeAdapterFactory(this, checkers, logger))
    .create()
}

/**
 * An interop-ing [JsonAdapter.Factory] that tries to intelligently defer to a `gson` instance for
 * appropriate types.
 */
private class MoshiGsonInteropJsonAdapterFactory(
  private val interop: MoshiGsonInterop,
  private val checkers: List<ClassChecker>,
  private val logger: ((String) -> Unit)?,
) : JsonAdapter.Factory {
  override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
    if (annotations.isNotEmpty() || type !is Class<*>) return null
    return if (checkers.any { it.serializerFor(type) == MOSHI }) {
      moshi.nextAdapter<Any>(this, type, annotations)
    } else {
      logger?.invoke("⮑ Gson: $type")
      GsonDelegatingJsonAdapter(interop.gson.getAdapter(type)).nullSafe()
    }
  }
}

internal class GsonDelegatingJsonAdapter<T>(
  private val delegate: TypeAdapter<T>,
) : JsonAdapter<T>() {
  override fun fromJson(reader: JsonReader): T? {
    return reader.nextSource().inputStream().reader().use {
      val gsonReader = com.google.gson.stream.JsonReader(it)
      gsonReader.isLenient = reader.isLenient
      delegate.read(gsonReader)
    }
  }

  override fun toJson(writer: JsonWriter, value: T?) {
    writer.valueSink().outputStream().writer().use {
      val gsonWriter = com.google.gson.stream.JsonWriter(it)
      gsonWriter.isLenient = writer.isLenient
      gsonWriter.serializeNulls = writer.serializeNulls
      delegate.write(gsonWriter, value)
    }
  }
}

/**
 * An interop-ing [TypeAdapterFactory] that tries to intelligently defer to a `moshi` instance for
 * appropriate types.
 */
private class MoshiGsonInteropTypeAdapterFactory(
  private val interop: MoshiGsonInterop,
  private val checkers: List<ClassChecker>,
  private val logger: ((String) -> Unit)?
) : TypeAdapterFactory {
  override fun <T> create(gson: Gson, typeToken: TypeToken<T>): TypeAdapter<T>? {
    val type = typeToken.type
    if (type !is Class<*>) return null

    @Suppress("UNCHECKED_CAST")
    return if (checkers.any { it.serializerFor(type) == GSON }) {
      gson.getDelegateAdapter(this, typeToken)
    } else {
      logger?.invoke("⮑ Moshi: $type")
      MoshiDelegatingTypeAdapter(interop.moshi.adapter<Any>(type)).nullSafe()
    } as TypeAdapter<T>
  }
}

internal class MoshiDelegatingTypeAdapter<T>(
  private val delegate: JsonAdapter<T>
) : TypeAdapter<T>() {
  override fun write(writer: com.google.gson.stream.JsonWriter, value: T?) {
    val adjustedDelegate = delegate
      .run { if (writer.serializeNulls) serializeNulls() else this }
      .run { if (writer.isLenient) lenient() else this }
    if (writer is JsonTreeWriter) {
      // https://github.com/slackhq/moshi-gson-interop/issues/22
      // Pending https://github.com/google/gson/pull/1819
      val jsonValue = adjustedDelegate.toJsonValue(value)
      val jsonElement = jsonValue.toJsonElement()
      TypeAdapters.JSON_ELEMENT.write(writer, jsonElement)
    } else {
      val serializedValue = adjustedDelegate.toJson(value)
      writer.jsonValue(serializedValue)
    }
  }

  override fun read(reader: com.google.gson.stream.JsonReader): T? {
    val jsonValue = JsonParser.parseReader(reader).toJsonValue()
    return delegate
      .run { if (reader.isLenient) lenient() else this }
      .fromJsonValue(jsonValue)
  }
}

/** Converts a [JsonElement] to a simple object for use with [JsonAdapter.fromJsonValue]. */
private fun JsonElement.toJsonValue(): Any? {
  return when (this) {
    is JsonArray -> {
      map { it.toJsonValue() }
    }
    is JsonObject -> {
      entrySet().associate { (key, elementValue) ->
        key to elementValue.toJsonValue()
      }
    }
    is JsonPrimitive -> {
      when {
        isBoolean -> asBoolean
        isNumber -> asNumber
        isString -> asString
        else -> error("Unknown type: $this")
      }
    }
    is JsonNull -> null
    else -> error("Not possible")
  }
}

private fun Any?.toJsonElement(): JsonElement {
  return when (this) {
    null -> JsonNull.INSTANCE
    is Boolean -> JsonPrimitive(this)
    is Char -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is String -> JsonPrimitive(this)
    is Array<*> -> {
      JsonArray(size).apply {
        for (element in this) {
          add(element.toJsonElement())
        }
      }
    }
    is Collection<*> -> {
      JsonArray(size).apply {
        for (element in this@toJsonElement) {
          add(element.toJsonElement())
        }
      }
    }
    is Map<*, *> -> {
      JsonObject().apply {
        for ((k, v) in entries) {
          check(k is String) {
            "JSON only supports String keys!"
          }
          add(k, v.toJsonElement())
        }
      }
    }
    else -> error("Unrecognized JsonValue type: $this")
  }
}
