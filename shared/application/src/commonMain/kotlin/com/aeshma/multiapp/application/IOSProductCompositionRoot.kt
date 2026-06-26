package com.aeshma.multiapp.application

import com.aeshma.multiapp.core.model.AppId
import com.aeshma.multiapp.core.model.BusinessUnitId
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

class IOSProductCompositionRoot(productIdName: String) {
    private val runtime = createProductRuntime(ProductId.fromExternalName(productIdName))
    private val snapshotMapper = SessionSnapshotMapper()

    val productName: String = runtime.productDefinition.displayName
    val showsExperiencePicker: Boolean = runtime.productDefinition.showsExperiencePicker
    val defaultExperienceId: String? = runtime.defaultExperience?.externalName
    val businessUnits: List<SharedBusinessUnit> = runtime.businessUnits.map { definition ->
        SharedBusinessUnit(id = definition.id.value)
    }
    val supportedExperiences: List<SharedAppExperience> = runtime.supportedExperienceDefinitions.map { definition ->
        val appDefinition = runtime.appRuntimeFor(definition.appId).appDefinition
        SharedAppExperience(
            id = definition.appId.externalName,
            displayName = definition.displayName,
            theme = definition.theme,
            allowedSites = definition.allowedSites.toList(),
            defaultUsername = appDefinition.defaultUsername,
            supportedUsernames = appDefinition.supportedUsernames,
        )
    }

    fun supportedExperiencesForBusinessUnit(businessUnitIdName: String): List<SharedAppExperience> =
        runtime.allowedExperiencesFor(BusinessUnitId.fromExternalName(businessUnitIdName)).map { definition ->
            val appDefinition = runtime.appRuntimeFor(definition.appId).appDefinition
            SharedAppExperience(
                id = definition.appId.externalName,
                displayName = definition.displayName,
                theme = definition.theme,
                allowedSites = definition.allowedSites.toList(),
                defaultUsername = appDefinition.defaultUsername,
                supportedUsernames = appDefinition.supportedUsernames,
            )
        }

    suspend fun login(appIdName: String, username: String): SharedSessionSnapshot {
        val appRuntime = runtime.appRuntimeFor(AppId.fromExternalName(appIdName))
        val context = appRuntime.session.login(appRuntime.appId, username)
        return snapshotMapper.map(context, appRuntime.session)
    }

    fun logout(appIdName: String) {
        runtime.appRuntimeFor(AppId.fromExternalName(appIdName)).session.logout()
    }
}
