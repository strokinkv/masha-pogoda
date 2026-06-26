import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.serialization")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

android {
    namespace = "masha.pogoda"
    compileSdk = 35

    defaultConfig {
        applicationId = "masha.pogoda"
        minSdk = 31
        targetSdk = 35
        versionCode = 6
        versionName = "1.0.5"

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }

        buildConfigField(
            "String",
            "YANDEX_DEV_KEY",
            "\"${localProperties.getProperty("YANDEX_DEV_KEY", "")}\""
        )
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    val keystorePath = System.getenv("KEYSTORE_FILE")
        ?: localProperties.getProperty("KEYSTORE_FILE")

    signingConfigs {
        create("release") {
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                    ?: localProperties.getProperty("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                    ?: localProperties.getProperty("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
                    ?: localProperties.getProperty("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Подписываем release только если keystore задан (env или local.properties);
            // иначе собираем неподписанный APK (например, в форках без секретов).
            signingConfig = if (keystorePath != null) {
                signingConfigs.getByName("release")
            } else {
                null
            }
        }
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.caverock:androidsvg-aar:1.4")

    testImplementation("junit:junit:4.13.2")
}
