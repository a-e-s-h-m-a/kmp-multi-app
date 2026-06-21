package com.aeshma.multiapp.core.config

import com.aeshma.multiapp.core.model.AppContext
import com.aeshma.multiapp.core.model.AppId

class AppCatalog(definitions: List<AppDefinition>) {
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

        return AppContext(
            appId = definition.id,
            userId = "${definition.configKey}-$normalizedUsername",
            userType = profile.userType,
            capabilities = profile.capabilities,
        )
    }
}
