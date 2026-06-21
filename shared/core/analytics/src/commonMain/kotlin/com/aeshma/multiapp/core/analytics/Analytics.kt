package com.aeshma.multiapp.core.analytics

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
