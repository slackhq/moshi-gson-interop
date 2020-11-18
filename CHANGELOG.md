Changelog
=========

0.4.0
-----

* **New:** `JsonAdapterAnnotationClassChecker` in `StandardClassCheckers` to watch for GSON's `@JsonAdapter` annotation.
* **Fix:** Support `JsonTreeWriter` in interop. Note that our implementation uses internal GSON APIs to work around this,
  which obviously isn't ideal. We've PR'd a fix for this upstream https://github.com/google/gson/pull/1819.

0.3.0
-----

_2020-11-12_

* **New:** Builder API for composing logic. This allows for easy customization of picking which
  classes should go to each serializer.

  ```kotlin
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
  ```

* **API Change:** `MoshiClassChecker` is now just `ClassChecker` and returns a nullable
  `Serializer`. `Serializer` can be `MOSHI`, `GSON`, or `null` to defer to another checker. The
  builder will try checkers until one claims the given class.
* **New:** Instead of indicating interop via `Pair<Moshi, Gson>`, these are now represented via
  `MoshiGsonInterop` data class.
* **New:** Optional logger API via the builder for easy printing of the internal decision tree.
* **New:** Default serializer option via the builder.
* **New:** Public APIs for standard `ClassChecker`s.

0.2.0
-----

_2020-10-22_

Add support for `lenient` and `serializeNulls` to match behavior between adapters.

Thanks to [@romankivalin](https://github.com/romankivalin) for contributing to this release.

0.1.0
-----

_2020-10-05_

Initial release!
