package com.aeshma.multiapp.application

import com.aeshma.multiapp.core.model.AppContext

data class SharedFeatureSnapshot(
    val id: String,
    val title: String,
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
        val policy = session.deliveryPolicy()
        return SharedSessionSnapshot(
            userSummary = "${context.userId} (${context.userType.name})",
            availableFeatures = session.availableFeatures().map { feature ->
                SharedFeatureSnapshot(
                    id = feature.id.value,
                    title = feature.title,
                )
            },
            deliveryExperienceName = policy.experienceName,
            deliveryOrders = session.deliveryOrders().map { order ->
                SharedDeliveryOrderSnapshot(
                    id = order.id,
                    title = order.title,
                    status = order.status.name,
                    actions = policy.availableActions(order).map { it.name },
                )
            },
        )
    }
}
