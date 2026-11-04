plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.plugin.compose")
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
    applicationId = "com.squareup.radiography.sample.compose"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildFeatures { compose = true }

  packaging { resources.excludes += listOf("META-INF/AL2.0", "META-INF/LGPL2.1") }
  namespace = "com.squareup.radiography.sample.compose"
  testNamespace = "com.squareup.radiography.sample.compose.test"
}

kotlin { compilerOptions { freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn") } }

dependencies {
  implementation(project(":radiography"))
  implementation(libs.appCompat)
  implementation(libs.compose.sample.activity)
  implementation(libs.compose.sample.material)
  implementation(libs.compose.sample.tooling)

  androidTestImplementation(libs.compose.sample.testing)
  androidTestImplementation(libs.test.androidx.rules)
  androidTestImplementation(libs.test.androidx.runner)
}
