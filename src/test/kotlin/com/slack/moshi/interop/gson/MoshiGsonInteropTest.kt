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

import com.google.common.truth.Truth.assertThat
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.internal.NullSafeJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.zacsweers.moshix.adapter
import org.junit.Test

class MoshiGsonInteropTest {
  private val interop = wireMoshiGsonInterop(
    seedMoshi = Moshi.Builder().build(),
    seedGson = GsonBuilder().create()
  ) {
    it.add(KotlinJsonAdapterFactory())
  }
  private val moshi = interop.first
  private val gson = interop.second

  //language=JSON
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
          "value": "value!"
        },
        "genericGsonClass": {
          "value": "value!"
        },
        "mixedGenericGsonClass": {
          "value": {
            "value": "value!"
          }
        },
        "mixedGenericMoshiClass": {
          "value": {
            "value": "value!"
          }
        }
      }
    """.trimIndent()

  //language=JSON
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
    """.trimIndent()

  private val complexInstance = Complex(ComplexNestedOne(ComplexNestedTwo(ComplexNestedThree("value!"))))

  @Test
  fun simpleMoshiDelegation() {
    val gsonClassAdapter = moshi.adapter<SimpleGsonClass>()
    check(gsonClassAdapter is NullSafeJsonAdapter)
    val delegate = gsonClassAdapter.delegate()
    check(delegate is GsonDelegatingJsonAdapter)
  }

  @Test
  fun simpleGsonDelegation() {
    // Can't check if it's a nullsafe adapter like moshi, so we just run it
    val adapter = gson.getAdapter(SimpleMoshiClass::class.java)

    //language=JSON
    val json =
      """
      {
        "value": "moshi!"
      }
      """.trimIndent()

    val instance = adapter.fromJson(json)
    assertThat(instance).isEqualTo(SimpleMoshiClass("moshi!"))
  }

  @Test
  fun integrationMoshi() {
    val adapter = moshi.adapter<MoshiClass>()
    val instance = adapter.fromJson(integrationJson)!!
    val expected = MoshiClass(
      SimpleMoshiClass("moshi!"),
      SimpleGsonClass("gson!"),
      RegularEnum.TYPE,
      MoshiEnum.TYPE,
      GsonEnum.TYPE,
      listOf(SimpleMoshiClass("moshi!")),
      listOf(SimpleGsonClass("gson!")),
      SimpleGenericMoshiClass("value!"),
      SimpleGenericGsonClass("value!"),
      SimpleMixedGenericGsonClass(SimpleGenericMoshiClass("value!")),
      SimpleMixedGenericMoshiClass(SimpleGenericGsonClass("value!"))
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
    val expected = GsonClass(
      SimpleMoshiClass("moshi!"),
      SimpleGsonClass("gson!"),
      RegularEnum.TYPE,
      MoshiEnum.TYPE,
      GsonEnum.TYPE,
      listOf(SimpleMoshiClass("moshi!")),
      listOf(SimpleGsonClass("gson!")),
      SimpleGenericMoshiClass("value!"),
      SimpleGenericGsonClass("value!"),
      SimpleMixedGenericGsonClass(SimpleGenericMoshiClass("value!")),
      SimpleMixedGenericMoshiClass(SimpleGenericGsonClass("value!"))
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
    val adapter = gson.getAdapter(TypeToken.getParameterized(List::class.java, GsonEnum::class.java)) as TypeAdapter<List<GsonEnum>>
    val expected = listOf(GsonEnum.TYPE)
    val json = "[\"__type\"]"
    val instance = adapter.fromJson(json)
    assertThat(instance).isEqualTo(expected)
    val serialized = adapter.toJson(expected)
    assertThat(json == serialized)
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
  val genericMoshiClass: SimpleGenericMoshiClass<String>,
  val genericGsonClass: SimpleGenericGsonClass<String>,
  val mixedGenericGsonClass: SimpleMixedGenericGsonClass<SimpleGenericMoshiClass<String>>,
  val mixedGenericMoshiClass: SimpleMixedGenericMoshiClass<SimpleGenericGsonClass<String>>
)

data class GsonClass(
  val anotherMoshiClass: SimpleMoshiClass,
  val simpleGsonClass: SimpleGsonClass,
  val regularEnum: RegularEnum,
  val moshiEnum: MoshiEnum,
  val gsonEnum: GsonEnum,
  val moreMoshiClasses: List<SimpleMoshiClass>,
  val moreSimpleGsonClasses: List<SimpleGsonClass>,
  val genericMoshiClass: SimpleGenericMoshiClass<String>,
  val genericGsonClass: SimpleGenericGsonClass<String>,
  val mixedGenericGsonClass: SimpleMixedGenericGsonClass<SimpleGenericMoshiClass<String>>,
  val mixedGenericMoshiClass: SimpleMixedGenericMoshiClass<SimpleGenericGsonClass<String>>
)

data class Complex(val value: ComplexNestedOne)

@JsonClass(generateAdapter = true)
data class ComplexNestedOne(val value: ComplexNestedTwo)

data class ComplexNestedTwo(val value: ComplexNestedThree)

@JsonClass(generateAdapter = true)
data class ComplexNestedThree(val value: String)

@JsonClass(generateAdapter = true)
data class SimpleMoshiClass(val value: String)

data class SimpleGsonClass(val value: String)

@JsonClass(generateAdapter = true)
data class SimpleGenericMoshiClass<T>(val value: T)

data class SimpleGenericGsonClass<T>(val value: T)

@JsonClass(generateAdapter = true)
data class SimpleMixedGenericMoshiClass<T>(val value: T)

data class SimpleMixedGenericGsonClass<T>(val value: T)

enum class RegularEnum {
  TYPE
}

enum class MoshiEnum {
  @Json(name = "_type")
  TYPE
}

enum class GsonEnum {
  @SerializedName("__type")
  TYPE
}
