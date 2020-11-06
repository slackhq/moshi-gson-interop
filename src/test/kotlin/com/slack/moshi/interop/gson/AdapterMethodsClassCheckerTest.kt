package com.slack.moshi.interop.gson

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import org.junit.Test

class AdapterMethodsClassCheckerTest {
  @Test
  fun simple() {
    assertThat(AdapterMethodsClassChecker.serializerFor(ThingWithAdapterMethods::class.java))
      .isEqualTo(Serializer.MOSHI)
  }

  @Test
  fun justFromJson() {
    assertThat(AdapterMethodsClassChecker.serializerFor(ThingWithJsonFromJson::class.java))
      .isEqualTo(Serializer.MOSHI)
  }

  @Test
  fun justToJson() {
    assertThat(AdapterMethodsClassChecker.serializerFor(ThingWithJsonToJson::class.java))
      .isEqualTo(Serializer.MOSHI)
  }

  @Test
  fun subclass() {
    assertThat(AdapterMethodsClassChecker.serializerFor(SubclassWithoutFunctions::class.java))
      .isEqualTo(Serializer.MOSHI)
  }

  @Test
  fun ignoreOtherStuff() {
    assertThat(AdapterMethodsClassChecker.serializerFor(String::class.java))
      .isNull()
  }
}

class ThingWithAdapterMethods {
  @FromJson
  fun fromJson() {

  }

  @ToJson
  fun toJson() {

  }
}

class ThingWithJsonFromJson {
  @FromJson
  fun fromJson() {

  }
}

class ThingWithJsonToJson {
  @ToJson
  fun toJson() {

  }
}

abstract class BaseClassWithFunctions {
  @FromJson
  fun fromJson() {

  }

  @ToJson
  fun toJson() {

  }
}

class SubclassWithoutFunctions : BaseClassWithFunctions()