plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.cancunconnect"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.cancunconnect"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    // Firebase
    implementation ("com.google.firebase:firebase-firestore-ktx")
    implementation ("com.google.firebase:firebase-auth-ktx")
    implementation ("com.google.firebase:firebase-analytics-ktx")
    implementation ("com.google.firebase:firebase-storage:20.2.1")
    implementation (platform("com.google.firebase:firebase-bom:33.3.0"))
    implementation("com.google.firebase:firebase-perf")
    //implementation ("com.google.firebase:firebase-perf:20.0.4")

    // Stripe para pagos
    //implementation ("com.stripe:stripe-android:20.10.0")
    //implementation ("com.stripe:stripe-android:20.20.0")
    implementation ("com.stripe:stripe-android:20.53.0")


    // Google Play Services (para el wallet si usas Google Pay en el futuro)
    implementation ("com.google.android.gms:play-services-wallet:18.1.3")
    implementation ("com.google.android.gms:play-services-auth:20.2.0")
    implementation ("com.google.android.gms:play-services-maps:18.0.2")
    implementation ("com.google.android.gms:play-services-location:21.0.1")
    implementation ("com.google.android.gms:play-services-ads:22.0.0")




    // UI/Material
    implementation ("com.google.android.material:material:1.9.0")
    implementation ("androidx.core:core-ktx:1.8.0")
    implementation ("androidx.appcompat:appcompat:1.4.0")
    implementation ("androidx.constraintlayout:constraintlayout:2.1.4")

    // Carga de imágenes
    implementation ("com.squareup.picasso:picasso:2.8")
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.navigation.fragment)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}