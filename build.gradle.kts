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
import com.ncorti.ktfmt.gradle.KtfmtExtension
import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import kotlinx.validation.ApiValidationExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

println("Building with Kotlin compiler version ${libs.versions.kotlin.get()}")

buildscript {
  repositories {
    mavenCentral()
    gradlePluginPortal()
    google()
    // For binary compatibility validator.
    maven { url = uri("https://kotlin.bintray.com/kotlinx") }
  }

  dependencies {
    classpath(libs.build.android)
    classpath(libs.build.mavenPublish)
    classpath(libs.build.kotlin)
    classpath(libs.build.compose.compiler)
    classpath(libs.build.ktfmt)
    classpath(libs.build.binaryCompatibility)
    // Required for the gradle-maven-publish-plugin plugin.
    // See https://github.com/vanniktech/gradle-maven-publish-plugin/issues/205.
    classpath(libs.build.dokka)
  }
}

// We use JetBrain's Kotlin Binary Compatibility Validator to track changes to our public binary
// APIs.
// When making a change that results in a public ABI change, the apiCheck task will fail. When this
// happens, run ./gradlew apiDump to generate updated *.api files, and add those to your commit.
// See https://github.com/Kotlin/binary-compatibility-validator
apply(plugin = "binary-compatibility-validator")

extensions.configure<ApiValidationExtension> {
  // Ignore all sample projects, since they're not part of our API.
  // stoic-plugin builds a plugin to use with stoic - it's also not part of our API.
  // Only leaf project name is valid configuration, and every project must be individually ignored.
  // See https://github.com/Kotlin/binary-compatibility-validator/issues/3
  ignoredProjects =
    mutableSetOf(
      "compose-tests",
      "compose-unsupported-tests",
      "sample",
      "sample-compose",
      "stoic-plugin",
    )
}

// See https://stackoverflow.com/questions/25324880/detect-ide-environment-with-gradle
val isRunningFromIde
  get() = project.properties["android.injected.invoked.from.ide"] == "true"

allprojects {
  repositories {
    google()
    mavenCentral()
  }

  apply(plugin = "com.ncorti.ktfmt.gradle")

  // Code style is validated and applied using ktfmt (https://facebook.github.io/ktfmt/).
  //   ./gradlew ktfmtCheck   - Check formatting in all modules
  //   ./gradlew ktfmtFormat  - Fix formatting in all modules
  // Configuration: https://github.com/cortinico/ktfmt-gradle
  configure<KtfmtExtension> {
    // Match the style used by market and workflow-kotlin.
    googleStyle()
  }
}

subprojects {
  plugins.withId("com.vanniktech.maven.publish.base") {
    configure<MavenPublishBaseExtension> {
      publishToMavenCentral(automaticRelease = true)
      signAllPublications()
      pomFromGradleProperties()

      @Suppress("DEPRECATION")
      configure(
        AndroidSingleVariantLibrary(
          variant = "release",
          sourcesJar = true,
          publishJavadocJar = true,
        )
      )
    }
  }

  tasks.withType<KotlinCompile> {
    compilerOptions {
      // Allow warnings when running from IDE, makes it easier to experiment.
      if (!isRunningFromIde) {
        allWarningsAsErrors.set(true)
      }

      jvmTarget.set(JvmTarget.JVM_1_8)
    }
  }
}
