package com.aeshma.multiapp.feature.orders

import com.aeshma.multiapp.core.model.FeatureActionDefinition
import com.aeshma.multiapp.core.model.CommerceFeatureModule
import com.aeshma.multiapp.core.model.FeatureDefinitionSpec
import com.aeshma.multiapp.core.model.FeatureId
import com.aeshma.multiapp.core.model.FeaturePermissionRow
import com.aeshma.multiapp.core.model.FeatureUiBlock
import com.aeshma.multiapp.core.model.PermissionId

object OrdersFeature : CommerceFeatureModule {
    override val definition: FeatureDefinitionSpec = FeatureDefinitionSpec(
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
        uiBlocks = listOf(
            FeatureUiBlock(
                title = "Recent orders",
                requiredPermission = PermissionId.OrdersView,
                body = "Shows open, submitted, and delivered orders for the resolved customer context.",
            ),
            FeatureUiBlock(
                title = "Bulk order editor",
                requiredPermission = PermissionId.OrdersEdit,
                body = "Enables simulated quantity and delivery-window edits directly from the order list.",
            ),
            FeatureUiBlock(
                title = "Notification center",
                requiredPermission = PermissionId.OrdersNotifications,
                body = "Displays alert preferences and queued order status notifications.",
            ),
        ),
    )
}
