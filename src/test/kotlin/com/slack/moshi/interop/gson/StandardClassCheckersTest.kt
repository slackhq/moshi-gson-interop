package com.slack.moshi.interop.gson

import com.google.common.truth.Truth.assertThat
import com.google.gson.annotations.JsonAdapter
import com.squareup.moshi.JsonClass
import org.junit.Test

class StandardClassCheckersTest {
  @Test
  fun jsonAdapter() {
    assertThat(JsonAdapterAnnotationChecker.serializerFor(JsonAdapterClass::class.java))
      .isEqualTo(Serializer.GSON)
    assertThat(JsonAdapterAnnotationChecker.serializerFor(UnAnnotatedClass::class.java))
      .isNull()
    assertThat(JsonAdapterAnnotationChecker.serializerFor(JsonClassClass::class.java))
      .isNull()
  }

  @Test
  fun jsonClass() {
    assertThat(JsonClassClassChecker.serializerFor(JsonClassClass::class.java))
      .isEqualTo(Serializer.MOSHI)
    assertThat(JsonClassClassChecker.serializerFor(UnAnnotatedClass::class.java))
      .isNull()
    assertThat(JsonClassClassChecker.serializerFor(JsonAdapterClass::class.java))
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