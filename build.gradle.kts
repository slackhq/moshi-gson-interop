plugins {
  kotlin("jvm") version "1.4.10"
  kotlin("kapt") version "1.4.10"
}

repositories {
  mavenCentral()
  // Until Moshi 1.11.0 is released
  maven("https://oss.sonatype.org/content/repositories/snapshots")
}

// TODO spotless, uploading snapshots, target moshi snapshots

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  kotlinOptions {
    jvmTarget = "1.8"
    freeCompilerArgs = listOf("-progressive")
  }
}

val moshiVersion = "1.11.0-SNAPSHOT"
dependencies {
  implementation("com.google.code.gson:gson:2.8.6")
  implementation("com.squareup.moshi:moshi:$moshiVersion")

  kaptTest("com.squareup.moshi:moshi-kotlin-codegen:$moshiVersion")
  testImplementation("com.squareup.moshi:moshi-kotlin:$moshiVersion")
  testImplementation("junit:junit:4.13")
  testImplementation("com.google.truth:truth:1.0.1")
}