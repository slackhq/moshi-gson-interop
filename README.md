# moshi-gson-interop

A tool for bidirectional interop between Moshi and Gson.

This is targeted at codebases that are in the process of migrating from Gson _to_ Moshi and not a
general-purpose tool that should be used for other reasons.

## Usage

In order to properly link two `Moshi` and `Gson` instances, you must pass two _complete_
instances (i.e. not intended to `newBuilder()` later) to `Moshi.interopWith()` and use
the returned instances.

```kotlin
val seedMoshi: Moshi = ...
val seedGson: Gson = ...

// Use the returned instances!
val (moshi, gson) = seedMoshi.interopWith(seedGson)
```

By default, the interop mechanism will attempt to best-guess which serializer to use based on a combination
of things (see `DefaultMoshiClassChecker`'s kdoc). You can alternatively provide your own logic for this.

## Installation

NOTE: Until Moshi 1.11.0 is released, we will only have snapshots available. See the next `Caveats` section for more details.

```gradle
dependencies {
  implementation("com.slack.moshi:gson-interop:<version>")
}
```

Snapshots of the development version are available in [Sonatype's `snapshots` repository][snap].

## Caveats

* This uses an unreleased `nextSource()` API in Moshi, so it currently depends on 1.11.0-SNAPSHOT.
  We will update to the stable version when it's released.
* Performance
  * A Moshi adapter delegating to Gson should have no performance issues as it is able
    to stream data directly.
  * Streaming from Gson to Moshi will, however, be degraded as Gson has no equivalent
    streaming APIs to Moshi's `nextSource()` or `valueSink()` APIs. This means that it
    must either decode the entire source into an intermediary blob (i.e. a Map, List,
    etc) first when reading or encode the entire value into a json String first when writing.


License
--------

    Copyright 2020 Slack Technologies, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


[snap]: https://oss.sonatype.org/content/repositories/snapshots/com/slack/moshi/
