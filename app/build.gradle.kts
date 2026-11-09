plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace  = "edu.uph.m24si2.trash2cash"
    compileSdk = 35

    defaultConfig {
        applicationId  = "edu.uph.m24si2.trash2cash"
        minSdk         = 26
        targetSdk      = 35
        versionCode    = 1
        versionName    = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    buildFeatures {
        viewBinding = true
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.androidx.cardview)

    // Material Design
    implementation(libs.material)

<<<<<<< Updated upstream
    // Firebase (BOM mengelola versi semua library Firebase secara otomatis)
    implementation(platform(libs.firebase.bom))
=======
    // Firebase Auth
>>>>>>> Stashed changes
    implementation(libs.firebase.auth)
    // Firebase Realtime Database — data live dari ESP32
    implementation(libs.firebase.database)
    // Firebase Firestore — data statis (profil, mesin, riwayat tarik)
    implementation(libs.firebase.firestore)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
