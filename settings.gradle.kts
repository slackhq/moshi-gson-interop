pluginManagement {
  repositories {
    exclusiveContent {
      forRepository {
        maven {
          name = "JCenter"
          setUrl("https://jcenter.bintray.com/")
        }
      }
      filter {
        // Required for Dokka
        includeModule("org.jetbrains.kotlinx", "kotlinx-html-jvm")
        includeGroup("org.jetbrains.dokka")
        includeModule("org.jetbrains", "markdown")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

rootProject.name = "moshi-gson-interop"