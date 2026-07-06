import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

abstract class GenerateIosProductFeatureDefinitions : DefaultTask() {
    @get:Input
    abstract val featureIds: ListProperty<String>

    @get:Input
    abstract val featureKotlinObjects: MapProperty<String, String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val selectedFeatures = featureIds.get()
        val objectsByFeature = featureKotlinObjects.get()
        val selectedObjects = selectedFeatures.map { objectsByFeature.getValue(it) }
        val selectedObjectNames = selectedObjects.map { it.substringAfterLast(".") }
        val imports = buildList {
            add("import com.aeshma.multiapp.core.model.FeatureDefinitionSpec")
            add("import com.aeshma.multiapp.core.model.FeatureRuntimeContributor")
            selectedObjects.sorted().forEach { add("import $it") }
        }.joinToString("\n")
        val definitions = selectedObjectNames
            .joinToString(",\n    ") { "$it.definition" }
        val contributors = selectedObjectNames
            .joinToString(",\n    ") { "$it.runtimeContributor" }
        val file = outputDir.get().file(
            "com/aeshma/multiapp/application/PlatformFeatureDefinitions.ios.kt",
        ).asFile

        file.parentFile.mkdirs()
        file.writeText(
            """
            package com.aeshma.multiapp.application

            $imports

            internal actual fun platformFeatureDefinitions(): List<FeatureDefinitionSpec> = listOf(
                $definitions,
            )

            internal actual fun platformFeatureRuntimeContributors(): List<FeatureRuntimeContributor> = listOfNotNull(
                $contributors,
            )
            """.trimIndent(),
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
    val allProductFeatures = productBuildConfigs.flatMap(::productFeatures).distinct()
    val missingModulePaths = allProductFeatures.filterNot(featureModulePaths::containsKey)
    require(missingModulePaths.isEmpty()) {
        "Missing featureModules entries for ${missingModulePaths.joinToString()}."
    }

    val missingKotlinObjects = allProductFeatures.filterNot(featureKotlinObjectRefs::containsKey)
    require(missingKotlinObjects.isEmpty()) {
        "Missing featureKotlinObjects entries for ${missingKotlinObjects.joinToString()}."
    }
}

validateFeatureBundleConfig()

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.metro)
    alias(libs.plugins.skie)
}

val iosProductBundle = providers.gradleProperty("iosProductBundle").orElse("superApp")
val iosFeatureBundles = productBuildConfigs.associate { product ->
    product.getValue("swiftName").toString() to productFeatures(product)
}
val selectedIosFeatures = iosFeatureBundles[iosProductBundle.get()]
    ?: error("Unknown iosProductBundle '${iosProductBundle.get()}'. Expected one of ${iosFeatureBundles.keys}.")
val generatedIosFeatureDefinitionsDir = layout.buildDirectory.dir(
    "generated/iosProductFeatureDefinitions/${iosProductBundle.get()}/kotlin",
)

val generateIosProductFeatureDefinitions by tasks.registering(GenerateIosProductFeatureDefinitions::class) {
    inputs.file(rootProject.file("config/product-feature-bundles.json"))
    featureIds.set(selectedIosFeatures)
    featureKotlinObjects.set(featureKotlinObjectRefs)
    outputDir.set(generatedIosFeatureDefinitionsDir)
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "SharedLogic"
            isStatic = true
            export(projects.shared.core.model)
            export(projects.shared.core.config)
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
        }
        iosMain {
            kotlin.srcDir(generatedIosFeatureDefinitionsDir)
        }
        iosMain.dependencies {
            selectedIosFeatures.forEach { featureId ->
                api(project(featureModulePaths.getValue(featureId)))
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            featureModulePaths.values.distinct().forEach { featureModulePath ->
                implementation(project(featureModulePath))
            }
        }
    }
}

tasks.configureEach {
    if (name.startsWith("compileKotlinIos") || name.startsWith("compileTestKotlinIos")) {
        dependsOn(generateIosProductFeatureDefinitions)
    }
}
