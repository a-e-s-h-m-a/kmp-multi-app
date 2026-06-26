import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(projects.shared.ui)

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "com.aeshma.multiapp.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.aeshma.multiapp"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures {
        buildConfig = true
        resValues = true
    }
    flavorDimensions += "app"
    productFlavors {
        create("appOne") {
            dimension = "app"
            applicationId = "com.aeshma.appone"
            resValue("string", "app_name", "AppOne")
            buildConfigField("String", "PRODUCT_ID", "\"AppOneStandalone\"")
        }
        create("appTwo") {
            dimension = "app"
            applicationId = "com.aeshma.apptwo"
            resValue("string", "app_name", "AppTwo")
            buildConfigField("String", "PRODUCT_ID", "\"AppTwoStandalone\"")
        }
        create("superApp") {
            dimension = "app"
            applicationId = "com.aeshma.superapp"
            resValue("string", "app_name", "Super App")
            buildConfigField("String", "PRODUCT_ID", "\"SuperApp\"")
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
