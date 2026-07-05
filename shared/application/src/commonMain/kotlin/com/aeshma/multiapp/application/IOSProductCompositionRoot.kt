package com.aeshma.multiapp.application

import com.aeshma.multiapp.core.model.BusinessUnitId
import com.aeshma.multiapp.core.model.ExperienceId
import com.aeshma.multiapp.core.model.ProductId

data class SharedBusinessUnit(
    val id: String,
)

data class SharedAppExperience(
    val id: String,
    val displayName: String,
    val theme: String,
    val allowedSites: List<String>,
    val defaultUsername: String,
    val supportedUsernames: List<String>,
)

data class SharedResolvedExperienceOption(
    val id: String,
    val appId: String,
    val displayName: String,
    val theme: String,
    val allowedSites: List<String>,
    val grantLabel: String,
    val businessUnitId: String,
    val roles: List<String>,
    val resolvedPermissions: List<String>,
)

class IOSProductCompositionRoot(productIdName: String) {
    private val runtime = createProductRuntime(
        productId = ProductId.fromExternalName(productIdName),
        featureDefinitions = platformFeatureDefinitions(),
    )
    private val snapshotMapper = SessionSnapshotMapper()

    val productName: String = runtime.productDefinition.displayName
    val showsExperiencePicker: Boolean = runtime.productDefinition.showsExperiencePicker
    val defaultExperienceId: String? = runtime.defaultExperience?.value
    val defaultUsername: String = runtime.defaultUsername
    val supportedUsernames: List<String> = runtime.supportedUsernames
    val businessUnits: List<SharedBusinessUnit> = runtime.businessUnits.map { definition ->
        SharedBusinessUnit(id = definition.id.value)
    }
    val supportedExperiences: List<SharedAppExperience> = runtime.supportedExperienceDefinitions.map { definition ->
        val appDefinition = runtime.appRuntimeFor(definition.id).appDefinition
        SharedAppExperience(
            id = definition.id.value,
            displayName = definition.displayName,
            theme = definition.theme,
            allowedSites = definition.allowedSites.toList(),
            defaultUsername = appDefinition.defaultUsername,
            supportedUsernames = appDefinition.supportedUsernames,
        )
    }

    fun supportedExperiencesForBusinessUnit(businessUnitIdName: String): List<SharedAppExperience> =
        runtime.allowedExperiencesFor(BusinessUnitId.fromExternalName(businessUnitIdName)).map { definition ->
            val appDefinition = runtime.appRuntimeFor(definition.id).appDefinition
            SharedAppExperience(
                id = definition.id.value,
                displayName = definition.displayName,
                theme = definition.theme,
                allowedSites = definition.allowedSites.toList(),
                defaultUsername = appDefinition.defaultUsername,
                supportedUsernames = appDefinition.supportedUsernames,
            )
        }

    fun resolvedExperienceOptions(username: String): List<SharedResolvedExperienceOption> =
        runtime.resolvedExperienceOptions(username).map { option ->
            SharedResolvedExperienceOption(
                id = "${option.grant.id}:${option.experience.id.value}",
                appId = option.experience.hostAppId.externalName,
                displayName = option.experience.displayName,
                theme = option.experience.theme,
                allowedSites = option.experience.allowedSites.toList(),
                grantLabel = option.grant.label,
                businessUnitId = option.grant.businessUnitId.value,
                roles = option.grant.roles.map { it.value },
                resolvedPermissions = option.context.commerceCapabilities.permissions.map { it.value },
            )
        }

    fun launchResolvedExperience(optionId: String, username: String): SharedSessionSnapshot {
        val option = runtime.resolvedExperienceOptions(username)
            .first { "${it.grant.id}:${it.experience.id.value}" == optionId }
        val appRuntime = runtime.appRuntimeFor(option.experience.id)
        appRuntime.session.start(option.context, username)
        return snapshotMapper.map(option.context, appRuntime.session)
    }

    suspend fun login(appIdName: String, username: String): SharedSessionSnapshot {
        val appRuntime = runtime.appRuntimeFor(ExperienceId.fromExternalName(appIdName))
        val context = appRuntime.session.login(appRuntime.appId, username)
        return snapshotMapper.map(context, appRuntime.session)
    }

    fun logout(appIdName: String) {
        runtime.appRuntimeFor(ExperienceId.fromExternalName(appIdName)).session.logout()
    }
}
