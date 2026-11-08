import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Upload-key settings for release builds. Never commit the key or this file.
// Read from talk-timer-wear/keystore.properties, or from environment variables (for CI).
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun signingValue(key: String, env: String): String? =
    keystoreProps.getProperty(key) ?: System.getenv(env)

android {
    namespace = "dev.moonlightfield.talktimer"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.moonlightfield.talktimer"
        // Wear OS 3 (API 30) and later. Pixel Watch ships with Wear OS 3.5+.
        minSdk = 30
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"
    }

    signingConfigs {
        val storePath = signingValue("storeFile", "TALKTIMER_KEYSTORE")
        if (storePath != null) {
            create("release") {
                storeFile = rootProject.file(storePath)
                storePassword = signingValue("storePassword", "TALKTIMER_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "TALKTIMER_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "TALKTIMER_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // Shrink code and resources: smaller download and less memory on the watch.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Without keystore.properties the release build is unsigned and cannot be uploaded.
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    // Only ship the languages we have translated.
    androidResources {
        localeFilters += listOf("en", "ja", "ko", "zh-rCN")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.fragment)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.wear)
    implementation(libs.androidx.wear.ongoing)
    implementation(libs.androidx.wear.remote.interactions)
    implementation(libs.androidx.wear.compose.material3)
    implementation(libs.androidx.wear.compose.foundation)

    implementation(libs.androidx.wear.tiles)
    implementation(libs.androidx.wear.protolayout)
    implementation(libs.androidx.wear.complications.data.source.ktx)
    implementation(libs.androidx.concurrent.futures)

    testImplementation(libs.junit)
}
