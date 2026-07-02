import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    androidLibrary {
        namespace = "com.aeshma.multiapp.feature.productdetails"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions { jvmTarget = JvmTarget.JVM_11 }
        withHostTest { }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared.core.model)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
