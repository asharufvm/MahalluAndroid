plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.nextgenapps.Mahallu"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.nextgenapps.Mahallu"
        minSdk = 30
        targetSdk = 36
        versionCode = 4
        versionName = "1.0"

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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation("com.google.code.gson:gson:2.11.0")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

    implementation("com.google.firebase:firebase-config-ktx")

    implementation("androidx.compose.material3:material3:1.2.0")

    implementation("androidx.compose.material3:material3:1.2.1") // latest stable
    implementation("androidx.compose.material:material:1.6.1")   // for compatibility

    implementation("androidx.compose.material3:material3:1.3.0")

    implementation("com.google.accompanist:accompanist-swiperefresh:0.30.1")

    implementation("androidx.browser:browser:1.7.0")

    implementation("com.google.firebase:firebase-functions-ktx")

    // Add this line for the App Check Play Integrity provider
    implementation("com.google.firebase:firebase-appcheck-playintegrity")

    // Also make sure you have the Firebase BoM for consistent versions
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-appcheck")

    // Add these dependencies to your app's build.gradle file
    implementation("com.google.firebase:firebase-appcheck-playintegrity")

    implementation("io.coil-kt:coil-compose:2.6.0")

    // Firebase Storage
    implementation("com.google.firebase:firebase-storage-ktx:21.0.0")

    //implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.compose.material3:material3:1.2.0") // or latest

    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material:material-icons-extended:<latest-version>")
    implementation("com.google.firebase:firebase-firestore-ktx:25.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.2")
    implementation("androidx.compose.ui:ui-text:1.8.3") // or latest version
    implementation("androidx.navigation:navigation-compose:2.9.3")
    //implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-auth-ktx:23.2.1")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
apply(plugin = "com.google.gms.google-services")