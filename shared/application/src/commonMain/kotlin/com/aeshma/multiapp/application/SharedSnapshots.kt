package com.aeshma.multiapp.application

import com.aeshma.multiapp.core.model.AppContext
import com.aeshma.multiapp.core.model.FeatureId

data class SharedFeatureSnapshot(
    val id: String,
    val title: String,
    val requiredPermission: String,
    val enabledTweaks: List<String>,
    val permissionRows: List<SharedFeaturePermissionRowSnapshot>,
    val actions: List<SharedFeatureActionSnapshot>,
    val uiBlocks: List<SharedFeatureUiBlockSnapshot>,
)

data class SharedFeaturePermissionRowSnapshot(
    val label: String,
    val permission: String,
    val enabled: Boolean,
)

data class SharedFeatureActionSnapshot(
    val label: String,
    val requiredPermission: String,
    val result: String,
)

data class SharedFeatureUiBlockSnapshot(
    val title: String,
    val requiredPermission: String,
    val body: String,
)

data class SharedDeliveryOrderSnapshot(
    val id: String,
    val title: String,
    val status: String,
    val actions: List<String>,
)

data class SharedSessionSnapshot(
    val userSummary: String,
    val availableFeatures: List<SharedFeatureSnapshot>,
    val deliveryExperienceName: String,
    val deliveryOrders: List<SharedDeliveryOrderSnapshot>,
)

class SessionSnapshotMapper {
    fun map(context: AppContext, session: AppSession): SharedSessionSnapshot {
        val deliveryRuntime = session.featureRuntimeSnapshot(FeatureId.Delivery)
        return SharedSessionSnapshot(
            userSummary = "${context.userId} (${context.userType.name})",
            availableFeatures = session.availableFeatures().map { feature ->
                SharedFeatureSnapshot(
                    id = feature.id.value,
                    title = feature.title,
                    requiredPermission = feature.requiredPermission.value,
                    enabledTweaks = feature.enabledTweaks(context).map { it.value },
                    permissionRows = feature.permissionRows.map { row ->
                        SharedFeaturePermissionRowSnapshot(
                            label = row.label,
                            permission = row.permission.value,
                            enabled = context.commerceCapabilities.has(row.permission),
                        )
                    },
                    actions = feature.actions
                        .filter { context.commerceCapabilities.has(it.requiredPermission) }
                        .map { action ->
                            SharedFeatureActionSnapshot(
                                label = action.label,
                                requiredPermission = action.requiredPermission.value,
                                result = action.result,
                            )
                        },
                    uiBlocks = feature.uiBlocks
                        .filter { context.commerceCapabilities.has(it.requiredPermission) }
                        .map { block ->
                            SharedFeatureUiBlockSnapshot(
                                title = block.title,
                                requiredPermission = block.requiredPermission.value,
                                body = block.body,
                            )
                        },
                )
            },
            deliveryExperienceName = deliveryRuntime?.title ?: "Delivery Disabled",
            deliveryOrders = deliveryRuntime?.items.orEmpty().map { order ->
                SharedDeliveryOrderSnapshot(
                    id = order.id,
                    title = order.title,
                    status = order.status,
                    actions = order.actions.map { it.label },
                )
            },
        )
    }
}
