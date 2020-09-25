package com.slack.moshi.interop.gson

import com.google.common.truth.Truth.assertThat
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.internal.NullSafeJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
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
        ]
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
    val gsonClassAdapter = moshi.adapter(SimpleGsonClass::class.java)
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
    val adapter = moshi.adapter(MoshiClass::class.java)
    val instance = adapter.fromJson(integrationJson)!!
    val expected = MoshiClass(
      SimpleMoshiClass("moshi!"),
      SimpleGsonClass("gson!"),
      RegularEnum.TYPE,
      MoshiEnum.TYPE,
      GsonEnum.TYPE,
      listOf(SimpleMoshiClass("moshi!")),
      listOf(SimpleGsonClass("gson!"))
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
      listOf(SimpleGsonClass("gson!"))
    )
    assertThat(instance).isEqualTo(expected)
    val serialized = adapter.toJson(instance)
    val secondInstance = adapter.fromJson(serialized)
    assertThat(secondInstance).isEqualTo(expected)
  }

  @Test
  fun complexMoshi() {
    val adapter = moshi.adapter(Complex::class.java)
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

  // TODO
  //  collections of enums
  //  generic types
}

@JsonClass(generateAdapter = true)
data class MoshiClass(
  val anotherMoshiClass: SimpleMoshiClass,
  val simpleGsonClass: SimpleGsonClass,
  val regularEnum: RegularEnum,
  val moshiEnum: MoshiEnum,
  val gsonEnum: GsonEnum,
  val moreMoshiClasses: List<SimpleMoshiClass>,
  val moreSimpleGsonClasses: List<SimpleGsonClass>
)

data class GsonClass(
  val anotherMoshiClass: SimpleMoshiClass,
  val simpleGsonClass: SimpleGsonClass,
  val regularEnum: RegularEnum,
  val moshiEnum: MoshiEnum,
  val gsonEnum: GsonEnum,
  val moreMoshiClasses: List<SimpleMoshiClass>,
  val moreSimpleGsonClasses: List<SimpleGsonClass>
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