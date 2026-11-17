plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.saltyhaus.sosbridge"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.saltyhaus.sosbridge"
        minSdk = 26
        targetSdk = 29
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Intentionally no external dependencies -- plain framework widgets and
    // java.net.HttpURLConnection only, so this builds with nothing beyond the
    // Android SDK platform + AGP + Kotlin stdlib (already pulled in by the plugins).
}
