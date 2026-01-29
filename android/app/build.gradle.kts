plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.reactonelle"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.reactonelle"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            // Em debug, define flag para carregar do localhost
            buildConfigField("Boolean", "USE_LOCAL_SERVER", "true")
            buildConfigField("String", "LOCAL_SERVER_URL", "\"http://10.0.2.0:5173\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("Boolean", "USE_LOCAL_SERVER", "false")
            buildConfigField("String", "LOCAL_SERVER_URL", "\"\"")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.webkit:webkit:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Biometria
    implementation("androidx.biometric:biometric:1.1.0")
    
    // Location (Google Play Services)
    implementation("com.google.android.gms:play-services-location:21.0.1")
    
    // QR Code (ZXing)
    implementation("com.google.zxing:core:3.5.2")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    
    // Lottie para animações da Splash Screen
    implementation("com.airbnb.android:lottie:6.3.0")
    
    // OneSignal Push Notifications
    implementation("com.onesignal:OneSignal:[5.1.0, 5.1.99]")
}
