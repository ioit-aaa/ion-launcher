plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "one.zagura.IonLauncher"
    compileSdk = 35

    defaultConfig {
        applicationId = "one.zagura.IonLauncher"
        minSdk = 21
        targetSdk = 35
        versionCode = 17
        versionName = "y25-v2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        resourceConfigurations.addAll(listOf("notnight", "small"))
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFile("proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        buildConfig = true
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation("com.willowtreeapps:fuzzywuzzy-kotlin-jvm:0.9.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
//    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("com.google.android.material:material:1.12.0")

    implementation("com.kieronquinn.smartspacer:sdk-client:1.1") {
        exclude(group = "com.github.skydoves", module = "balloon")
    }

    // Idk how else to control wallpaper zoom
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")
}