package com.aeshma.multiapp.application

import com.aeshma.multiapp.core.model.AppId
import com.aeshma.multiapp.core.model.ProductId

data class SharedAppExperience(
    val id: String,
    val displayName: String,
    val defaultUsername: String,
    val supportedUsernames: List<String>,
)

class IOSProductCompositionRoot(productIdName: String) {
    private val runtime = createProductRuntime(ProductId.fromExternalName(productIdName))
    private val snapshotMapper = SessionSnapshotMapper()

    val productName: String = runtime.productDefinition.displayName
    val showsExperiencePicker: Boolean = runtime.productDefinition.showsExperiencePicker
    val defaultExperienceId: String? = runtime.defaultExperience?.externalName
    val supportedExperiences: List<SharedAppExperience> = runtime.supportedExperiences.map { definition ->
        SharedAppExperience(
            id = definition.id.externalName,
            displayName = definition.displayName,
            defaultUsername = definition.defaultUsername,
            supportedUsernames = definition.supportedUsernames,
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
