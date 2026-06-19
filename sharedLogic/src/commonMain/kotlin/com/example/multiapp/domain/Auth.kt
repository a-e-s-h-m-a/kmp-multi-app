package com.example.multiapp.domain

interface AuthRepository {
    suspend fun login(appId: AppId, username: String): AppContext
}

class FakeAuthRepository(
    private val networkClient: NetworkClient,
) : AuthRepository {
    override suspend fun login(appId: AppId, username: String): AppContext {
        val normalizedUsername = username.normalizedUsername()
        val path = configPath(appId, normalizedUsername)
        networkClient.get(path)
        return contextFor(appId, normalizedUsername)
    }

    fun contextFor(appId: AppId, username: String): AppContext {
        val normalizedUsername = username.normalizedUsername()
        return when {
            normalizedUsername == "nod" -> appContext(
                appId = appId,
                username = normalizedUsername,
                userType = defaultUserType(appId),
                capabilities = UserCapabilities(delivery = null, reports = null, payments = null),
            )
            appId == AppId.AppOne -> appOneContext(normalizedUsername)
            appId == AppId.AppTwo -> appTwoContext(normalizedUsername)
            else -> appContext(
                appId = appId,
                username = normalizedUsername,
                userType = defaultUserType(appId),
                capabilities = UserCapabilities(delivery = null, reports = null, payments = null),
            )
        }
    }

    private fun appOneContext(username: String): AppContext =
        when (username) {
            "driver" -> appContext(
                appId = AppId.AppOne,
                username = username,
                userType = UserType.Driver,
                capabilities = UserCapabilities(
                    delivery = deliveryCapability(
                        mode = DeliveryMode.Driver,
                        showLiveTracking = true,
                        canAcceptDelivery = true,
                        canMarkPickedUp = true,
                        canMarkDelivered = true,
                    ),
                    reports = null,
                    payments = null,
                ),
            )
            "readonly" -> readOnlyContext(AppId.AppOne, username, UserType.Customer)
            "admin",
            "merchant" -> appContext(
                appId = AppId.AppOne,
                username = username,
                userType = UserType.Customer,
                capabilities = UserCapabilities(delivery = null, reports = null, payments = null),
            )
            else -> appContext(
                appId = AppId.AppOne,
                username = username,
                userType = UserType.Customer,
                capabilities = UserCapabilities(
                    delivery = deliveryCapability(
                        mode = DeliveryMode.Customer,
                        canCreateDelivery = true,
                        canCancelDelivery = true,
                        canEditAddress = true,
                        showLiveTracking = true,
                    ),
                    reports = null,
                    payments = PaymentsCapability(
                        canMakePayment = true,
                        canViewPaymentHistory = true,
                    ),
                ),
            )
        }

    private fun appTwoContext(username: String): AppContext =
        when (username) {
            "admin" -> appContext(
                appId = AppId.AppTwo,
                username = username,
                userType = UserType.Admin,
                capabilities = UserCapabilities(
                    delivery = deliveryCapability(
                        mode = DeliveryMode.Admin,
                        canCancelDelivery = true,
                        canEditAddress = true,
                        showLiveTracking = true,
                    ),
                    reports = ReportsCapability(
                        canViewReports = true,
                        canViewGlobalReports = true,
                        canViewMerchantReports = true,
                    ),
                    payments = null,
                ),
            )
            "merchant" -> appContext(
                appId = AppId.AppTwo,
                username = username,
                userType = UserType.Merchant,
                capabilities = UserCapabilities(
                    delivery = deliveryCapability(
                        mode = DeliveryMode.Merchant,
                        showLiveTracking = true,
                    ),
                    reports = ReportsCapability(
                        canViewReports = true,
                        canViewGlobalReports = false,
                        canViewMerchantReports = true,
                    ),
                    payments = null,
                ),
            )
            "readonly" -> readOnlyContext(AppId.AppTwo, username, UserType.Merchant)
            "driver",
            "customer" -> appContext(
                appId = AppId.AppTwo,
                username = username,
                userType = UserType.Merchant,
                capabilities = UserCapabilities(delivery = null, reports = null, payments = null),
            )
            else -> appTwoContext("admin")
        }

    private fun readOnlyContext(appId: AppId, username: String, userType: UserType): AppContext =
        appContext(
            appId = appId,
            username = username,
            userType = userType,
            capabilities = UserCapabilities(
                delivery = deliveryCapability(mode = DeliveryMode.ReadOnly),
                reports = null,
                payments = null,
            ),
        )

    private fun appContext(
        appId: AppId,
        username: String,
        userType: UserType,
        capabilities: UserCapabilities,
    ): AppContext =
        AppContext(
            appId = appId,
            userId = "${appId.name.lowercase()}-$username",
            userType = userType,
            capabilities = capabilities,
        )

    private fun deliveryCapability(
        mode: DeliveryMode,
        canCreateDelivery: Boolean = false,
        canCancelDelivery: Boolean = false,
        canEditAddress: Boolean = false,
        showLiveTracking: Boolean = false,
        canAcceptDelivery: Boolean = false,
        canMarkPickedUp: Boolean = false,
        canMarkDelivered: Boolean = false,
    ): DeliveryCapability =
        DeliveryCapability(
            mode = mode,
            canCreateDelivery = canCreateDelivery,
            canCancelDelivery = canCancelDelivery,
            canEditAddress = canEditAddress,
            showLiveTracking = showLiveTracking,
            canAcceptDelivery = canAcceptDelivery,
            canMarkPickedUp = canMarkPickedUp,
            canMarkDelivered = canMarkDelivered,
        )

    private fun configPath(appId: AppId, username: String): String =
        if (username == "nod") {
            "/config/common/nod"
        } else {
            "/config/${appId.name.replaceFirstChar { it.lowercase() }}/$username"
        }

    private fun defaultUserType(appId: AppId): UserType =
        when (appId) {
            AppId.AppOne -> UserType.Customer
            AppId.AppTwo -> UserType.Merchant
        }

    private fun String.normalizedUsername(): String =
        trim().lowercase().ifBlank { "readonly" }
}
