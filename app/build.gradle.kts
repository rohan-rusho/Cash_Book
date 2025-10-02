plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    // id("com.google.firebase.crashlytics") // Uncomment when using Crashlytics
}

android {
    namespace = "com.moneytrackultra.cashbook"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.moneytrackultra.cashbook"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // multiDexEnabled = true // Uncomment if method count > 64K
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // For smaller APKs in production consider enabling both:
            // isMinifyEnabled = true
            // isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // If using Crashlytics later:
            // firebaseCrashlytics { mappingFileUploadEnabled = true }
        }
        debug {
            // For local Crashlytics opt‑out:
            // firebaseCrashlytics { mappingFileUploadEnabled = false }
        }
    }


        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
            isCoreLibraryDesugaringEnabled = true   // <-- correct
        }


    kotlinOptions {
        jvmTarget = "11"
        // To allow default interface methods:
        // freeCompilerArgs += "-Xjvm-default=all"
    }

    buildFeatures {
        viewBinding = true
        // compose = true // Enable if you start migrating to Compose
    }

    packaging {
        resources {
            excludes += listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/versions/9/previous-compilation-data.bin"
            )
        }
    }

    lint {
        abortOnError = true
        // sarifReport = true // For CI security scanning output
    }
}

dependencies {
    // Core / AndroidX (Version catalog drives versions)
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)

    // Charts
    implementation(libs.mpandroidchart)

    // Desugaring (java.time on < API 26 etc.)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Firebase BOM (pin all Firebase libs to consistent versions)
    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-firestore")
    // Optional:
    // implementation("com.google.firebase:firebase-analytics")
    // implementation("com.google.firebase:firebase-crashlytics")
    // implementation("com.google.firebase:firebase-messaging")
    // implementation("com.google.firebase:firebase-config")

    // Google Sign-In / Identity
    implementation("com.google.android.gms:play-services-auth:21.1.0")

    // JSON (Prefs or lightweight serialization)
    implementation("com.google.code.gson:gson:2.10.1")

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

/*
Optional future improvements:

1. Migrate to Java 17:
   - Update sourceCompatibility / targetCompatibility / jvmTarget
   - Update Gradle & AGP if needed.

2. Add BuildConfig fields:
android {
    defaultConfig {
        buildConfigField("String", "API_BASE_URL", "\"https://api.example.com\"")
    }
}

3. Dependency version alignment:
Use the libs.versions.toml for all new libraries to avoid ad hoc versions.

4. Enable R8 & resource shrinking in release for smaller APK:
release {
    isMinifyEnabled = true
    isShrinkResources = true
}

5. Compose adoption:
buildFeatures { compose = true }
composeOptions { kotlinCompilerExtensionVersion = "x.y.z" }

6. Crashlytics:
Add plugin and un-comment Crashlytics dependency.
*/