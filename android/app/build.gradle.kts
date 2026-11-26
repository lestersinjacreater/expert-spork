import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Every CI build gets its own number (GitHub's run number) and commit, so the app
// shows exactly which build is installed ("Version 0.1.12 (89fa57a)") and Android
// refuses to put an older build over a newer one. A local build says "0.1.0 (local)".
val ciRunNumber: Int? = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()
val ciCommit: String = System.getenv("GITHUB_SHA")?.take(7) ?: "local"

android {
    namespace = "com.lester.typewriter"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lester.typewriter"
        minSdk = 26 // must run on cheap phones
        targetSdk = 36 // Google Play requires API 36 for new apps
        versionCode = ciRunNumber ?: 1
        versionName =
            if (ciRunNumber != null) "0.1.$ciRunNumber ($ciCommit)" else "0.1.0 (local)"
    }

    // A fixed debug key that is committed on purpose. It protects nothing: its
    // only job is to make every CI build carry the same signature, so a new build
    // installs over the old one instead of failing with "App not installed".
    // Google Play needs a separate, private upload key (added before release).
    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("debug") // sideloaded test builds
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true // version name on the welcome screen, debug-only tour log
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")

    testImplementation("junit:junit:4.13.2")
}
