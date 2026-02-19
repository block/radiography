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
  namespace = "radiography.stoic"
  compileSdk = 35

  defaultConfig {
    applicationId = "radiography.stoic"
    minSdk = 26
    targetSdk = 35
    versionCode = 1
    versionName = project.property("VERSION_NAME") as String
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }

  kotlinOptions {
    jvmTarget = "1.8"
  }
}

dependencies {
  compileOnly("com.squareup.stoic:plugin-sdk:0.7.0")
  implementation(kotlin("stdlib"))
  implementation(project(":radiography"))
}

// Task to create a distribution tar.gz
tasks.register<Tar>("dist") {
  dependsOn("assembleDebug")

  archiveBaseName.set("radiography-stoic-plugin")
  archiveVersion.set(project.property("VERSION_NAME") as String)
  compression = Compression.GZIP
  archiveExtension.set("tar.gz")

  into("radiography-stoic-plugin") {
    // Include the APK
    from("${layout.buildDirectory.get()}/outputs/apk/debug") {
      include("stoic-plugin-debug.apk")
    }

    // Include the wrapper script and README from prebuilt
    from("${projectDir}/prebuilt") {
      include("radiography")
      include("README.md")
      filePermissions {
        unix("rwxr-xr-x")
      }
    }
  }

  destinationDirectory.set(layout.buildDirectory.dir("distributions"))
}

// Convenience alias
tasks.register("distribution") {
  dependsOn("dist")
}
