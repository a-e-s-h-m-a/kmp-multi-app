package com.aeshma.multiapp.feature.delivery

import com.aeshma.multiapp.core.model.FeatureDefinitionSpec
import com.aeshma.multiapp.core.model.FeatureId
import com.aeshma.multiapp.core.model.FeaturePermissionRow
import com.aeshma.multiapp.core.model.FeatureUiBlock
import com.aeshma.multiapp.core.model.PermissionId

object DeliveryFeature {
    val definition: FeatureDefinitionSpec = FeatureDefinitionSpec(
        id = FeatureId.Delivery,
        title = "Delivery",
        requiredPermission = PermissionId.DeliveryView,
        tweakPermissions = listOf(
            PermissionId.DeliveryEdit,
            PermissionId.DeliveryProgress,
            PermissionId.DeliveryStatus,
            PermissionId.DeliveryMap,
            PermissionId.DeliveryInvoices,
        ),
        permissionRows = listOf(
            FeaturePermissionRow("Edit delivery", PermissionId.DeliveryEdit),
            FeaturePermissionRow("Progress timeline", PermissionId.DeliveryProgress),
            FeaturePermissionRow("Status updates", PermissionId.DeliveryStatus),
            FeaturePermissionRow("Map tracking", PermissionId.DeliveryMap),
            FeaturePermissionRow("Invoices", PermissionId.DeliveryInvoices),
        ),
        uiBlocks = listOf(
            FeatureUiBlock(
                title = "Delivery editor",
                requiredPermission = PermissionId.DeliveryEdit,
                body = "Shows address, window, and cancellation controls for eligible orders.",
            ),
            FeatureUiBlock(
                title = "Progress timeline",
                requiredPermission = PermissionId.DeliveryProgress,
                body = "Shows created, assigned, picked up, and delivered checkpoints.",
            ),
            FeatureUiBlock(
                title = "Status controls",
                requiredPermission = PermissionId.DeliveryStatus,
                body = "Enables status badges and simulated driver/customer state updates.",
            ),
            FeatureUiBlock(
                title = "Map tracking",
                requiredPermission = PermissionId.DeliveryMap,
                body = "Shows a route summary and current-stop tracking placeholder.",
            ),
            FeatureUiBlock(
                title = "Invoices",
                requiredPermission = PermissionId.DeliveryInvoices,
                body = "Reveals invoice links and proof-of-delivery placeholders.",
            ),
        ),
    )
}
