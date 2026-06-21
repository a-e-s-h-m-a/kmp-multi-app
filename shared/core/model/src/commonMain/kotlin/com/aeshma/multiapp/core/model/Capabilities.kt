package com.aeshma.multiapp.core.model

data class UserCapabilities(
    val delivery: DeliveryCapability? = null,
    val reports: ReportsCapability? = null,
    val payments: PaymentsCapability? = null,
) {
    companion object {
        fun none(): UserCapabilities = UserCapabilities()
    }
}

data class DeliveryCapability(
    val mode: DeliveryMode,
    val canCreateDelivery: Boolean = false,
    val canCancelDelivery: Boolean = false,
    val canEditAddress: Boolean = false,
    val showLiveTracking: Boolean = false,
    val canAcceptDelivery: Boolean = false,
    val canMarkPickedUp: Boolean = false,
    val canMarkDelivered: Boolean = false,
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
