/*
 * Copyright (C) 2020 Slack Technologies, LLC
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

import com.google.common.truth.Truth.assertThat
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.internal.bind.JsonTreeWriter
import com.google.gson.reflect.TypeToken
import com.slack.moshi.interop.gson.Serializer.GSON
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.internal.NonNullJsonAdapter
import com.squareup.moshi.internal.NullSafeJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.fail
import org.junit.Test

class MoshiGsonInteropTest {
  private val interop =
    Moshi.Builder()
      .addLast(KotlinJsonAdapterFactory())
      .build()
      .interopBuilder(GsonBuilder().create())
      .logger(::println)
      .build()
  private val moshi = interop.moshi
  private val gson = interop.gson

  // language=JSON
  private val integrationJson =
    """
      {
        "anotherMoshiClass": {
          "value": "moshi!"
        },
        "simpleGsonClass": {
          "value": "gson!"
        },
        "regularEnum": "TYPE",
        "moshiEnum": "_type",
        "gsonEnum": "__type",
        "moreMoshiClasses": [
          {
            "value": "moshi!"
          }
        ],
        "moreSimpleGsonClasses": [
          {
            "value": "gson!"
          }
        ],
        "genericMoshiClass": {
          "value": "_type"
        },
        "genericGsonClass": {
          "value": "__type"
        },
        "mixedGenericGsonClass": {
          "value": {
            "value": "__type"
          }
        },
        "mixedGenericMoshiClass": {
          "value": {
            "value": "_type"
          }
        }
      }
    """
      .trimIndent()

  // language=JSON
  private val complexJson =
    """
    {
      "value": {
        "value": {
          "value": {
            "value": "value!"
          }
        }
      }
    }
    """
      .trimIndent()

  private val complexInstance =
    Complex(ComplexNestedOne(ComplexNestedTwo(ComplexNestedThree("value!"))))

  @Test
  fun simpleMoshiDelegation() {
    val gsonClassAdapter = moshi.adapter<SimpleGsonClass>()
    check(gsonClassAdapter is NullSafeJsonAdapter)
    val delegate = gsonClassAdapter.delegate()
    assertThat(delegate).isInstanceOf(GsonDelegatingJsonAdapter::class.java)
  }

  @Test
  fun simpleGsonDelegation() {
    // Can't check if it's a nullsafe adapter like moshi, so we just run it
    val adapter = gson.getAdapter(SimpleMoshiClass::class.java)

    // language=JSON
    val json =
      """
      {
        "value": "moshi!"
      }
      """
        .trimIndent()

    val instance = adapter.fromJson(json)
    assertThat(instance).isEqualTo(SimpleMoshiClass("moshi!"))
  }

  @Test
  fun integrationMoshi() {
    val adapter = moshi.adapter<MoshiClass>()
    val instance = adapter.fromJson(integrationJson)!!
    val expected =
      MoshiClass(
        SimpleMoshiClass("moshi!"),
        SimpleGsonClass("gson!"),
        RegularEnum.TYPE,
        MoshiEnum.TYPE,
        GsonEnum.TYPE,
        listOf(SimpleMoshiClass("moshi!")),
        listOf(SimpleGsonClass("gson!")),
        SimpleGenericMoshiClass(MoshiEnum.TYPE),
        SimpleGenericGsonClass(GsonEnum.TYPE),
        SimpleMixedGenericGsonClass(SimpleGenericMoshiClass(GsonEnum.TYPE)),
        SimpleMixedGenericMoshiClass(SimpleGenericGsonClass(MoshiEnum.TYPE))
      )
    assertThat(instance).isEqualTo(expected)
    val serialized = adapter.toJson(instance)
    val secondInstance = adapter.fromJson(serialized)
    assertThat(secondInstance).isEqualTo(expected)
  }

  @Test
  fun integrationGson() {
    val adapter = gson.getAdapter(GsonClass::class.java)
    val instance = adapter.fromJson(integrationJson)!!
    val expected =
      GsonClass(
        SimpleMoshiClass("moshi!"),
        SimpleGsonClass("gson!"),
        RegularEnum.TYPE,
        MoshiEnum.TYPE,
        GsonEnum.TYPE,
        listOf(SimpleMoshiClass("moshi!")),
        listOf(SimpleGsonClass("gson!")),
        SimpleGenericMoshiClass(MoshiEnum.TYPE),
        SimpleGenericGsonClass(GsonEnum.TYPE),
        SimpleMixedGenericGsonClass(SimpleGenericMoshiClass(GsonEnum.TYPE)),
        SimpleMixedGenericMoshiClass(SimpleGenericGsonClass(MoshiEnum.TYPE))
      )
    assertThat(instance).isEqualTo(expected)
    val serialized = adapter.toJson(instance)
    val secondInstance = adapter.fromJson(serialized)
    assertThat(secondInstance).isEqualTo(expected)
  }

  @Test
  fun complexMoshi() {
    val adapter = moshi.adapter<Complex>()
    val instance = adapter.fromJson(complexJson)!!
    assertThat(instance).isEqualTo(complexInstance)
    val serialized = adapter.toJson(instance)
    val secondInstance = adapter.fromJson(serialized)
    assertThat(secondInstance).isEqualTo(complexInstance)
  }

  @Test
  fun complexGson() {
    val adapter = gson.getAdapter(Complex::class.java)
    val instance = adapter.fromJson(complexJson)!!
    assertThat(instance).isEqualTo(complexInstance)
    val serialized = adapter.toJson(instance)
    val secondInstance = adapter.fromJson(serialized)
    assertThat(secondInstance).isEqualTo(complexInstance)
  }

  @Test
  fun moshiEnumCollections() {
    val adapter = moshi.adapter<List<MoshiEnum>>()
    val expected = listOf(MoshiEnum.TYPE)
    val json = "[\"_type\"]"
    val instance = adapter.fromJson(json)
    assertThat(instance).isEqualTo(expected)
    val serialized = adapter.toJson(expected)
    assertThat(json == serialized)
  }

  @Test
  fun gsonEnumCollections() {
    @Suppress("UNCHECKED_CAST")
    val adapter =
      gson.getAdapter(TypeToken.getParameterized(List::class.java, GsonEnum::class.java))
        as TypeAdapter<List<GsonEnum>>
    val expected = listOf(GsonEnum.TYPE)
    val json = "[\"__type\"]"
    val instance = adapter.fromJson(json)
    assertThat(instance).isEqualTo(expected)
    val serialized = adapter.toJson(expected)
    assertThat(json == serialized)
  }

  @Test
  fun customClassChecker() {
    // An interop Moshi instance set to _always_ claim classes
    val (moshi, _) =
      Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
        .interopBuilder(GsonBuilder().create())
        .addClassChecker { Serializer.MOSHI }
        .build()

    val gsonClassAdapter = moshi.adapter<SimpleGsonClass>()
    check(gsonClassAdapter is NullSafeJsonAdapter)
    val delegate = gsonClassAdapter.delegate()
    // We set it to _always_ use Moshi, so we should never delegate here
    check(delegate !is GsonDelegatingJsonAdapter)
    // A little ugly to rely on toString, but just to confirm we're using the KotlinJsonAdapter
    // version
    assertThat(delegate.toString())
      .isEqualTo("KotlinJsonAdapter(${SimpleGsonClass::class.java.canonicalName})")
  }

  @Test
  fun serializeNulls() {
    val withoutNulls = moshi.adapter(GsonNumber::class.java).toJson(GsonNumber(value = null))
    assertThat(withoutNulls).isEqualTo("{}")
    val withNulls =
      moshi.adapter(GsonNumber::class.java).serializeNulls().toJson(GsonNumber(value = null))
    assertThat(withNulls).isEqualTo("""{"value":null}""")
  }

  @Test
  fun lenientRead() {
    try {
      moshi.adapter(GsonNumber::class.java).fromJson("""{"value":NaN}""")
      fail()
    } catch (err: Exception) {
      assertThat(err.message).startsWith("Use JsonReader.setLenient(true) to accept malformed JSON")
    }
    val withNaN = moshi.adapter(GsonNumber::class.java).lenient().fromJson("""{"value":NaN}""")
    assertThat(withNaN).isEqualTo(GsonNumber(value = Double.NaN))
  }

  @Test
  fun lenientWrite() {
    try {
      moshi.adapter(GsonNumber::class.java).toJson(GsonNumber(value = Double.NaN))
      fail()
    } catch (err: Exception) {
      assertThat(err.message).startsWith("Numeric values must be finite")
    }
    val withNaN =
      moshi.adapter(GsonNumber::class.java).lenient().toJson(GsonNumber(value = Double.NaN))
    assertThat(withNaN).isEqualTo("""{"value":NaN}""")
  }

  @Test
  fun jsonTreeWriter_simple() {
    val writer = JsonTreeWriter()
    val moshiDelegate = moshi.adapter<Any>()
    val typeAdapter = MoshiDelegatingTypeAdapter(moshiDelegate)
    typeAdapter.write(writer, "yay")
    assertThat(writer.get().toString()).isEqualTo("\"yay\"")
  }

  @Test
  fun jsonTreeWriter_large() {
    val writer = JsonTreeWriter()
    val moshiDelegate = moshi.adapter<MoshiClass>()
    val typeAdapter = MoshiDelegatingTypeAdapter(moshiDelegate)
    val instance =
      MoshiClass(
        SimpleMoshiClass("moshi!"),
        SimpleGsonClass("gson!"),
        RegularEnum.TYPE,
        MoshiEnum.TYPE,
        GsonEnum.TYPE,
        listOf(SimpleMoshiClass("moshi!")),
        listOf(SimpleGsonClass("gson!")),
        SimpleGenericMoshiClass(MoshiEnum.TYPE),
        SimpleGenericGsonClass(GsonEnum.TYPE),
        SimpleMixedGenericGsonClass(SimpleGenericMoshiClass(GsonEnum.TYPE)),
        SimpleMixedGenericMoshiClass(SimpleGenericGsonClass(MoshiEnum.TYPE))
      )
    typeAdapter.write(writer, instance)
    val serialized = writer.get().toString()

    // Now read the serialized one with a real instance and ensure they're equal
    val secondInstance = moshi.adapter<MoshiClass>().fromJson(serialized)
    assertThat(secondInstance).isEqualTo(instance)
  }

  @Test
  fun shortCircuitTest() {
    // This test ensures that even if a standard class checker at the bottom can claim a type, a
    // registered one before it will take precedence.
    val preferGsonEnums = EnumClassChecker(GSON)
    val interop =
      Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
        .interopBuilder(GsonBuilder().create())
        .addClassChecker(preferGsonEnums)
        .addGsonType<Int>()
        .logger(::println)
        .build()
    val moshi = interop.moshi

    // Even though moshi can handle this by default, we short-circuited it to choose gson anyway
    moshi.adapter<SimpleEnum>().assertIsJsonAdapterInstanceOf<GsonDelegatingJsonAdapter<*>>()
    moshi.adapter<Int>().assertIsJsonAdapterInstanceOf<GsonDelegatingJsonAdapter<*>>()
  }

  // A simple enum that both moshi and gson could handle
  enum class SimpleEnum {
    INSTANCE
  }

  private inline fun <reified T : JsonAdapter<*>> JsonAdapter<*>.assertIsJsonAdapterInstanceOf() {
    val adapter =
      when (this) {
        is NonNullJsonAdapter<*> -> delegate()
        is NullSafeJsonAdapter<*> -> delegate()
        else -> this
      }
    assertThat(adapter).isInstanceOf(T::class.java)
  }
}

@JsonClass(generateAdapter = true)
data class MoshiClass(
  val anotherMoshiClass: SimpleMoshiClass,
  val simpleGsonClass: SimpleGsonClass,
  val regularEnum: RegularEnum,
  val moshiEnum: MoshiEnum,
  val gsonEnum: GsonEnum,
  val moreMoshiClasses: List<SimpleMoshiClass>,
  val moreSimpleGsonClasses: List<SimpleGsonClass>,
  val genericMoshiClass: SimpleGenericMoshiClass<MoshiEnum>,
  val genericGsonClass: SimpleGenericGsonClass<GsonEnum>,
  val mixedGenericGsonClass: SimpleMixedGenericGsonClass<SimpleGenericMoshiClass<GsonEnum>>,
  val mixedGenericMoshiClass: SimpleMixedGenericMoshiClass<SimpleGenericGsonClass<MoshiEnum>>,
)

data class GsonClass(
  val anotherMoshiClass: SimpleMoshiClass,
  val simpleGsonClass: SimpleGsonClass,
  val regularEnum: RegularEnum,
  val moshiEnum: MoshiEnum,
  val gsonEnum: GsonEnum,
  val moreMoshiClasses: List<SimpleMoshiClass>,
  val moreSimpleGsonClasses: List<SimpleGsonClass>,
  val genericMoshiClass: SimpleGenericMoshiClass<MoshiEnum>,
  val genericGsonClass: SimpleGenericGsonClass<GsonEnum>,
  val mixedGenericGsonClass: SimpleMixedGenericGsonClass<SimpleGenericMoshiClass<GsonEnum>>,
  val mixedGenericMoshiClass: SimpleMixedGenericMoshiClass<SimpleGenericGsonClass<MoshiEnum>>,
)

data class Complex(val value: ComplexNestedOne)

@JsonClass(generateAdapter = true) data class ComplexNestedOne(val value: ComplexNestedTwo)

data class ComplexNestedTwo(val value: ComplexNestedThree)

@JsonClass(generateAdapter = true) data class ComplexNestedThree(val value: String)

@JsonClass(generateAdapter = true) data class SimpleMoshiClass(val value: String)

data class SimpleGsonClass(val value: String)

@JsonClass(generateAdapter = true) data class SimpleGenericMoshiClass<T>(val value: T)

data class SimpleGenericGsonClass<T>(val value: T)

@JsonClass(generateAdapter = true) data class SimpleMixedGenericMoshiClass<T>(val value: T)

data class SimpleMixedGenericGsonClass<T>(val value: T)

enum class RegularEnum {
  TYPE
}

enum class MoshiEnum {
  @Json(name = "_type") TYPE
}

enum class GsonEnum {
  @SerializedName("__type") TYPE
}

data class GsonNumber(val value: Double? = null)
