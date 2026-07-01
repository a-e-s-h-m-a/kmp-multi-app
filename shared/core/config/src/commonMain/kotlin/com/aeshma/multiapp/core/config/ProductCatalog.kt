package com.aeshma.multiapp.core.config

import com.aeshma.multiapp.core.model.ExperienceId
import com.aeshma.multiapp.core.model.ProductId

class ProductCatalog(definitions: List<ProductDefinition>) {
    private val definitionsByName = definitions.associateBy { it.id.externalName.lowercase() }

    init {
        require(definitions.isNotEmpty()) { "At least one product must be configured." }
        require(definitionsByName.size == definitions.size) { "Product ids must be unique." }
    }

    fun definition(productId: ProductId): ProductDefinition =
        definitionsByName[productId.externalName.lowercase()] ?: throw UnknownProductException(productId)
}

fun defaultProductDefinitions(): List<ProductDefinition> = listOf(
    ProductDefinition(
        id = ProductId.AppOneStandalone,
        displayName = "AppOne",
        supportedExperiences = setOf(ExperienceId.Shop),
        defaultExperience = ExperienceId.Shop,
        showsExperiencePicker = false,
    ),
    ProductDefinition(
        id = ProductId.AppTwoStandalone,
        displayName = "AppTwo",
        supportedExperiences = setOf(ExperienceId.NewportBuckhead),
        defaultExperience = ExperienceId.NewportBuckhead,
        showsExperiencePicker = false,
    ),
    ProductDefinition(
        id = ProductId.SuperApp,
        displayName = "Super App",
        supportedExperiences = setOf(ExperienceId.NewportBuckhead, ExperienceId.Shop),
        defaultExperience = null,
        showsExperiencePicker = true,
    ),
)
