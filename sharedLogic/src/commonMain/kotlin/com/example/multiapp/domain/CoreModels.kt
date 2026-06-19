package com.example.multiapp.domain

data class AppId(val externalName: String) {
    init {
        require(externalName.isNotBlank()) { "App id cannot be blank." }
    }

    companion object {
        val AppOne = AppId("AppOne")
        val AppTwo = AppId("AppTwo")

        fun fromExternalName(value: String): AppId =
            when {
                AppOne.externalName.equals(value.trim(), ignoreCase = true) -> AppOne
                AppTwo.externalName.equals(value.trim(), ignoreCase = true) -> AppTwo
                else -> AppId(value.trim())
            }
    }
}

enum class UserType {
    Customer,
    Driver,
    Admin,
    Merchant,
}

enum class FeatureId {
    Home,
    Delivery,
    Profile,
    Reports,
    Payments,
}

data class AppContext(
    val appId: AppId,
    val userId: String,
    val userType: UserType,
    val capabilities: UserCapabilities,
)

data class UserCapabilities(
    val delivery: DeliveryCapability?,
    val reports: ReportsCapability?,
    val payments: PaymentsCapability?,
)

data class DeliveryCapability(
    val mode: DeliveryMode,
    val canCreateDelivery: Boolean,
    val canCancelDelivery: Boolean,
    val canEditAddress: Boolean,
    val showLiveTracking: Boolean,
    val canAcceptDelivery: Boolean,
    val canMarkPickedUp: Boolean,
    val canMarkDelivered: Boolean,
)

enum class DeliveryMode {
    Customer,
    Driver,
    Admin,
    Merchant,
    ReadOnly,
}

data class ReportsCapability(
    val canViewReports: Boolean,
    val canViewGlobalReports: Boolean,
    val canViewMerchantReports: Boolean,
)

data class PaymentsCapability(
    val canMakePayment: Boolean,
    val canViewPaymentHistory: Boolean,
)

data class DeliveryOrder(
    val id: String,
    val title: String,
    val status: DeliveryStatus,
)

enum class DeliveryStatus {
    Created,
    Assigned,
    PickedUp,
    Delivered,
    Cancelled,
}

enum class DeliveryAction {
    Create,
    Cancel,
    EditAddress,
    Track,
    Accept,
    MarkPickedUp,
    MarkDelivered,
    ViewOnly,
}
