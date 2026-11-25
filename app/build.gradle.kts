import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun prop(key: String): String = localProps.getProperty(key, "").replace("\"", "\\\"")

android {
    namespace = "com.youtubevault.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.youtubevault.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        ndk {
            // Modern 64-bit Android architectures only (excludes legacy 32-bit armeabi-v7a & x86)
            abiFilters += listOf("arm64-v8a", "x86_64")
        }

        buildConfigField("String", "YOUTUBE_API_KEY", "\"${prop("YOUTUBE_API_KEY")}\"")
        buildConfigField("String", "GOOGLE_CLIENT_ID", "\"${prop("GOOGLE_CLIENT_ID")}\"")
        buildConfigField("String", "ALLOWED_EMAILS", "\"${prop("ALLOWED_EMAILS")}\"")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.okhttp)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.datasource.okhttp)
    implementation(libs.youtube.player)
    implementation(libs.play.services.auth)
    implementation(libs.kotlinx.coroutines.play.services)
}
