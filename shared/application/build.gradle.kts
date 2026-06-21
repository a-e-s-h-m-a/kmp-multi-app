import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.metro)
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "SharedLogic"
            isStatic = true
            export(projects.shared.core.model)
            export(projects.shared.core.config)
            export(projects.shared.features.delivery)
        }
    }

    androidLibrary {
        namespace = "com.aeshma.multiapp.application"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions { jvmTarget = JvmTarget.JVM_11 }
        withHostTest { }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared.core.model)
            api(projects.shared.core.config)
            api(projects.shared.core.analytics)
            api(projects.shared.features.delivery)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
