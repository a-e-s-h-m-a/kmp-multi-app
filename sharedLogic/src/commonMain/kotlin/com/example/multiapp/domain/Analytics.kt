package com.example.multiapp.domain

data class AnalyticsEvent(
    val name: String,
    val properties: Map<String, String> = emptyMap(),
)

interface AnalyticsClient {
    fun track(event: AnalyticsEvent)
}

class ConsoleAnalyticsClient : AnalyticsClient {
    private val events = mutableListOf<AnalyticsEvent>()

    override fun track(event: AnalyticsEvent) {
        events += event
    }

    fun trackedEvents(): List<AnalyticsEvent> = events.toList()
}

class AppAwareAnalyticsClient(
    private val delegate: AnalyticsClient,
    private val contextProvider: () -> AppContext?,
) : AnalyticsClient {
    override fun track(event: AnalyticsEvent) {
        val context = contextProvider()
        val baseProperties = buildMap {
            context?.let {
                put("appId", it.appId.name)
                put("userType", it.userType.name)
            }
        }

        delegate.track(
            event.copy(properties = baseProperties + event.properties),
        )
    }

    fun trackFeature(featureId: FeatureId, name: String, properties: Map<String, String> = emptyMap()) {
        track(
            AnalyticsEvent(
                name = name,
                properties = mapOf("featureId" to featureId.name) + properties,
            ),
        )
    }
}
