package com.aeshma.multiapp.application

import com.aeshma.multiapp.core.model.AppId

class IOSAppFacade(appIdName: String) {
    private val runtime = createAppRuntime(AppId.fromExternalName(appIdName))
    private val snapshotMapper = SessionSnapshotMapper()

    val appName: String = runtime.appDefinition.displayName
    val defaultUsername: String = runtime.appDefinition.defaultUsername
    val supportedUsernames: List<String> = runtime.appDefinition.supportedUsernames

    suspend fun login(username: String): SharedSessionSnapshot {
        val context = runtime.session.login(runtime.appId, username)
        return snapshotMapper.map(context, runtime.session)
    }
}
