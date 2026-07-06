package com.aeshma.multiapp.application

import com.aeshma.multiapp.core.analytics.AnalyticsClient
import com.aeshma.multiapp.core.analytics.AnalyticsEvent
import com.aeshma.multiapp.core.config.AuthRepository
import com.aeshma.multiapp.core.model.AppContext
import com.aeshma.multiapp.core.model.AppId
import com.aeshma.multiapp.core.model.FeatureId
import com.aeshma.multiapp.core.model.FeatureRuntimeContributor
import com.aeshma.multiapp.core.model.FeatureRuntimeSnapshot

class SessionNotStartedException : IllegalStateException("Call login before requesting session data.")

class AppSession(
    private val authRepository: AuthRepository,
    private val featureRegistry: FeatureRegistry,
    private val featureRuntimeContributors: List<FeatureRuntimeContributor>,
    private val analyticsClient: AnalyticsClient,
) {
    var currentContext: AppContext? = null
        private set

    suspend fun login(appId: AppId, username: String): AppContext {
        val context = authRepository.login(appId, username)
        start(context, username)
        return context
    }

    fun start(context: AppContext, username: String) {
        currentContext = context
        analyticsClient.track(
            AnalyticsEvent(
                name = "login_completed",
                properties = context.analyticsProperties() + ("username" to username),
            ),
        )
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

    fun featureRuntimeSnapshots(): List<FeatureRuntimeSnapshot> {
        val context = requireContext()
        val availableFeatureIds = availableFeatures().map { it.id }.toSet()
        return featureRuntimeContributors
            .filter { it.featureId in availableFeatureIds }
            .map { it.snapshot(context) }
    }

    fun featureRuntimeSnapshot(featureId: FeatureId): FeatureRuntimeSnapshot? =
        featureRuntimeSnapshots().firstOrNull { it.featureId == featureId }

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
