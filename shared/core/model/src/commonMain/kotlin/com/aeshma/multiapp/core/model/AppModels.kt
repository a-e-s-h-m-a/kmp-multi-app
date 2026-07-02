package com.aeshma.multiapp.core.model

data class AppId(val externalName: String) {
    init {
        require(externalName.isNotBlank()) { "App id cannot be blank." }
    }

    companion object {
        val AppOne = AppId("AppOne")
        val AppTwo = AppId("AppTwo")

        fun fromExternalName(value: String): AppId = AppId(value.trim())
    }
}

data class ProductId(val externalName: String) {
    init {
        require(externalName.isNotBlank()) { "Product id cannot be blank." }
    }

    companion object {
        val AppOneStandalone = ProductId("AppOneStandalone")
        val AppTwoStandalone = ProductId("AppTwoStandalone")
        val SuperApp = ProductId("SuperApp")

        fun fromExternalName(value: String): ProductId = ProductId(value.trim())
    }
}

data class ExperienceId(val value: String) {
    init {
        require(value.isNotBlank()) { "Experience id cannot be blank." }
    }

    companion object {
        val NewportBuckhead = ExperienceId("newport-buckhead")
        val Shop = ExperienceId("shop")

        fun fromExternalName(value: String): ExperienceId = ExperienceId(value.trim().lowercase())
    }
}

enum class UserType {
    Customer,
    Driver,
    Admin,
    Merchant,
}

data class AppContext(
    val appId: AppId,
    val businessUnitId: BusinessUnitId,
    val userId: String,
    val userType: UserType,
    val roles: Set<RoleId>,
    val explicitPermissions: Set<PermissionId>,
    val supportedFeatures: Set<FeatureId>,
    val commerceCapabilities: CommerceCapabilities,
    val capabilities: UserCapabilities,
)
