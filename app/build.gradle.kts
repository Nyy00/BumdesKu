plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") version "1.9.23-1.0.19"
}

android {
    namespace = "com.dony.bumdesku"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dony.bumdesku"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
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

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Material Design (untuk komponen UI yang modern)
    implementation("com.google.android.material:material:1.12.0")

    // Architectural Components - Penting untuk struktur aplikasi yang baik (MVVM)
    val lifecycleVersion = "2.8.1"
    // ViewModel: Untuk menyimpan dan mengelola data terkait UI
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    // LiveData: Untuk membuat data yang bisa "diamati" oleh UI
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")

    // Room Database: Untuk penyimpanan data lokal
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    // Annotation processor untuk Room
    ksp("androidx.room:room-compiler:$roomVersion")
    // Dukungan Kotlin Coroutines untuk Room
    implementation("androidx.room:room-ktx:$roomVersion")

    // Navigation Component: Untuk mengelola alur perpindahan antar layar/fragment
    val navVersion = "2.7.7"
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navVersion")

    // Kotlin Coroutines (untuk proses background)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // Lifecycle-aware collector for Compose
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.1")

    // Integration with Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.1")
}