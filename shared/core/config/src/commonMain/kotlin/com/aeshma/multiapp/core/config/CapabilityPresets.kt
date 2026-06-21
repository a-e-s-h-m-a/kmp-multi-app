package com.aeshma.multiapp.core.config

import com.aeshma.multiapp.core.model.DeliveryCapability
import com.aeshma.multiapp.core.model.DeliveryMode
import com.aeshma.multiapp.core.model.PaymentsCapability
import com.aeshma.multiapp.core.model.ReportsCapability
import com.aeshma.multiapp.core.model.UserCapabilities

object CapabilityPresets {
    fun customer(): UserCapabilities = UserCapabilities(
        delivery = DeliveryCapability(
            mode = DeliveryMode.Customer,
            canCreateDelivery = true,
            canCancelDelivery = true,
            canEditAddress = true,
            showLiveTracking = true,
        ),
        payments = PaymentsCapability(
            canMakePayment = true,
            canViewPaymentHistory = true,
        ),
    )

    fun driver(): UserCapabilities = UserCapabilities(
        delivery = DeliveryCapability(
            mode = DeliveryMode.Driver,
            showLiveTracking = true,
            canAcceptDelivery = true,
            canMarkPickedUp = true,
            canMarkDelivered = true,
        ),
    )

    fun admin(): UserCapabilities = UserCapabilities(
        delivery = DeliveryCapability(
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
    )

    fun merchant(): UserCapabilities = UserCapabilities(
        delivery = DeliveryCapability(
            mode = DeliveryMode.Merchant,
            showLiveTracking = true,
        ),
        reports = ReportsCapability(
            canViewReports = true,
            canViewGlobalReports = false,
            canViewMerchantReports = true,
        ),
    )

    fun readOnly(): UserCapabilities = UserCapabilities(
        delivery = DeliveryCapability(mode = DeliveryMode.ReadOnly),
    )

    fun none(): UserCapabilities = UserCapabilities.none()
}
