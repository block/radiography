import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("com.android.library")
  kotlin("android")
  id("com.vanniktech.maven.publish")
}

android {
  compileSdk = 30

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }

  defaultConfig {
    // Compose minSdk is also 21.
    minSdk = 21
    targetSdk = 30
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildFeatures {
    buildConfig = false
    compose = true
  }

  composeOptions {
    kotlinCompilerExtensionVersion = Versions.Compose
  }

  testOptions {
    execution = "ANDROIDX_TEST_ORCHESTRATOR"
  }
}

tasks.withType<KotlinCompile> {
  kotlinOptions {
    freeCompilerArgs += listOfNotNull(
      "-Xopt-in=kotlin.RequiresOptIn",

      // Require explicit public modifiers and types.
      // TODO this should be moved to a top-level `kotlin { explicitApi() }` once that's working
      //  for android projects, see https://youtrack.jetbrains.com/issue/KT-37652.
      "-Xexplicit-api=strict".takeUnless {
        // Tests aren't part of the public API, don't turn explicit API mode on for them.
        name.contains("test", ignoreCase = true)
      }
    )
  }
}

dependencies {
  implementation(Dependencies.Compose().Material)
  // implementation(Dependencies.Compose().ToolingData)
  implementation(Dependencies.Compose().Tooling)

  testImplementation(Dependencies.JUnit)
  testImplementation(Dependencies.Mockito)
  testImplementation(Dependencies.Robolectric)
  testImplementation(Dependencies.Truth)

  androidTestImplementation(Dependencies.Compose().Testing)
  androidTestImplementation(Dependencies.InstrumentationTests.Core)
  androidTestImplementation(Dependencies.InstrumentationTests.Espresso)
  androidTestImplementation(Dependencies.InstrumentationTests.Rules)
  androidTestImplementation(Dependencies.InstrumentationTests.Runner)
  androidTestImplementation(Dependencies.Truth)
  androidTestUtil(Dependencies.InstrumentationTests.Orchestrator)
}
