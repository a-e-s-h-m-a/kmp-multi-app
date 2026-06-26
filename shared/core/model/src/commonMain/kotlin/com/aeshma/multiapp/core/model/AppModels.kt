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

enum class UserType {
    Customer,
    Driver,
    Admin,
    Merchant,
}

data class AppContext(
    val appId: AppId,
    val userId: String,
    val userType: UserType,
    val capabilities: UserCapabilities,
)
