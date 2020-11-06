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
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.slack.moshi.interop.gson.Serializer.GSON
import com.slack.moshi.interop.gson.Serializer.MOSHI
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.adapter
import org.junit.Test

class InteropBuilderTest {
  @Test
  fun factories() {
    val builder = Moshi.Builder().build()
      .interopBuilder(GsonBuilder().create())

    builder.addGsonType<String>()
    builder.addMoshiType<Int>()
    builder.addGsonType<List<String>>() // Interesting because only the List is captured
    builder.addGsonFactory(object : TypeAdapterFactory {
      override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        return if (type.rawType == Boolean::class.javaObjectType) {
          @Suppress("UNCHECKED_CAST")
          gson.getAdapter(Boolean::class.java) as TypeAdapter<T>
        } else {
          null
        }
      }
    })
    builder.addMoshiFactory { type, _, moshi ->
      if (Types.getRawType(type) == Double::class.javaObjectType) {
        moshi.adapter<Double>()
      } else {
        null
      }
    }

    val checker = CompositeChecker(builder.checkers)
    assertThat(checker.serializerFor(String::class.java)).isEqualTo(GSON)
    assertThat(checker.serializerFor(Int::class.javaObjectType)).isEqualTo(MOSHI)
    assertThat(checker.serializerFor(List::class.java)).isEqualTo(GSON)
    assertThat(checker.serializerFor(Boolean::class.javaObjectType)).isEqualTo(GSON)
    assertThat(checker.serializerFor(Double::class.javaObjectType)).isEqualTo(MOSHI)
  }

  class CompositeChecker(private val checkers: List<ClassChecker>) : ClassChecker {
    override fun serializerFor(rawType: Class<*>): Serializer? {
      return checkers
        .mapNotNull { checker ->
          checker.serializerFor(rawType)
        }
        .firstOrNull()
    }
  }
}
