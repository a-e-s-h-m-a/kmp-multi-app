package com.aeshma.multiapp.application

import com.aeshma.multiapp.core.config.ExperienceCatalog
import com.aeshma.multiapp.core.config.defaultExperienceDefinitions
import com.aeshma.multiapp.core.model.AppId

class IOSAppCompositionRoot(appIdName: String) {
    private val runtime = createAppRuntime(
        appId = AppId.fromExternalName(appIdName),
        featureDefinitions = platformFeatureDefinitions(),
        featureRuntimeContributors = platformFeatureRuntimeContributors(),
    )
    private val experienceDefinition = ExperienceCatalog(defaultExperienceDefinitions())
        .definitions()
        .first { it.hostAppId == runtime.appId }
    private val snapshotMapper = SessionSnapshotMapper()

    val appName: String = experienceDefinition.displayName
    val defaultUsername: String = runtime.appDefinition.defaultUsername
    val supportedUsernames: List<String> = runtime.appDefinition.supportedUsernames

    suspend fun login(username: String): SharedSessionSnapshot {
        val context = runtime.session.login(runtime.appId, username)
        return snapshotMapper.map(context, runtime.session)
    }

    fun logout() {
        runtime.session.logout()
    }
}
