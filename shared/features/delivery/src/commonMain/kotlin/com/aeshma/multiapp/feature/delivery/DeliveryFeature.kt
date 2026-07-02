package com.aeshma.multiapp.feature.delivery

import com.aeshma.multiapp.core.model.FeatureDefinitionSpec
import com.aeshma.multiapp.core.model.FeatureId
import com.aeshma.multiapp.core.model.FeaturePermissionRow
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
    )
}
