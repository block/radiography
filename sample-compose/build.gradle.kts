import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("com.android.application")
  kotlin("android")
}

/** Use a separate property for the sample so we can test with different versions easily. */
val sampleComposeVersion = "1.6.8"
val sampleComposeCompilerVersion = "1.5.3"

android {
  compileSdk = 34

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }

  defaultConfig {
    minSdk = 21
    targetSdk = 34
    applicationId = "com.squareup.radiography.sample.compose"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildFeatures {
    compose = true
  }

  composeOptions {
    kotlinCompilerExtensionVersion = sampleComposeCompilerVersion
  }

  packaging {
    resources.excludes += listOf(
      "META-INF/AL2.0",
      "META-INF/LGPL2.1"
    )
  }
    namespace = "com.squareup.radiography.sample.compose"
    testNamespace = "com.squareup.radiography.sample.compose.test"
}

tasks.withType<KotlinCompile> {
  kotlinOptions {
    jvmTarget = "1.8"
    freeCompilerArgs += listOf(
      "-Xopt-in=kotlin.RequiresOptIn"
    )
  }
}

dependencies {
  implementation(project(":radiography"))
  implementation(project(":slot-table-inspector"))
  implementation(Dependencies.AppCompat)
  implementation(Dependencies.Compose(sampleComposeVersion).Activity())
  implementation(Dependencies.Compose(sampleComposeVersion).Material)
  implementation(Dependencies.Compose(sampleComposeVersion).Tooling)

  androidTestImplementation(Dependencies.Compose(sampleComposeVersion).Testing)
  androidTestImplementation(Dependencies.InstrumentationTests.Rules)
  androidTestImplementation(Dependencies.InstrumentationTests.Runner)
}
