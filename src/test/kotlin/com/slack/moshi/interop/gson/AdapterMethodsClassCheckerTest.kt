package com.slack.moshi.interop.gson

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import org.junit.Test

class AdapterMethodsClassCheckerTest {
  @Test
  fun simple() {
    assertThat(AdapterMethodsClassChecker.shouldUseMoshi(ThingWithAdapterMethods::class.java))
      .isTrue()
  }

  @Test
  fun justFromJson() {
    assertThat(AdapterMethodsClassChecker.shouldUseMoshi(ThingWithJsonFromJson::class.java))
      .isTrue()
  }

  @Test
  fun justToJson() {
    assertThat(AdapterMethodsClassChecker.shouldUseMoshi(ThingWithJsonToJson::class.java))
      .isTrue()
  }

  @Test
  fun subclass() {
    assertThat(AdapterMethodsClassChecker.shouldUseMoshi(SubclassWithoutFunctions::class.java))
      .isTrue()
  }

  @Test
  fun ignoreOtherStuff() {
    assertThat(AdapterMethodsClassChecker.shouldUseMoshi(String::class.java))
      .isFalse()
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