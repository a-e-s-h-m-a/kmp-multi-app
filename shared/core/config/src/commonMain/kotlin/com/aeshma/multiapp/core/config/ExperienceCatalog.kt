package com.aeshma.multiapp.core.config

import com.aeshma.multiapp.core.model.AppId
import com.aeshma.multiapp.core.model.BusinessUnitId
import com.aeshma.multiapp.core.model.CommerceCapabilities
import com.aeshma.multiapp.core.model.PermissionId
import com.aeshma.multiapp.core.model.RoleId

class ExperienceCatalog(definitions: List<ExperienceDefinition>) {
    private val definitionsByName = definitions.associateBy { it.appId.externalName.lowercase() }

    init {
        require(definitions.isNotEmpty()) { "At least one experience must be configured." }
        require(definitionsByName.size == definitions.size) { "Experience ids must be unique." }
    }

    fun definition(appId: AppId): ExperienceDefinition =
        definitionsByName[appId.externalName.lowercase()] ?: throw UnknownExperienceException(appId)
}

class BusinessUnitCatalog(definitions: List<BusinessUnitDefinition>) {
    private val definitionsByName = definitions.associateBy { it.id.value.lowercase() }

    init {
        require(definitions.isNotEmpty()) { "At least one business unit must be configured." }
        require(definitionsByName.size == definitions.size) { "Business unit ids must be unique." }
    }

    val definitions: List<BusinessUnitDefinition> = definitions

    fun definition(businessUnitId: BusinessUnitId): BusinessUnitDefinition =
        definitionsByName[businessUnitId.value.lowercase()] ?: throw UnknownBusinessUnitException(businessUnitId)
}

class PermissionTemplateCatalog(definitions: List<PermissionTemplate>) {
    private val definitionsByName = definitions.associateBy { it.role.value.lowercase() }

    init {
        require(definitions.isNotEmpty()) { "At least one permission template must be configured." }
        require(definitionsByName.size == definitions.size) { "Permission template roles must be unique." }
    }

    fun capabilitiesFor(roles: Set<RoleId>): CommerceCapabilities =
        roles.fold(CommerceCapabilities.none()) { capabilities, role ->
            capabilities.plus(definitionsByName[role.value.lowercase()]?.commerceCapabilities ?: CommerceCapabilities.none())
        }
}

fun defaultExperienceDefinitions(): List<ExperienceDefinition> = listOf(
    ExperienceDefinition(
        appId = AppId.AppOne,
        displayName = "Newport&Buckhead",
        url = null,
        logo = null,
        allowedSites = setOf("BHNP"),
        theme = "SSMG Boutique Theme",
        commerceCapabilities = CommerceCapabilities.of(
            "orders.view",
            "orders.edit",
            "lists.view",
            "catalog.view",
            "pdp.view",
            "delivery.view",
            "delivery.status",
        ),
        supportedBusinessUnits = setOf(BusinessUnitId.SSMG),
    ),
    ExperienceDefinition(
        appId = AppId.AppTwo,
        displayName = "Shop",
        url = null,
        logo = null,
        allowedSites = setOf("USBL"),
        theme = "Broadline Theme",
        commerceCapabilities = CommerceCapabilities.of(
            "orders.view",
            "orders.notifications",
            "lists.view",
            "lists.purchaseHistory",
            "catalog.view",
            "catalog.recommendations",
            "pdp.view",
            "delivery.view",
            "delivery.progress",
            "delivery.map",
            "delivery.invoices",
        ),
        supportedBusinessUnits = setOf(BusinessUnitId.USBL),
    ),
)

fun defaultBusinessUnitDefinitions(): List<BusinessUnitDefinition> = listOf(
    BusinessUnitDefinition(
        id = BusinessUnitId.SSMG,
        allowedExperiences = setOf(AppId.AppOne),
        commerceCapabilities = CommerceCapabilities.of("orders.view", "catalog.view", "delivery.view"),
    ),
    BusinessUnitDefinition(
        id = BusinessUnitId.USBL,
        allowedExperiences = setOf(AppId.AppTwo),
        commerceCapabilities = CommerceCapabilities.of("orders.view", "lists.view", "catalog.view", "delivery.view"),
    ),
)

fun defaultPermissionTemplates(): List<PermissionTemplate> = listOf(
    PermissionTemplate(
        role = RoleId.Customer,
        commerceCapabilities = CommerceCapabilities.of(
            "orders.view",
            "lists.view",
            "catalog.view",
            "pdp.view",
            "delivery.view",
            "delivery.status",
        ),
    ),
    PermissionTemplate(
        role = RoleId.CustomerAdmin,
        commerceCapabilities = CommerceCapabilities.of(
            "orders.view",
            "orders.edit",
            "orders.notifications",
            "lists.view",
            "lists.edit",
            "lists.purchaseHistory",
            "catalog.view",
            "catalog.recommendations",
            "pdp.view",
            "pdp.internalDetails",
            "delivery.view",
            "delivery.edit",
            "delivery.progress",
            "delivery.status",
            "delivery.map",
            "delivery.invoices",
        ),
    ),
)
