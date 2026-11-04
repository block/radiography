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

plugins {
  id("com.android.application")
  kotlin("android")
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
    applicationId = "com.squareup.radiography.sample"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }
  namespace = "com.squareup.radiography.sample"
  testNamespace = "com.squareup.radiography.sample.test"
}

dependencies {
  implementation(project(":radiography"))
  implementation(libs.appCompat)
  implementation(libs.constraintLayout)

  androidTestImplementation(libs.test.androidx.core)
  androidTestImplementation(libs.test.androidx.espresso)
  androidTestImplementation(libs.test.androidx.rules)
  androidTestImplementation(libs.test.androidx.junit)
  androidTestImplementation(libs.test.androidx.runner)
  androidTestImplementation(libs.test.truth)
}
