package com.aeshma.multiapp.application

import com.aeshma.multiapp.core.config.AppCatalog
import com.aeshma.multiapp.core.config.BusinessUnitCatalog
import com.aeshma.multiapp.core.config.BusinessUnitDefinition
import com.aeshma.multiapp.core.config.ExperienceCatalog
import com.aeshma.multiapp.core.config.ExperienceDefinition
import com.aeshma.multiapp.core.config.PermissionTemplateCatalog
import com.aeshma.multiapp.core.config.ProductCatalog
import com.aeshma.multiapp.core.config.ProductDefinition
import com.aeshma.multiapp.core.config.UnsupportedExperienceException
import com.aeshma.multiapp.core.config.defaultAppDefinitions
import com.aeshma.multiapp.core.config.defaultBusinessUnitDefinitions
import com.aeshma.multiapp.core.config.defaultExperienceDefinitions
import com.aeshma.multiapp.core.config.defaultPermissionTemplates
import com.aeshma.multiapp.core.config.defaultProductDefinitions
import com.aeshma.multiapp.core.model.AppId
import com.aeshma.multiapp.core.model.BusinessUnitId
import com.aeshma.multiapp.core.model.CommerceCapabilities
import com.aeshma.multiapp.core.model.ProductId
import com.aeshma.multiapp.core.model.RoleId

class ProductRuntime internal constructor(
    val productDefinition: ProductDefinition,
    private val appCatalog: AppCatalog,
    private val experienceCatalog: ExperienceCatalog,
    private val businessUnitCatalog: BusinessUnitCatalog,
    private val permissionTemplateCatalog: PermissionTemplateCatalog,
) {
    private val appRuntimesByName = mutableMapOf<String, AppRuntime>()

    val productId: ProductId = productDefinition.id

    val supportedExperiences = productDefinition.supportedExperiences
        .map(appCatalog::definition)

    val supportedExperienceDefinitions: List<ExperienceDefinition> = productDefinition.supportedExperiences
        .map(experienceCatalog::definition)

    val businessUnits: List<BusinessUnitDefinition> = businessUnitCatalog.definitions

    val defaultExperience: AppId? = productDefinition.defaultExperience

    fun experienceDefinition(appId: AppId): ExperienceDefinition =
        experienceCatalog.definition(appId)

    fun allowedExperiencesFor(businessUnitId: BusinessUnitId): List<ExperienceDefinition> {
        val businessUnit = businessUnitCatalog.definition(businessUnitId)

        return productDefinition.supportedExperiences
            .intersect(businessUnit.allowedExperiences)
            .map(experienceCatalog::definition)
            .filter { businessUnit.id in it.supportedBusinessUnits }
    }

    fun resolvedCommerceCapabilities(
        appId: AppId,
        businessUnitId: BusinessUnitId,
        roles: Set<RoleId>,
        explicitCapabilities: CommerceCapabilities = CommerceCapabilities.none(),
    ): CommerceCapabilities {
        val experience = experienceCatalog.definition(appId)
        val businessUnit = businessUnitCatalog.definition(businessUnitId)
        if (appId !in businessUnit.allowedExperiences || businessUnit.id !in experience.supportedBusinessUnits) {
            throw UnsupportedExperienceException(productId, appId)
        }

        return businessUnit.commerceCapabilities
            .plus(experience.commerceCapabilities)
            .plus(permissionTemplateCatalog.capabilitiesFor(roles))
            .plus(explicitCapabilities)
    }

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
    experienceCatalog: ExperienceCatalog = ExperienceCatalog(defaultExperienceDefinitions()),
    businessUnitCatalog: BusinessUnitCatalog = BusinessUnitCatalog(defaultBusinessUnitDefinitions()),
    permissionTemplateCatalog: PermissionTemplateCatalog = PermissionTemplateCatalog(defaultPermissionTemplates()),
): ProductRuntime =
    ProductRuntime(
        productDefinition = productCatalog.definition(productId),
        appCatalog = appCatalog,
        experienceCatalog = experienceCatalog,
        businessUnitCatalog = businessUnitCatalog,
        permissionTemplateCatalog = permissionTemplateCatalog,
    )
