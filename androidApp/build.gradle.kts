import groovy.json.JsonSlurper
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

@Suppress("UNCHECKED_CAST")
val productBuildConfigs = (
    JsonSlurper().parse(rootProject.file("config/product-feature-bundles.json")) as Map<String, Any>
)["products"] as List<Map<String, Any>>

fun quotedBuildConfig(value: String): String = "\"$value\""

fun csvBuildConfig(values: Any?): String =
    (values as List<*>).joinToString(",") { it.toString() }

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
        productBuildConfigs.forEach { product ->
            create(product.getValue("flavorName").toString()) {
                dimension = "app"
                applicationId = product.getValue("androidApplicationId").toString()
                resValue("string", "app_name", product.getValue("displayName").toString())
                buildConfigField("String", "PRODUCT_ID", quotedBuildConfig(product.getValue("productId").toString()))
                buildConfigField(
                    "String",
                    "SUPPORTED_EXPERIENCES",
                    quotedBuildConfig(csvBuildConfig(product["supportedExperiences"])),
                )
                buildConfigField(
                    "String",
                    "BUNDLED_FEATURES",
                    quotedBuildConfig(csvBuildConfig(product["bundledFeatures"])),
                )
            }
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

dependencies {
    add("appOneImplementation", projects.shared.features.orders)
    add("appOneImplementation", projects.shared.features.catalog)
    add("appOneImplementation", projects.shared.features.productdetails)
    add("appOneImplementation", projects.shared.features.delivery)

    add("appTwoImplementation", projects.shared.features.orders)
    add("appTwoImplementation", projects.shared.features.lists)
    add("appTwoImplementation", projects.shared.features.delivery)

    add("superAppImplementation", projects.shared.features.orders)
    add("superAppImplementation", projects.shared.features.lists)
    add("superAppImplementation", projects.shared.features.catalog)
    add("superAppImplementation", projects.shared.features.productdetails)
    add("superAppImplementation", projects.shared.features.delivery)
}
