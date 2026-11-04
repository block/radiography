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
import kotlinx.validation.KotlinApiBuildTask
import kotlinx.validation.KotlinApiCompareTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("com.android.library")
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
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildFeatures { buildConfig = false }

  testOptions {
    execution = "ANDROIDX_TEST_ORCHESTRATOR"
    targetSdk = 35
  }
  namespace = "com.squareup.radiography"
  testNamespace = "com.squareup.radiography.test"
}

kotlin {
  // Require explicit public modifiers and types. AGP's built-in Kotlin only applies this to
  // production sources; tests aren't part of the public API.
  explicitApi()

  compilerOptions { freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn") }
}

// The binary-compatibility-validator plugin (applied in the root build script) only configures
// Android modules when the standalone org.jetbrains.kotlin.android plugin is applied, which AGP's
// built-in Kotlin support replaces. Until BCV (or KGP's own abiValidation) supports built-in
// Kotlin, reproduce BCV's kotlin-android wiring here using its public task types: apiBuild dumps
// the public API of the release compilation, apiCheck compares it with api/radiography.api, and
// apiDump updates that file.
val apiDir = layout.projectDirectory.dir("api")
val apiFileName = "${project.name}.api"

// BCV only adds kotlin-metadata-jvm to its worker classpath when it sees a Kotlin plugin id.
dependencies.add(
  "bcv-rt-jvm-cp",
  "org.jetbrains.kotlin:kotlin-metadata-jvm:${libs.versions.kotlin.get()}",
)

val apiBuild =
  tasks.register<KotlinApiBuildTask>("apiBuild") {
    description = "Builds Kotlin API for the release compilation of ${project.name}."
    // AGP's compilations don't populate output.classesDirs, so read the compile tasks' outputs.
    inputClassesDirs.from(
      tasks.named<KotlinCompile>("compileReleaseKotlin").flatMap { it.destinationDirectory },
      tasks.named<JavaCompile>("compileReleaseJavaWithJavac").flatMap { it.destinationDirectory },
    )
    inputDependencies.from(
      kotlin.target.compilations.named("release").map { it.compileDependencyFiles }
    )
    outputApiFile.set(layout.buildDirectory.file("api/$apiFileName"))
    runtimeClasspath.from(configurations.named("bcv-rt-jvm-cp-resolver"))
  }

val apiCheck =
  tasks.register<KotlinApiCompareTask>("apiCheck") {
    group = "verification"
    description =
      "Checks signatures of public API against the golden value in API folder for ${project.name}"
    projectApiFile.set(apiDir.file(apiFileName))
    generatedApiFile.set(apiBuild.flatMap { it.outputApiFile })
  }

tasks.register("apiDump") {
  description = "Syncs the API file for ${project.name}"
  val from = apiBuild.flatMap { it.outputApiFile }
  val to = apiDir.file(apiFileName)
  inputs.file(from)
  outputs.file(to)
  doLast { from.get().asFile.copyTo(to.asFile, overwrite = true) }
}

tasks.named("check") { dependsOn(apiCheck) }

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
