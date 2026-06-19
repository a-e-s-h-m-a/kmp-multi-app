package com.example.multiapp.domain

data class UserProfile(
    val userType: UserType,
    val capabilities: UserCapabilities,
)

data class AppDefinition(
    val id: AppId,
    val displayName: String,
    val configKey: String,
    val defaultUsername: String,
    val defaultUserType: UserType,
    val profiles: Map<String, UserProfile>,
) {
    init {
        require(defaultUsername in profiles) {
            "Default user '$defaultUsername' is not configured for ${id.externalName}."
        }
    }

    fun profileFor(username: String): UserProfile =
        profiles[username] ?: profiles.getValue(defaultUsername)
}

class AppCatalog(
    definitions: List<AppDefinition>,
) {
    private val definitionsByName = definitions.associateBy { it.id.externalName.lowercase() }

    init {
        require(definitions.isNotEmpty()) { "At least one app must be configured." }
        require(definitionsByName.size == definitions.size) { "App ids must be unique." }
    }

    fun definition(appId: AppId): AppDefinition =
        definitionsByName[appId.externalName.lowercase()]
            ?: error("No app definition registered for ${appId.externalName}.")

    fun contextFor(appId: AppId, rawUsername: String): AppContext {
        val username = rawUsername.normalizedUsername()
        val definition = definition(appId)
        val profile = if (username == NO_DELIVERY_USERNAME) {
            UserProfile(
                userType = definition.defaultUserType,
                capabilities = noCapabilities(),
            )
        } else {
            definition.profileFor(username)
        }

        return AppContext(
            appId = definition.id,
            userId = "${definition.configKey}-$username",
            userType = profile.userType,
            capabilities = profile.capabilities,
        )
    }

    fun configPath(appId: AppId, rawUsername: String): String {
        val username = rawUsername.normalizedUsername()
        return if (username == NO_DELIVERY_USERNAME) {
            "/config/common/$NO_DELIVERY_USERNAME"
        } else {
            "/config/${definition(appId).configKey}/$username"
        }
    }

    companion object {
        private const val NO_DELIVERY_USERNAME = "nod"

        fun default(): AppCatalog =
            AppCatalog(
                definitions = listOf(
                    AppDefinition(
                        id = AppId.AppOne,
                        displayName = "AppOne",
                        configKey = "appOne",
                        defaultUsername = "customer",
                        defaultUserType = UserType.Customer,
                        profiles = mapOf(
                            "customer" to UserProfile(
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
                            ),
                            "driver" to UserProfile(
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
                            ),
                            "readonly" to readOnlyProfile(UserType.Customer),
                            "admin" to disabledProfile(UserType.Customer),
                            "merchant" to disabledProfile(UserType.Customer),
                        ),
                    ),
                    AppDefinition(
                        id = AppId.AppTwo,
                        displayName = "AppTwo",
                        configKey = "appTwo",
                        defaultUsername = "admin",
                        defaultUserType = UserType.Merchant,
                        profiles = mapOf(
                            "admin" to UserProfile(
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
                            ),
                            "merchant" to UserProfile(
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
                            ),
                            "readonly" to readOnlyProfile(UserType.Merchant),
                            "driver" to disabledProfile(UserType.Merchant),
                            "customer" to disabledProfile(UserType.Merchant),
                        ),
                    ),
                ),
            )

        private fun readOnlyProfile(userType: UserType): UserProfile =
            UserProfile(
                userType = userType,
                capabilities = UserCapabilities(
                    delivery = deliveryCapability(mode = DeliveryMode.ReadOnly),
                    reports = null,
                    payments = null,
                ),
            )

        private fun disabledProfile(userType: UserType): UserProfile =
            UserProfile(userType = userType, capabilities = noCapabilities())

        private fun noCapabilities(): UserCapabilities =
            UserCapabilities(delivery = null, reports = null, payments = null)

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

        private fun String.normalizedUsername(): String =
            trim().lowercase().ifBlank { "readonly" }
    }
}
