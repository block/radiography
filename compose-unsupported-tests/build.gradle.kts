import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("com.android.library")
  kotlin("android")
  id("org.jetbrains.kotlin.plugin.compose")
}

android {
  compileSdk = 37

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  defaultConfig {
    minSdk = 23
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  testOptions { targetSdk = 35 }

  buildFeatures {
    buildConfig = false
    compose = true
  }

  packaging { resources.excludes += listOf("META-INF/AL2.0", "META-INF/LGPL2.1") }
  namespace = "com.squareup.radiography.test.compose.unsupported.empty"
  testNamespace = "com.squareup.radiography.test.compose.unsupported"
}

tasks.withType<KotlinCompile> {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_11)
    freeCompilerArgs.addAll("-opt-in=kotlin.RequiresOptIn")
  }
}

dependencies {
  androidTestImplementation(project(":radiography"))
  androidTestImplementation(libs.appCompat)
  androidTestImplementation(libs.compose.old.activity)
  androidTestImplementation(libs.compose.old.material)
  androidTestImplementation(libs.compose.testing)
  androidTestImplementation(libs.test.androidx.rules)
  androidTestImplementation(libs.test.junit)
  androidTestImplementation(libs.test.androidx.runner)
  androidTestImplementation(libs.test.truth)
}
