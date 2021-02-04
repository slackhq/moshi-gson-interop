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
import com.google.gson.JsonParseException
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.internal.bind.JsonTreeWriter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonToken
import com.slack.moshi.interop.gson.Serializer.GSON
import com.slack.moshi.interop.gson.Serializer.MOSHI
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonReader.Token
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import okio.Buffer
import java.lang.reflect.Type
import com.google.gson.stream.JsonReader as GsonReader
import com.google.gson.stream.JsonWriter as GsonWriter

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
      val gsonReader = GsonReader(it)
      gsonReader.isLenient = reader.isLenient
      delegate.read(gsonReader)
    }
  }

  override fun toJson(writer: JsonWriter, value: T?) {
    writer.valueSink().outputStream().writer().use {
      val gsonWriter = GsonWriter(it)
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
  override fun write(writer: GsonWriter, value: T?) {
    val adjustedDelegate = delegate
      .run { if (writer.serializeNulls) serializeNulls() else this }
      .run { if (writer.isLenient) lenient() else this }
    // Write to a raw JSON string first, then stream it back into a GSON writer
    // Moshi's toJsonValue() will up-convert all numbers to Double. We want to preserve data
    // exactly as-is!
    val serializedValue = adjustedDelegate.toJson(value)
    if (writer is JsonTreeWriter) {
      // See https://github.com/slackhq/moshi-gson-interop/issues/22
      // Even with https://github.com/google/gson/pull/1819, using JsonElement is not enough because
      val buffer = Buffer()
      buffer.writeUtf8(serializedValue)
      val reader = JsonReader.of(buffer)
      writer.write(reader)
    } else {
      writer.jsonValue(serializedValue)
    }
  }

  override fun read(reader: GsonReader): T? {
    val jsonString = reader.readToString()
    return delegate
      .run { if (reader.isLenient) lenient() else this }
      .fromJson(jsonString)
  }
}

/** Streams [this] reader to an encoded [String] for use with [JsonAdapter.fromJson]. */
private fun GsonReader.readToString(): String {
  val builder = StringBuilder()
  readTo(builder)
  return builder.toString()
}

/** Streams [this] reader into the target [builder] as an encoded JSON [String]. */
@Suppress("LongMethod")
private fun GsonReader.readTo(builder: StringBuilder) {
  when (val token = peek()) {
    JsonToken.STRING -> {
      builder.append('"')
      builder.append(nextString())
      builder.append('"')
    }
    JsonToken.NUMBER -> {
      // This allows moshi-gson-interop to preserve encoding from the reader,
      // avoiding issues like Gson's JsonElement API converting all
      // numbers potentially to Doubles.
      val lenient = isLenient
      try {
        builder.append(nextString())
      } finally {
        isLenient = lenient
      }
    }
    JsonToken.BOOLEAN -> {
      builder.append(nextBoolean().toString())
    }
    JsonToken.NULL -> {
      nextNull()
      builder.append("null")
    }
    JsonToken.BEGIN_ARRAY -> {
      builder.append('[')
      beginArray()
      var first = true
      while (hasNext()) {
        if (first) {
          first = false
        } else {
          builder.append(',')
        }
        readTo(builder)
      }
      builder.append(']')
    }
    JsonToken.BEGIN_OBJECT -> {
      builder.append('{')
      beginObject()
      var first = true
      while (hasNext()) {
        if (first) {
          first = false
        } else {
          builder.append(',')
        }
        builder.append('"')
        builder.append(nextName())
        builder.append('"')
        builder.append(':')
        readTo(builder)
      }
      endObject()
      builder.append('}')
    }
    JsonToken.END_DOCUMENT, JsonToken.NAME, JsonToken.END_OBJECT, JsonToken.END_ARRAY -> {
      throw JsonParseException("Unexpected token $token at $path")
    }
  }
}

/** Streams the contents of a given Moshi [reader] into this writer. */
private fun GsonWriter.write(reader: JsonReader) {
  when (val token = reader.peek()) {
    Token.BEGIN_ARRAY -> {
      reader.beginArray()
      beginArray()
      while (reader.hasNext()) {
        write(reader)
      }
      endArray()
      reader.endArray()
    }
    Token.BEGIN_OBJECT -> {
      reader.beginObject()
      beginObject()
      while (reader.hasNext()) {
        name(reader.nextName())
        write(reader)
      }
      endObject()
      reader.endObject()
    }
    Token.STRING -> value(reader.nextString())
    Token.NUMBER -> {
      // This allows moshi-gson-interop to preserve encoding from the reader,
      // avoiding issues like Moshi's `toJsonValue` API converting all
      // numbers potentially to Doubles.
      val lenient = reader.isLenient
      try {
        value(reader.nextString())
      } finally {
        reader.isLenient = lenient
      }
    }
    Token.BOOLEAN -> value(reader.nextBoolean())
    Token.NULL -> value(reader.nextNull<String>())
    Token.NAME, Token.END_ARRAY, Token.END_OBJECT, Token.END_DOCUMENT -> {
      throw JsonDataException("Unexpected token $token at ${reader.path}")
    }
  }
}
