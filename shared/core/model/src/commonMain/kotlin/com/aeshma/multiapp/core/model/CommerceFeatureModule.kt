package com.aeshma.multiapp.core.model

interface CommerceFeatureModule {
    val definition: FeatureDefinitionSpec
    val runtimeContributor: FeatureRuntimeContributor?
        get() = null
}

interface FeatureRuntimeContributor {
    val featureId: FeatureId

    fun snapshot(context: AppContext): FeatureRuntimeSnapshot
}

data class FeatureRuntimeSnapshot(
    val featureId: FeatureId,
    val title: String,
    val items: List<FeatureRuntimeItem>,
)

data class FeatureRuntimeItem(
    val id: String,
    val title: String,
    val status: String,
    val subtitle: String,
    val actions: List<FeatureRuntimeAction>,
)

data class FeatureRuntimeAction(
    val label: String,
    val result: String,
    val nextStatus: String = "",
)
