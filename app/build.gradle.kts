plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.project.jibsin_project"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.project.jibsin_project"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true // Compose 활성화
        prefab = true  // OpenCV 사용
    }

    packaging {
        resources {
            excludes.add("META-INF/DEPENDENCIES")
            pickFirsts.add("google/protobuf/type.proto")
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Jetpack Compose dependencies
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Debugging tools
    debugImplementation("androidx.compose.ui:ui-tooling:1.5.4")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.5.4")

    implementation ("androidx.compose.material:material-icons-extended:1.5.4")

    implementation("com.google.accompanist:accompanist-systemuicontroller:0.31.0-alpha")
    implementation("com.google.accompanist:accompanist-pager:0.31.0-alpha")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.31.0-alpha")

    // firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.2"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-storage-ktx")  // Firebase Storage
    implementation("com.google.firebase:firebase-firestore-ktx") // Firebase Firestore

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.4.0")

    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("com.google.accompanist:accompanist-permissions:0.31.0-alpha")

    // OpenCV
    implementation("com.quickbirdstudios:opencv:4.5.3.0")

    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("com.airbnb.android:lottie:5.2.0") // Lottie 애니메이션
    implementation("com.airbnb.android:lottie-compose:6.0.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
}