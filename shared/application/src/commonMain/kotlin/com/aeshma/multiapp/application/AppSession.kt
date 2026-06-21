package com.aeshma.multiapp.application

import com.aeshma.multiapp.core.analytics.AnalyticsClient
import com.aeshma.multiapp.core.analytics.AnalyticsEvent
import com.aeshma.multiapp.core.config.AuthRepository
import com.aeshma.multiapp.core.model.AppContext
import com.aeshma.multiapp.core.model.AppId
import com.aeshma.multiapp.core.model.FeatureId
import com.aeshma.multiapp.feature.delivery.DeliveryOrder
import com.aeshma.multiapp.feature.delivery.DeliveryPolicy
import com.aeshma.multiapp.feature.delivery.DeliveryPolicyResolver
import com.aeshma.multiapp.feature.delivery.DeliveryRepository

class SessionNotStartedException : IllegalStateException("Call login before requesting session data.")

class AppSession(
    private val authRepository: AuthRepository,
    private val featureRegistry: FeatureRegistry,
    private val deliveryPolicyResolver: DeliveryPolicyResolver,
    private val deliveryRepository: DeliveryRepository,
    private val analyticsClient: AnalyticsClient,
) {
    var currentContext: AppContext? = null
        private set

    suspend fun login(appId: AppId, username: String): AppContext {
        val context = authRepository.login(appId, username)
        currentContext = context
        analyticsClient.track(
            AnalyticsEvent(
                name = "login_completed",
                properties = context.analyticsProperties() + ("username" to username),
            ),
        )
        return context
    }

    fun logout() {
        val context = currentContext ?: return
        analyticsClient.track(
            AnalyticsEvent(
                name = "logout_completed",
                properties = context.analyticsProperties(),
            ),
        )
        currentContext = null
    }

    fun availableFeatures(): List<FeatureDescriptor> =
        featureRegistry.availableFeatures(requireContext())

    fun deliveryPolicy(): DeliveryPolicy =
        deliveryPolicyResolver.resolve(requireContext())

    fun deliveryOrders(): List<DeliveryOrder> = deliveryRepository.orders()

    fun trackFeatureOpened(featureId: FeatureId) {
        analyticsClient.track(
            AnalyticsEvent(
                name = "${featureId.value}_screen_opened",
                properties = requireContext().analyticsProperties() + ("featureId" to featureId.value),
            ),
        )
    }

    private fun requireContext(): AppContext = currentContext ?: throw SessionNotStartedException()

    private fun AppContext.analyticsProperties(): Map<String, String> = mapOf(
        "appId" to appId.externalName,
        "userType" to userType.name,
    )
}
