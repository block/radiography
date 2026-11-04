/*
 * Copyright 2020 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("com.android.library")
  kotlin("android")
  id("com.vanniktech.maven.publish.base")
}

android {
  compileSdk = 35

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }

  defaultConfig {
    minSdk = 23
    targetSdk = 35
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildFeatures { buildConfig = false }

  testOptions { execution = "ANDROIDX_TEST_ORCHESTRATOR" }
  namespace = "com.squareup.radiography"
  testNamespace = "com.squareup.radiography.test"
}

tasks.withType<KotlinCompile> {
  compilerOptions {
    freeCompilerArgs.addAll(
      listOfNotNull(
        "-opt-in=kotlin.RequiresOptIn",

        // Require explicit public modifiers and types.
        // TODO this should be moved to a top-level `kotlin { explicitApi() }` once that's working
        //  for android projects, see https://youtrack.jetbrains.com/issue/KT-37652.
        "-Xexplicit-api=strict"
          .takeUnless {
            // Tests aren't part of the public API, don't turn explicit API mode on for them.
            name.contains("test", ignoreCase = true)
          },
      )
    )
  }
}

dependencies {
  implementation(libs.curtains)
  // We don't want to bring any Compose dependencies in unless the consumer of this library is
  // bringing them in itself.
  compileOnly(libs.compose.toolingData)

  testImplementation(libs.test.junit)
  testImplementation(libs.test.mockito)
  testImplementation(libs.test.robolectric)
  testImplementation(libs.test.truth)

  androidTestImplementation(libs.test.androidx.core)
  androidTestImplementation(libs.test.androidx.espresso)
  androidTestImplementation(libs.test.androidx.rules)
  androidTestImplementation(libs.test.androidx.runner)
  androidTestImplementation(libs.test.truth)
  androidTestUtil(libs.test.androidx.orchestrator)
}
