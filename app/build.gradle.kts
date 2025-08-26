plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.mim.lifelog"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mim.lifelog"
        minSdk = 31
        targetSdk = 34
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    testImplementation(libs.junit) // Assuming this is JUnit 4, e.g., 4.13.2
    testImplementation("org.mockito:mockito-core:4.0.0")
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
    implementation("commons-codec:commons-codec:1.17.0")
    implementation(platform("com.google.firebase:firebase-bom:33.5.0")) // Use the Firebase BOM for version management
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation(libs.coil.compose)
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("org.json:json:20210307")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.compose.material3:material3:1.3.1") // Material 3
    implementation("androidx.compose.material:material:1.7.5") // Material 2 (fallback)
    implementation("androidx.compose.ui:ui:1.7.5")
    implementation("androidx.compose.foundation:foundation:1.7.5")
    implementation("androidx.compose.runtime:runtime:1.7.5")
    implementation("com.google.ai.client.generativeai:generativeai:0.5.0")
}