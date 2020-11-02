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
import com.google.gson.reflect.TypeToken
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
  public val checkers: MutableList<MoshiClassChecker> = mutableListOf()

  public fun addMoshiType(clazz: Class<*>): InteropBuilder = apply {
    checkers += MoshiClassChecker {
      it == clazz
    }
  }

  public fun addGsonType(clazz: Class<*>): InteropBuilder = apply {
    checkers += MoshiClassChecker {
      it != clazz
    }
  }

  public fun addMoshiFactory(factory: JsonAdapter.Factory): InteropBuilder = apply {
    checkers += MoshiClassChecker {
      factory.create(it, emptySet(), moshi) != null
    }
  }

  public fun addGsonFactory(factory: TypeAdapterFactory): InteropBuilder = apply {
    checkers += MoshiClassChecker {
      factory.create(gson, TypeToken.get(it)) != null
    }
  }

  public fun addClassChecker(classChecker: MoshiClassChecker): InteropBuilder = apply {
    checkers += classChecker
  }

  public fun build(): Pair<Moshi, Gson> {
    val interop = MoshiGsonInterop(moshi, gson, checkers + DefaultMoshiClassChecker)
    return interop.moshi to interop.gson
  }
}

public inline fun <reified T> InteropBuilder.addMoshiType(): InteropBuilder {
  return addMoshiType(T::class.java)
}

public inline fun <reified T> InteropBuilder.addGsonType(): InteropBuilder {
  return addGsonType(T::class.java)
}

private class MoshiGsonInterop(
  seedMoshi: Moshi,
  seedGson: Gson,
  checkers: List<MoshiClassChecker>,
) {

  val moshi: Moshi = seedMoshi.newBuilder()
    .add(MoshiGsonInteropJsonAdapterFactory(this, checkers))
    .build()

  val gson: Gson = seedGson.newBuilder()
    .registerTypeAdapterFactory(MoshiGsonInteropTypeAdapterFactory(this, checkers))
    .create()
}

/**
 * An interop-ing [JsonAdapter.Factory] that tries to intelligently defer to a `gson` instance for
 * appropriate types.
 */
private class MoshiGsonInteropJsonAdapterFactory(
  private val interop: MoshiGsonInterop,
  private val checkers: List<MoshiClassChecker>,
) : JsonAdapter.Factory {
  override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
    if (annotations.isNotEmpty() || type !is Class<*>) return null
    return if (checkers.any { it.shouldUseMoshi(type) }) {
      moshi.nextAdapter<Any>(this, type, annotations)
    } else {
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
  private val checkers: List<MoshiClassChecker>,
) : TypeAdapterFactory {
  override fun <T> create(gson: Gson, typeToken: TypeToken<T>): TypeAdapter<T>? {
    val type = typeToken.type
    if (type !is Class<*>) return null

    @Suppress("UNCHECKED_CAST")
    return if (checkers.any { it.shouldUseMoshi(type) }) {
      MoshiDelegatingTypeAdapter(interop.moshi.adapter<Any>(type)).nullSafe()
    } else {
      gson.getDelegateAdapter(this, typeToken)
    } as TypeAdapter<T>
  }
}

internal class MoshiDelegatingTypeAdapter<T>(
  private val delegate: JsonAdapter<T>,
) : TypeAdapter<T>() {
  override fun write(writer: com.google.gson.stream.JsonWriter, value: T?) {
    val serializedValue = delegate
      .run { if (writer.serializeNulls) serializeNulls() else this }
      .run { if (writer.isLenient) lenient() else this }
      .toJson(value)
    writer.jsonValue(serializedValue)
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