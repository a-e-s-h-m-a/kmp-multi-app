package com.aeshma.multiapp.core.config

import com.aeshma.multiapp.core.model.AppContext
import com.aeshma.multiapp.core.model.AppId

interface AuthRepository {
    suspend fun login(appId: AppId, username: String): AppContext
}

class LocalAuthRepository(
    private val appCatalog: AppCatalog,
) : AuthRepository {
    override suspend fun login(appId: AppId, username: String): AppContext =
        appCatalog.contextFor(appId, username)
}
