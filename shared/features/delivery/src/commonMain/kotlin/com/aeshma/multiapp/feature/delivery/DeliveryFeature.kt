package com.aeshma.multiapp.feature.delivery

import com.aeshma.multiapp.core.model.CommerceFeatureModule
import com.aeshma.multiapp.core.model.AppContext
import com.aeshma.multiapp.core.model.FeatureDefinitionSpec
import com.aeshma.multiapp.core.model.FeatureId
import com.aeshma.multiapp.core.model.FeaturePermissionRow
import com.aeshma.multiapp.core.model.FeatureRuntimeAction
import com.aeshma.multiapp.core.model.FeatureRuntimeContributor
import com.aeshma.multiapp.core.model.FeatureRuntimeItem
import com.aeshma.multiapp.core.model.FeatureRuntimeSnapshot
import com.aeshma.multiapp.core.model.FeatureUiBlock
import com.aeshma.multiapp.core.model.PermissionId

object DeliveryFeature : CommerceFeatureModule {
    private val policyResolver = DeliveryPolicyResolver()
    private val repository = SampleDeliveryRepository()

    override val definition: FeatureDefinitionSpec = FeatureDefinitionSpec(
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

    override val runtimeContributor: FeatureRuntimeContributor = object : FeatureRuntimeContributor {
        override val featureId: FeatureId = FeatureId.Delivery

        override fun snapshot(context: AppContext): FeatureRuntimeSnapshot {
            val policy = policyResolver.resolve(context)
            return FeatureRuntimeSnapshot(
                featureId = FeatureId.Delivery,
                title = policy.experienceName,
                items = repository.orders().map { order ->
                    FeatureRuntimeItem(
                        id = order.id,
                        title = order.title,
                        status = order.status.name,
                        subtitle = "${order.id} / ${order.status.name}",
                        actions = policy.availableActions(order).map { action ->
                            FeatureRuntimeAction(
                                label = action.name,
                                result = "${action.name} applied to ${order.id}; status is now ${action.nextStatus(order.status).name}.",
                                nextStatus = action.nextStatus(order.status).name,
                            )
                        },
                    )
                },
            )
        }
    }

    private fun DeliveryAction.nextStatus(currentStatus: DeliveryStatus): DeliveryStatus =
        when (this) {
            DeliveryAction.Cancel -> DeliveryStatus.Cancelled
            DeliveryAction.Accept -> DeliveryStatus.Assigned
            DeliveryAction.MarkPickedUp -> DeliveryStatus.PickedUp
            DeliveryAction.MarkDelivered -> DeliveryStatus.Delivered
            else -> currentStatus
        }
}
