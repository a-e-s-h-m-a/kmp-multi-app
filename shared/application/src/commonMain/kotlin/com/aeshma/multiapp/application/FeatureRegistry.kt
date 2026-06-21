package com.aeshma.multiapp.application

import com.aeshma.multiapp.core.model.AppContext
import com.aeshma.multiapp.core.model.FeatureId

class FeatureDescriptor(
    val id: FeatureId,
    val title: String,
    private val availability: (AppContext) -> Boolean,
) {
    fun isAvailable(context: AppContext): Boolean = availability(context)
}

class FeatureRegistry {
    private val features = listOf(
        FeatureDescriptor(FeatureId.Home, "Home") { true },
        FeatureDescriptor(FeatureId.Delivery, "Delivery") { it.capabilities.delivery != null },
        FeatureDescriptor(FeatureId.Reports, "Reports") { it.capabilities.reports?.canViewReports == true },
        FeatureDescriptor(FeatureId.Payments, "Payments") { it.capabilities.payments?.canMakePayment == true },
        FeatureDescriptor(FeatureId.Profile, "Profile") { true },
    )

    fun availableFeatures(context: AppContext): List<FeatureDescriptor> =
        features.filter { it.isAvailable(context) }
}
