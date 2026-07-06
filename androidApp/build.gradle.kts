import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

abstract class GenerateAndroidProductFeatureBundle : DefaultTask() {
    @get:Input
    abstract val featureIds: ListProperty<String>

    @get:Input
    abstract val featureKotlinObjects: MapProperty<String, String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val features = featureIds.get()
        val objectsByFeature = featureKotlinObjects.get()
        val imports = features
            .map { objectsByFeature.getValue(it) }
            .sorted()
        val featureObjects = features
            .map { objectsByFeature.getValue(it).substringAfterLast(".") }
        val outputFile = outputDir.get()
            .file("com/aeshma/multiapp/android/ProductFeatureBundle.kt")
            .asFile

        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            buildString {
                appendLine("package com.aeshma.multiapp.android")
                appendLine()
                appendLine("import com.aeshma.multiapp.core.model.FeatureDefinitionSpec")
                appendLine("import com.aeshma.multiapp.core.model.FeatureRuntimeContributor")
                imports.forEach { appendLine("import $it") }
                appendLine()
                appendLine("object ProductFeatureBundle {")
                appendLine("    val featureDefinitions: List<FeatureDefinitionSpec> = listOf(")
                featureObjects.forEach { appendLine("        $it.definition,") }
                appendLine("    )")
                appendLine()
                appendLine("    val featureRuntimeContributors: List<FeatureRuntimeContributor> = listOfNotNull(")
                featureObjects.forEach { appendLine("        $it.runtimeContributor,") }
                appendLine("    )")
                appendLine("}")
            },
        )
    }
}

@Suppress("UNCHECKED_CAST")
val productFeatureBundleConfig = JsonSlurper().parse(
    rootProject.file("config/product-feature-bundles.json"),
) as Map<String, Any>

@Suppress("UNCHECKED_CAST")
val featureModulePaths = productFeatureBundleConfig["featureModules"] as Map<String, String>

@Suppress("UNCHECKED_CAST")
val productBuildConfigs = productFeatureBundleConfig["products"] as List<Map<String, Any>>

@Suppress("UNCHECKED_CAST")
val featureKotlinObjectRefs = productFeatureBundleConfig["featureKotlinObjects"] as Map<String, String>

fun productFeatures(product: Map<String, Any>): List<String> =
    (product["bundledFeatures"] as List<*>).map { it.toString() }

fun validateFeatureBundleConfig() {
    val missingModulePaths = productBuildConfigs
        .flatMap(::productFeatures)
        .filterNot(featureModulePaths::containsKey)
        .distinct()
    require(missingModulePaths.isEmpty()) {
        "Missing featureModules entries for ${missingModulePaths.joinToString()}."
    }

    val missingKotlinObjects = productBuildConfigs
        .flatMap(::productFeatures)
        .filterNot(featureKotlinObjectRefs::containsKey)
        .distinct()
    require(missingKotlinObjects.isEmpty()) {
        "Missing featureKotlinObjects entries for ${missingKotlinObjects.joinToString()}."
    }
}

validateFeatureBundleConfig()

fun quotedBuildConfig(value: String): String = "\"$value\""

fun csvBuildConfig(values: Any?): String =
    (values as List<*>).joinToString(",") { it.toString() }

val productFeaturesByFlavor = productBuildConfigs.associate { product ->
    product.getValue("flavorName").toString() to productFeatures(product)
}

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
    productBuildConfigs.forEach { product ->
        val configurationName = "${product.getValue("flavorName")}Implementation"
        productFeatures(product).forEach { featureId ->
            add(configurationName, project(featureModulePaths.getValue(featureId)))
        }
    }
}

androidComponents {
    onVariants { variant ->
        val flavorName = variant.productFlavors
            .firstOrNull { it.first == "app" }
            ?.second
            ?: return@onVariants
        val taskName = "generate${variant.name.replaceFirstChar { it.uppercase() }}ProductFeatureBundle"
        val generateBundle = tasks.register<GenerateAndroidProductFeatureBundle>(taskName) {
            featureIds.set(productFeaturesByFlavor.getValue(flavorName))
            featureKotlinObjects.set(featureKotlinObjectRefs)
            outputDir.set(layout.buildDirectory.dir("generated/productFeatureBundles/${variant.name}/kotlin"))
        }

        variant.sources.java?.addGeneratedSourceDirectory(
            generateBundle,
            GenerateAndroidProductFeatureBundle::outputDir,
        )
    }
}
