package com.aeshma.multiapp.application

import com.aeshma.multiapp.core.config.AppCatalog
import com.aeshma.multiapp.core.config.BusinessUnitCatalog
import com.aeshma.multiapp.core.config.BusinessUnitDefinition
import com.aeshma.multiapp.core.config.ExperienceCatalog
import com.aeshma.multiapp.core.config.ExperienceDefinition
import com.aeshma.multiapp.core.config.HardcodedLoginConfig
import com.aeshma.multiapp.core.config.PermissionTemplateCatalog
import com.aeshma.multiapp.core.config.ProductCatalog
import com.aeshma.multiapp.core.config.ProductDefinition
import com.aeshma.multiapp.core.config.SimulatedLoginGrant
import com.aeshma.multiapp.core.config.UnsupportedExperienceException
import com.aeshma.multiapp.core.config.defaultAppDefinitions
import com.aeshma.multiapp.core.config.defaultBusinessUnitDefinitions
import com.aeshma.multiapp.core.config.defaultExperienceDefinitions
import com.aeshma.multiapp.core.config.defaultPermissionTemplates
import com.aeshma.multiapp.core.config.defaultProductDefinitions
import com.aeshma.multiapp.core.config.explicitCommerceCapabilities
import com.aeshma.multiapp.core.model.AppContext
import com.aeshma.multiapp.core.model.AppId
import com.aeshma.multiapp.core.model.BusinessUnitId
import com.aeshma.multiapp.core.model.CommerceCapabilities
import com.aeshma.multiapp.core.model.ExperienceId
import com.aeshma.multiapp.core.model.ProductId
import com.aeshma.multiapp.core.model.RoleId

data class ResolvedExperienceOption(
    val grant: SimulatedLoginGrant,
    val experience: ExperienceDefinition,
    val context: AppContext,
)

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
        .map(experienceCatalog::definition)
        .map { appCatalog.definition(it.hostAppId) }

    val supportedExperienceDefinitions: List<ExperienceDefinition> = productDefinition.supportedExperiences
        .map(experienceCatalog::definition)

    val businessUnits: List<BusinessUnitDefinition> = businessUnitCatalog.definitions

    val defaultExperience: ExperienceId? = productDefinition.defaultExperience

    val supportedUsernames: List<String> = supportedExperiences
        .flatMap { it.supportedUsernames }
        .distinct()

    val defaultUsername: String = defaultExperience
        ?.let(experienceCatalog::definition)
        ?.hostAppId
        ?.let(appCatalog::definition)
        ?.defaultUsername
        ?: supportedExperiences.first().defaultUsername

    fun experienceDefinition(experienceId: ExperienceId): ExperienceDefinition =
        experienceCatalog.definition(experienceId)

    fun allowedExperiencesFor(businessUnitId: BusinessUnitId): List<ExperienceDefinition> {
        val businessUnit = businessUnitCatalog.definition(businessUnitId)

        return productDefinition.supportedExperiences
            .intersect(businessUnit.allowedExperiences)
            .map(experienceCatalog::definition)
            .filter { businessUnit.id in it.supportedBusinessUnits }
    }

    fun resolvedCommerceCapabilities(
        experienceId: ExperienceId,
        businessUnitId: BusinessUnitId,
        roles: Set<RoleId>,
        explicitCapabilities: CommerceCapabilities = CommerceCapabilities.none(),
    ): CommerceCapabilities {
        val experience = experienceCatalog.definition(experienceId)
        val businessUnit = businessUnitCatalog.definition(businessUnitId)
        if (experienceId !in businessUnit.allowedExperiences || businessUnit.id !in experience.supportedBusinessUnits) {
            throw UnsupportedExperienceException(productId, experienceId)
        }

        val userGrantedCapabilities = permissionTemplateCatalog.capabilitiesFor(roles)
            .plus(explicitCapabilities)

        return userGrantedCapabilities
            .intersect(businessUnit.allowedCapabilities)
            .intersect(experience.supportedCapabilities)
    }

    fun resolvedExperienceOptions(username: String): List<ResolvedExperienceOption> =
        HardcodedLoginConfig.loginGrants(productId, username).flatMap { grant ->
            productDefinition.supportedExperiences.mapNotNull { experienceId ->
                val experience = experienceCatalog.definition(experienceId)
                val businessUnit = businessUnitCatalog.definition(grant.businessUnitId)
                if (experienceId !in businessUnit.allowedExperiences || businessUnit.id !in experience.supportedBusinessUnits) {
                    null
                } else {
                    val capabilities = resolvedCommerceCapabilities(
                        experienceId = experienceId,
                        businessUnitId = grant.businessUnitId,
                        roles = grant.roles,
                        explicitCapabilities = grant.explicitCommerceCapabilities(),
                    )
                    ResolvedExperienceOption(
                        grant = grant,
                        experience = experience,
                        context = AppContext(
                            appId = experience.hostAppId,
                            businessUnitId = grant.businessUnitId,
                            userId = "${productDefinition.id.externalName}-${username.trim().lowercase()}-${grant.id}",
                            userType = grant.userType,
                            roles = grant.roles,
                            explicitPermissions = grant.explicitPermissions,
                            commerceCapabilities = capabilities,
                            capabilities = grant.capabilities,
                        ),
                    )
                }
            }
        }

    fun appRuntimeFor(experienceId: ExperienceId): AppRuntime {
        val experienceDefinition = experienceCatalog.definition(experienceId)
        if (experienceDefinition.id !in productDefinition.supportedExperiences) {
            throw UnsupportedExperienceException(productId, experienceDefinition.id)
        }
        val appDefinition = appCatalog.definition(experienceDefinition.hostAppId)

        return appRuntimesByName.getOrPut(appDefinition.id.externalName.lowercase()) {
            createAppRuntime(appDefinition.id, appCatalog)
        }
    }

    fun appRuntimeForHost(appId: AppId): AppRuntime =
        createAppRuntime(appId, appCatalog)
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
