package com.aeshma.multiapp.application

@Deprecated(
    message = "Use IOSAppCompositionRoot.",
    replaceWith = ReplaceWith("IOSAppCompositionRoot(appIdName)"),
)
class IOSAppFacade(appIdName: String) {
    private val compositionRoot = IOSAppCompositionRoot(appIdName)

    val appName: String = compositionRoot.appName
    val defaultUsername: String = compositionRoot.defaultUsername
    val supportedUsernames: List<String> = compositionRoot.supportedUsernames

    suspend fun login(username: String): SharedSessionSnapshot =
        compositionRoot.login(username)

    fun logout() {
        compositionRoot.logout()
    }
}
