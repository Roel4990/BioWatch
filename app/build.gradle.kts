import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}

val localEnvironment = Properties().apply {
    val environmentFile = rootProject.file(".env")
    if (environmentFile.exists()) {
        environmentFile.inputStream().use { load(it) }
    }
}

fun environmentValue(name: String): String =
    providers.environmentVariable(name).orNull
        ?: localEnvironment.getProperty(name).orEmpty()

fun buildConfigString(value: String): String =
    34.toChar() + value + 34.toChar()

android {
    namespace = "com.example.biowatch"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.biowatch"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "ANALYSIS_API_BASE_URL", buildConfigString(environmentValue("ANALYSIS_API_BASE_URL")))
        buildConfigField("String", "ANALYSIS_API_TOKEN", buildConfigString(environmentValue("ANALYSIS_API_TOKEN")))
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    useLibrary("wear-sdk")
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.play.services.wearable)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.compose.navigation)
    implementation(libs.wear.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.core.splashscreen)
    implementation(libs.hilt.android)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.hilt.lifecycle.viewmodel.compose)
    implementation(libs.okhttp)
    implementation(libs.datastore.preferences)
    implementation(files("libs/samsung-health-sensor-api-1.4.1.aar"))
    kapt(libs.hilt.compiler)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}

kapt {
    correctErrorTypes = true
}
