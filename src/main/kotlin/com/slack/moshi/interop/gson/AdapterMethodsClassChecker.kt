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

import com.slack.moshi.interop.gson.Serializer.MOSHI
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson

/** Checks if a class has any [FromJson] or [ToJson] annotated methods and should be used with Moshi. */
public object AdapterMethodsClassChecker : ClassChecker {
  override fun serializerFor(rawType: Class<*>): Serializer? {
    var clazz: Class<*>? = rawType
    while (clazz != Any::class.java && clazz != null) {
      for (method in clazz.declaredMethods) {
        if (method.isAnnotationPresent(ToJson::class.java) ||
          method.isAnnotationPresent(FromJson::class.java)) {
          return MOSHI
        }
      }
      clazz = clazz.superclass
    }
    return null
  }

  override fun toString(): String {
    return "AdapterMethodsClassChecker"
  }
}
