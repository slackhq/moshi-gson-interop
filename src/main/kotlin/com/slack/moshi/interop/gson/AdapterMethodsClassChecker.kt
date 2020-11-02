package com.slack.moshi.interop.gson

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson

/** Checks if a class has any [FromJson] or [ToJson] annotated methods and should be used with Moshi. */
public object AdapterMethodsClassChecker : MoshiClassChecker {
  override fun shouldUseMoshi(rawType: Class<*>): Boolean {
    var clazz: Class<*>? = rawType
    while (clazz != Any::class.java && clazz != null) {
      for (method in clazz.declaredMethods) {
        if (method.isAnnotationPresent(ToJson::class.java)) {
          return true
        }
        if (method.isAnnotationPresent(FromJson::class.java)) {
          return true
        }
      }
      clazz = clazz.superclass
    }
    return false
  }
}