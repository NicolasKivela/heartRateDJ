plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.heartratedj"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.heartratedj"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        manifestPlaceholders["appAuthRedirectScheme"] = "heartratedj"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/LICENSE",
                "META-INF/NOTICE",
                "META-INF/DEPENDENCIES"
            )
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.appauth)

    // Correct way to include the Spotify App Remote SDK AAR
    implementation(files("libs/spotify-app-remote-release-0.8.0.aar"))

    // ANT+ AAR file
    implementation(files("libs/antpluginlib_3-9-0.aar"))

    // Gson
    implementation("com.google.code.gson:gson:2.6.1")
}
