import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

abstract class GenerateIosProductFeatureDefinitions : DefaultTask() {
    @get:Input
    abstract val selectedFeatures: ListProperty<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val features = selectedFeatures.get()
        val file = outputDir.get().file(
            "com/aeshma/multiapp/application/PlatformFeatureDefinitions.ios.kt",
        ).asFile
        file.parentFile.mkdirs()

        val imports = buildList {
            add("import com.aeshma.multiapp.core.model.FeatureDefinitionSpec")
            add("import com.aeshma.multiapp.core.model.FeatureRuntimeContributor")
            if ("orders" in features) add("import com.aeshma.multiapp.feature.orders.OrdersFeature")
            if ("lists" in features) add("import com.aeshma.multiapp.feature.lists.ListsFeature")
            if ("catalog" in features) add("import com.aeshma.multiapp.feature.catalog.CatalogFeature")
            if ("productdetails" in features) add("import com.aeshma.multiapp.feature.productdetails.ProductDetailsFeature")
            if ("delivery" in features) add("import com.aeshma.multiapp.feature.delivery.DeliveryFeature")
        }.joinToString("\n")

        val definitions = buildList {
            if ("orders" in features) add("OrdersFeature.definition")
            if ("lists" in features) add("ListsFeature.definition")
            if ("catalog" in features) add("CatalogFeature.definition")
            if ("productdetails" in features) add("ProductDetailsFeature.definition")
            if ("delivery" in features) add("DeliveryFeature.definition")
        }.joinToString(",\n    ")

        val contributors = buildList {
            if ("orders" in features) add("OrdersFeature.runtimeContributor")
            if ("lists" in features) add("ListsFeature.runtimeContributor")
            if ("catalog" in features) add("CatalogFeature.runtimeContributor")
            if ("productdetails" in features) add("ProductDetailsFeature.runtimeContributor")
            if ("delivery" in features) add("DeliveryFeature.runtimeContributor")
        }.joinToString(",\n    ")

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

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.metro)
    alias(libs.plugins.skie)
}

val iosProductBundle = providers.gradleProperty("iosProductBundle").orElse("superApp")
val iosFeatureBundles = mapOf(
    "appOne" to listOf("orders", "catalog", "productdetails", "delivery"),
    "appTwo" to listOf("orders", "lists", "delivery"),
    "superApp" to listOf("orders", "lists", "catalog", "productdetails", "delivery"),
)
val selectedIosFeatures = iosFeatureBundles[iosProductBundle.get()]
    ?: error("Unknown iosProductBundle '${iosProductBundle.get()}'. Expected one of ${iosFeatureBundles.keys}.")
val generatedIosFeatureDefinitionsDir = layout.buildDirectory.dir(
    "generated/iosProductFeatureDefinitions/${iosProductBundle.get()}/kotlin",
)

val generateIosProductFeatureDefinitions by tasks.registering(GenerateIosProductFeatureDefinitions::class) {
    selectedFeatures.set(selectedIosFeatures)
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
            if ("orders" in selectedIosFeatures) api(projects.shared.features.orders)
            if ("lists" in selectedIosFeatures) api(projects.shared.features.lists)
            if ("catalog" in selectedIosFeatures) api(projects.shared.features.catalog)
            if ("productdetails" in selectedIosFeatures) api(projects.shared.features.productdetails)
            if ("delivery" in selectedIosFeatures) api(projects.shared.features.delivery)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(projects.shared.features.orders)
            implementation(projects.shared.features.lists)
            implementation(projects.shared.features.catalog)
            implementation(projects.shared.features.productdetails)
            implementation(projects.shared.features.delivery)
        }
    }
}

tasks.configureEach {
    if (name.startsWith("compileKotlinIos") || name.startsWith("compileTestKotlinIos")) {
        dependsOn(generateIosProductFeatureDefinitions)
    }
}
