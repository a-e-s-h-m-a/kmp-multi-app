package com.example.multiapp.domain

interface AuthRepository {
    suspend fun login(appId: AppId, username: String): AppContext
}

class FakeAuthRepository(
    private val networkClient: NetworkClient,
    private val appCatalog: AppCatalog = AppCatalog.default(),
) : AuthRepository {
    override suspend fun login(appId: AppId, username: String): AppContext {
        networkClient.get(appCatalog.configPath(appId, username))
        return contextFor(appId, username)
    }

    fun contextFor(appId: AppId, username: String): AppContext =
        appCatalog.contextFor(appId, username)
}
