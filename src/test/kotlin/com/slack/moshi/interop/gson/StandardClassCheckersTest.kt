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
import com.google.gson.annotations.JsonAdapter
import com.squareup.moshi.JsonClass
import org.junit.Test

class StandardClassCheckersTest {
  @Test
  fun jsonAdapter() {
    assertThat(JsonAdapterAnnotationClassChecker.serializerFor(JsonAdapterClass::class.java))
      .isEqualTo(Serializer.GSON)
    assertThat(JsonAdapterAnnotationClassChecker.serializerFor(UnAnnotatedClass::class.java))
      .isNull()
    assertThat(JsonAdapterAnnotationClassChecker.serializerFor(JsonClassClass::class.java))
      .isNull()
  }

  @Test
  fun jsonClass() {
    assertThat(JsonClassAnnotationClassChecker.serializerFor(JsonClassClass::class.java))
      .isEqualTo(Serializer.MOSHI)
    assertThat(JsonClassAnnotationClassChecker.serializerFor(UnAnnotatedClass::class.java))
      .isNull()
    assertThat(JsonClassAnnotationClassChecker.serializerFor(JsonAdapterClass::class.java))
      .isNull()
  }

  @Test
  fun primitives() {
    assertThat(BuiltInsClassChecker.serializerFor(Boolean::class.javaPrimitiveType!!)).isEqualTo(Serializer.MOSHI)
    assertThat(BuiltInsClassChecker.serializerFor(Boolean::class.javaObjectType)).isEqualTo(Serializer.MOSHI)
    assertThat(BuiltInsClassChecker.serializerFor(Byte::class.javaPrimitiveType!!)).isEqualTo(Serializer.MOSHI)
    assertThat(BuiltInsClassChecker.serializerFor(Byte::class.javaObjectType)).isEqualTo(Serializer.MOSHI)
    assertThat(BuiltInsClassChecker.serializerFor(Char::class.javaPrimitiveType!!)).isEqualTo(Serializer.MOSHI)
    assertThat(BuiltInsClassChecker.serializerFor(Character::class.javaObjectType)).isEqualTo(Serializer.MOSHI)
    assertThat(BuiltInsClassChecker.serializerFor(Double::class.javaPrimitiveType!!)).isEqualTo(Serializer.MOSHI)
    assertThat(BuiltInsClassChecker.serializerFor(Double::class.javaObjectType)).isEqualTo(Serializer.MOSHI)
    assertThat(BuiltInsClassChecker.serializerFor(Float::class.javaPrimitiveType!!)).isEqualTo(Serializer.MOSHI)
    assertThat(BuiltInsClassChecker.serializerFor(Float::class.javaObjectType)).isEqualTo(Serializer.MOSHI)
    assertThat(BuiltInsClassChecker.serializerFor(Int::class.javaPrimitiveType!!)).isEqualTo(Serializer.MOSHI)
    assertThat(BuiltInsClassChecker.serializerFor(Integer::class.javaObjectType)).isEqualTo(Serializer.MOSHI)
    assertThat(BuiltInsClassChecker.serializerFor(Long::class.javaPrimitiveType!!)).isEqualTo(Serializer.MOSHI)
    assertThat(BuiltInsClassChecker.serializerFor(Long::class.javaObjectType)).isEqualTo(Serializer.MOSHI)
    assertThat(BuiltInsClassChecker.serializerFor(Short::class.javaPrimitiveType!!)).isEqualTo(Serializer.MOSHI)
    assertThat(BuiltInsClassChecker.serializerFor(Short::class.javaObjectType)).isEqualTo(Serializer.MOSHI)
    assertThat(BuiltInsClassChecker.serializerFor(Void::class.javaPrimitiveType!!)).isEqualTo(Serializer.MOSHI)
    assertThat(BuiltInsClassChecker.serializerFor(Void::class.javaObjectType)).isEqualTo(Serializer.MOSHI)
    assertThat(BuiltInsClassChecker.serializerFor(String::class.java)).isEqualTo(Serializer.MOSHI)
    assertThat(BuiltInsClassChecker.serializerFor(Any::class.java)).isEqualTo(Serializer.MOSHI)
    assertThat(BuiltInsClassChecker.serializerFor(UnAnnotatedClass::class.java)).isNull()
  }

  @JsonAdapter(Nothing::class)
  class JsonAdapterClass

  @JsonClass(generateAdapter = false)
  class JsonClassClass

  class UnAnnotatedClass
}
