package com.aeshma.multiapp.core.config

import com.aeshma.multiapp.core.model.AppId
import com.aeshma.multiapp.core.model.BusinessUnitId
import com.aeshma.multiapp.core.model.CommerceCapabilities
import com.aeshma.multiapp.core.model.ExperienceId
import com.aeshma.multiapp.core.model.RoleId

class ExperienceCatalog(definitions: List<ExperienceDefinition>) {
    private val definitionsByName = definitions.associateBy { it.id.value.lowercase() }
    private val definitions: List<ExperienceDefinition> = definitions

    init {
        require(definitions.isNotEmpty()) { "At least one experience must be configured." }
        require(definitionsByName.size == definitions.size) { "Experience ids must be unique." }
    }

    fun definition(experienceId: ExperienceId): ExperienceDefinition =
        definitionsByName[experienceId.value.lowercase()] ?: throw UnknownExperienceException(experienceId)

    fun definitions(): List<ExperienceDefinition> = definitions
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
            capabilities.plus(definitionsByName[role.value.lowercase()]?.grantedCapabilities ?: CommerceCapabilities.none())
        }
}

fun defaultExperienceDefinitions(): List<ExperienceDefinition> = listOf(
    ExperienceDefinition(
        id = ExperienceId.NewportBuckhead,
        hostAppId = AppId.AppTwo,
        displayName = "Newport&Buckhead",
        url = null,
        logo = null,
        allowedSites = setOf("BHNP"),
        theme = "SSMG Boutique Theme",
        supportedCapabilities = CommerceCapabilities.of(
            "orders.view",
            "orders.edit",
            "lists.view",
            "catalog.view",
            "pdp.view",
            "delivery.view",
            "delivery.status",
        ),
        supportedBusinessUnits = setOf(BusinessUnitId.SSMG, BusinessUnitId.CABL),
    ),
    ExperienceDefinition(
        id = ExperienceId.Shop,
        hostAppId = AppId.AppOne,
        displayName = "Shop",
        url = null,
        logo = null,
        allowedSites = setOf("USBL", "CABL"),
        theme = "Broadline Theme",
        supportedCapabilities = CommerceCapabilities.of(
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
        supportedBusinessUnits = setOf(BusinessUnitId.USBL, BusinessUnitId.CABL),
    ),
)

fun defaultBusinessUnitDefinitions(): List<BusinessUnitDefinition> = listOf(
    BusinessUnitDefinition(
        id = BusinessUnitId.SSMG,
        allowedExperiences = setOf(ExperienceId.NewportBuckhead),
        allowedCapabilities = CommerceCapabilities.of(
            "orders.view",
            "orders.edit",
            "lists.view",
            "catalog.view",
            "pdp.view",
            "delivery.view",
            "delivery.status",
        ),
    ),
    BusinessUnitDefinition(
        id = BusinessUnitId.USBL,
        allowedExperiences = setOf(ExperienceId.Shop),
        allowedCapabilities = CommerceCapabilities.of(
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
    ),
    BusinessUnitDefinition(
        id = BusinessUnitId.CABL,
        allowedExperiences = setOf(ExperienceId.NewportBuckhead, ExperienceId.Shop),
        allowedCapabilities = CommerceCapabilities.of(
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

fun defaultPermissionTemplates(): List<PermissionTemplate> = listOf(
    PermissionTemplate(
        role = RoleId.Customer,
        grantedCapabilities = CommerceCapabilities.of(
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
        grantedCapabilities = CommerceCapabilities.of(
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
