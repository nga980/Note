// File: app/build.gradle.kts

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android") // Đảm bảo plugin này được khai báo ĐẦU TIÊN hoặc rất sớm
    id("kotlin-kapt")                 // Plugin kapt cho annotation processing của Room
}

android {
    namespace = "com.example.notes"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.notes"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false // Đúng cú pháp cho Kotlin Script
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

    kotlinOptions { // Khối này giờ sẽ được nhận diện đúng
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
}

dependencies {
    // AndroidX core components - Sử dụng các phiên bản ổn định và phổ biến
    implementation("androidx.core:core-ktx:1.13.1") // Nên dùng -ktx cho Kotlin
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    // XÓA BỎ: implementation("androidx.room:room-common-jvm:2.7.1") - không cần thiết và có thể gây xung đột

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Room Persistence Library
    val roomVersion = "2.6.1" // Khai báo biến trong Kotlin Script
    implementation("androidx.room:room-runtime:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion") // Dùng kapt cho Room compiler
    implementation("androidx.room:room-ktx:$roomVersion") // Hỗ trợ coroutines cho Room (khuyến khích với Kotlin)

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Scalable Size Unit (sdp & ssp)
    implementation("com.intuit.sdp:sdp-android:1.1.1")
    implementation("com.intuit.ssp:ssp-android:1.1.1")

    // Rounded ImageView
    implementation("com.makeramen:roundedimageview:2.3.0")

    // Lifecycle Components - Sử dụng các phiên bản ổn định
    val lifecycleVersion = "2.7.0" // Khai báo biến
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-common-java8:$lifecycleVersion")
}