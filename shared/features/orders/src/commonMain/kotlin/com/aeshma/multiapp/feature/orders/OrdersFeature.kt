package com.aeshma.multiapp.feature.orders

import com.aeshma.multiapp.core.model.FeatureActionDefinition
import com.aeshma.multiapp.core.model.FeatureDefinitionSpec
import com.aeshma.multiapp.core.model.FeatureId
import com.aeshma.multiapp.core.model.FeaturePermissionRow
import com.aeshma.multiapp.core.model.PermissionId

object OrdersFeature {
    val definition: FeatureDefinitionSpec = FeatureDefinitionSpec(
        id = FeatureId.Orders,
        title = "Orders",
        requiredPermission = PermissionId.OrdersView,
        tweakPermissions = listOf(
            PermissionId.OrdersEdit,
            PermissionId.OrdersNotifications,
        ),
        permissionRows = listOf(
            FeaturePermissionRow("View orders", PermissionId.OrdersView),
            FeaturePermissionRow("Edit orders", PermissionId.OrdersEdit),
            FeaturePermissionRow("Order notifications", PermissionId.OrdersNotifications),
        ),
        actions = listOf(
            FeatureActionDefinition("Refresh", PermissionId.OrdersView, "Order list refreshed from simulated state."),
            FeatureActionDefinition("Edit", PermissionId.OrdersEdit, "Order edit command accepted."),
            FeatureActionDefinition("Notify", PermissionId.OrdersNotifications, "Notification queued for selected orders."),
        ),
    )
}
