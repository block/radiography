plugins {
  id("com.android.library")
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
  namespace = "com.squareup.radiography.test.compose.empty"
  testNamespace = "com.squareup.radiography.test.compose"
}

kotlin { compilerOptions { freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn") } }

dependencies {
  androidTestImplementation(project(":radiography"))
  androidTestImplementation(libs.appCompat)
  androidTestImplementation(libs.compose.material)
  androidTestImplementation(libs.compose.testing)
  androidTestImplementation(libs.compose.tooling)
  androidTestImplementation(libs.test.androidx.rules)
  androidTestImplementation(libs.test.androidx.runner)
  androidTestImplementation(libs.test.truth)
}
