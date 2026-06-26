package com.aeshma.multiapp.core.config

import com.aeshma.multiapp.core.model.AppContext
import com.aeshma.multiapp.core.model.AppId
import com.aeshma.multiapp.core.model.BusinessUnitId
import com.aeshma.multiapp.core.model.CommerceCapabilities
import com.aeshma.multiapp.core.model.PermissionId
import com.aeshma.multiapp.core.model.RoleId
import com.aeshma.multiapp.core.model.UserType

class AppCatalog(
    definitions: List<AppDefinition>,
    private val experienceCatalog: ExperienceCatalog = ExperienceCatalog(defaultExperienceDefinitions()),
    private val businessUnitCatalog: BusinessUnitCatalog = BusinessUnitCatalog(defaultBusinessUnitDefinitions()),
    private val permissionTemplateCatalog: PermissionTemplateCatalog = PermissionTemplateCatalog(defaultPermissionTemplates()),
) {
    private val definitionsByName = definitions.associateBy { it.id.externalName.lowercase() }

    init {
        require(definitions.isNotEmpty()) { "At least one app must be configured." }
        require(definitionsByName.size == definitions.size) { "App ids must be unique." }
    }

    fun definition(appId: AppId): AppDefinition =
        definitionsByName[appId.externalName.lowercase()] ?: throw UnknownAppException(appId)

    fun contextFor(appId: AppId, username: String): AppContext {
        val definition = definition(appId)
        val normalizedUsername = username.normalizedUsername()
        val profile = definition.profileFor(normalizedUsername)
        val businessUnitId = businessUnitFor(definition.id)
        val roles = rolesFor(profile.userType, normalizedUsername)
        val explicitPermissions = explicitPermissionsFor(profile.userType, normalizedUsername)
        val commerceCapabilities = commerceCapabilitiesFor(
            appId = definition.id,
            businessUnitId = businessUnitId,
            roles = roles,
            explicitPermissions = explicitPermissions,
        )

        return AppContext(
            appId = definition.id,
            businessUnitId = businessUnitId,
            userId = "${definition.configKey}-$normalizedUsername",
            userType = profile.userType,
            roles = roles,
            explicitPermissions = explicitPermissions,
            commerceCapabilities = commerceCapabilities,
            capabilities = profile.capabilities,
        )
    }

    private fun businessUnitFor(appId: AppId): BusinessUnitId =
        businessUnitCatalog.definitions
            .firstOrNull { appId in it.allowedExperiences }
            ?.id
            ?: error("No demo business unit mapping configured for '${appId.externalName}'.")

    private fun rolesFor(userType: UserType, username: String): Set<RoleId> =
        when {
            username == "admin" -> setOf(RoleId.CustomerAdmin, RoleId.InternalUser)
            username == "driver" -> setOf(RoleId.DeliveryUser)
            username == "readonly" -> setOf(RoleId.Customer)
            userType == UserType.Admin -> setOf(RoleId.CustomerAdmin)
            userType == UserType.Merchant -> setOf(RoleId.CustomerAdmin)
            else -> setOf(RoleId.Customer)
        }

    private fun explicitPermissionsFor(userType: UserType, username: String): Set<PermissionId> =
        when {
            username == "driver" -> setOf(
                PermissionId.DeliveryView,
                PermissionId.DeliveryProgress,
                PermissionId.DeliveryStatus,
                PermissionId.DeliveryMap,
            )
            userType == UserType.Admin -> setOf(PermissionId.PdpInternalDetails)
            else -> emptySet()
        }

    private fun commerceCapabilitiesFor(
        appId: AppId,
        businessUnitId: BusinessUnitId,
        roles: Set<RoleId>,
        explicitPermissions: Set<PermissionId>,
    ): CommerceCapabilities {
        val experience = experienceCatalog.definition(appId)
        val businessUnit = businessUnitCatalog.definition(businessUnitId)
        require(appId in businessUnit.allowedExperiences && businessUnit.id in experience.supportedBusinessUnits) {
            "Experience '${appId.externalName}' is not allowed for business unit '${businessUnit.id.value}'."
        }

        val userGrantedCapabilities = permissionTemplateCatalog.capabilitiesFor(roles)
            .plus(CommerceCapabilities(explicitPermissions))

        return userGrantedCapabilities
            .intersect(businessUnit.allowedCapabilities)
            .intersect(experience.supportedCapabilities)
    }
}
