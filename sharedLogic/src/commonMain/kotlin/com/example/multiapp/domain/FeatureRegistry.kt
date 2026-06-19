package com.example.multiapp.domain

interface FeatureDescriptor {
    val id: FeatureId
    val title: String
    fun isAvailable(context: AppContext): Boolean
}

class FeatureRegistry(
    private val features: List<FeatureDescriptor>,
) {
    fun availableFeatures(context: AppContext): List<FeatureDescriptor> =
        features.filter { it.isAvailable(context) }

    companion object {
        fun default(): FeatureRegistry =
            FeatureRegistry(
                listOf(
                    HomeFeatureDescriptor,
                    DeliveryFeatureDescriptor,
                    ReportsFeatureDescriptor,
                    PaymentsFeatureDescriptor,
                    ProfileFeatureDescriptor,
                ),
            )
    }
}

data object HomeFeatureDescriptor : FeatureDescriptor {
    override val id: FeatureId = FeatureId.Home
    override val title: String = "Home"

    override fun isAvailable(context: AppContext): Boolean = true
}

data object DeliveryFeatureDescriptor : FeatureDescriptor {
    override val id: FeatureId = FeatureId.Delivery
    override val title: String = "Delivery"

    override fun isAvailable(context: AppContext): Boolean =
        context.capabilities.delivery != null
}

data object ProfileFeatureDescriptor : FeatureDescriptor {
    override val id: FeatureId = FeatureId.Profile
    override val title: String = "Profile"

    override fun isAvailable(context: AppContext): Boolean = true
}

data object ReportsFeatureDescriptor : FeatureDescriptor {
    override val id: FeatureId = FeatureId.Reports
    override val title: String = "Reports"

    override fun isAvailable(context: AppContext): Boolean =
        context.capabilities.reports?.canViewReports == true
}

data object PaymentsFeatureDescriptor : FeatureDescriptor {
    override val id: FeatureId = FeatureId.Payments
    override val title: String = "Payments"

    override fun isAvailable(context: AppContext): Boolean =
        context.capabilities.payments?.canMakePayment == true
}
