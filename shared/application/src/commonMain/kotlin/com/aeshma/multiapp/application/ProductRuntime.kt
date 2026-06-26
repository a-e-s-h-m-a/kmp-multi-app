package com.aeshma.multiapp.application

import com.aeshma.multiapp.core.config.AppCatalog
import com.aeshma.multiapp.core.config.ProductCatalog
import com.aeshma.multiapp.core.config.ProductDefinition
import com.aeshma.multiapp.core.config.UnsupportedExperienceException
import com.aeshma.multiapp.core.config.defaultAppDefinitions
import com.aeshma.multiapp.core.config.defaultProductDefinitions
import com.aeshma.multiapp.core.model.AppId
import com.aeshma.multiapp.core.model.ProductId

class ProductRuntime internal constructor(
    val productDefinition: ProductDefinition,
    private val appCatalog: AppCatalog,
) {
    private val appRuntimesByName = mutableMapOf<String, AppRuntime>()

    val productId: ProductId = productDefinition.id

    val supportedExperiences = productDefinition.supportedExperiences
        .map(appCatalog::definition)

    val defaultExperience: AppId? = productDefinition.defaultExperience

    fun appRuntimeFor(appId: AppId): AppRuntime {
        val appDefinition = appCatalog.definition(appId)
        if (appDefinition.id !in productDefinition.supportedExperiences) {
            throw UnsupportedExperienceException(productId, appDefinition.id)
        }

        return appRuntimesByName.getOrPut(appDefinition.id.externalName.lowercase()) {
            createAppRuntime(appDefinition.id, appCatalog)
        }
    }
}

fun createProductRuntime(
    productId: ProductId,
    appCatalog: AppCatalog = AppCatalog(defaultAppDefinitions()),
    productCatalog: ProductCatalog = ProductCatalog(defaultProductDefinitions()),
): ProductRuntime =
    ProductRuntime(
        productDefinition = productCatalog.definition(productId),
        appCatalog = appCatalog,
    )
