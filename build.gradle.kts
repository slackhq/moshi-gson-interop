/*
 * Copyright (C) 2020 Slack Technologies, LLC
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
import com.diffplug.spotless.LineEnding
import io.gitlab.arturbosch.detekt.Detekt
import java.net.URL
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.9.21"
  id("org.jetbrains.dokka") version "1.9.10"
  id("com.diffplug.spotless") version "6.23.3"
  id("com.vanniktech.maven.publish") version "0.25.3"
  id("io.gitlab.arturbosch.detekt") version "1.23.5"
}

repositories { mavenCentral() }

java {
  toolchain { languageVersion.set(JavaLanguageVersion.of(11)) }
  tasks.withType<JavaCompile>().configureEach { options.release.set(8) }
}

tasks.withType<KotlinCompile>().configureEach {
  val isTest = name == "compileTestKotlin"
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_1_8)
    freeCompilerArgs.add("-progressive")
    if (isTest) {
      freeCompilerArgs.add("-opt-in=kotlin.ExperimentalStdlibApi")
    }
  }
}

tasks.withType<Detekt>().configureEach { jvmTarget = "1.8" }

tasks.named<DokkaTask>("dokkaHtml") {
  outputDirectory.set(rootDir.resolve("docs/0.x"))
  dokkaSourceSets.configureEach {
    skipDeprecated.set(true)
    externalDocumentationLink { url.set(URL("https://square.github.io/moshi/1.x/moshi/")) }
    // No GSON doc because they host on javadoc.io, which Dokka can't parse.
  }
}

kotlin { explicitApi() }

spotless {
  lineEndings = LineEnding.PLATFORM_NATIVE
  format("misc") {
    target("*.md", ".gitignore")
    trimTrailingWhitespace()
    endWithNewline()
  }
  val ktfmtVersion = "0.43"
  kotlin {
    target("**/*.kt")
    ktfmt(ktfmtVersion).googleStyle()
    trimTrailingWhitespace()
    endWithNewline()
    licenseHeaderFile("spotless/spotless.kt")
    targetExclude("**/spotless.kt")
  }
  kotlinGradle {
    ktfmt(ktfmtVersion).googleStyle()
    trimTrailingWhitespace()
    endWithNewline()
    licenseHeaderFile(
      "spotless/spotless.kt",
      "(import|plugins|buildscript|dependencies|pluginManagement)"
    )
  }
}

val moshiVersion = "1.15.1"

dependencies {
  implementation("com.google.code.gson:gson:2.10.1")
  implementation("com.squareup.moshi:moshi:$moshiVersion")

  testImplementation("com.squareup.moshi:moshi-kotlin:$moshiVersion")
  testImplementation("junit:junit:4.13.2")
  testImplementation("com.google.truth:truth:1.4.2")
}
