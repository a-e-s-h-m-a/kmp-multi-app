package com.example.multiapp.domain

interface DeliveryPolicy {
    val experienceName: String
    fun availableActions(order: DeliveryOrder): List<DeliveryAction>
    fun canOpenDeliveryDetails(order: DeliveryOrder): Boolean
}

class CustomerDeliveryPolicy(
    private val capability: DeliveryCapability,
) : DeliveryPolicy {
    override val experienceName: String = "Customer Delivery"

    override fun availableActions(order: DeliveryOrder): List<DeliveryAction> =
        when (order.status) {
            DeliveryStatus.Created -> buildList {
                if (capability.canCancelDelivery) add(DeliveryAction.Cancel)
                if (capability.canEditAddress) add(DeliveryAction.EditAddress)
                if (capability.showLiveTracking) add(DeliveryAction.Track)
            }
            DeliveryStatus.Assigned,
            DeliveryStatus.PickedUp -> trackingOrViewOnly()
            DeliveryStatus.Delivered,
            DeliveryStatus.Cancelled -> listOf(DeliveryAction.ViewOnly)
        }

    override fun canOpenDeliveryDetails(order: DeliveryOrder): Boolean = true

    private fun trackingOrViewOnly(): List<DeliveryAction> =
        if (capability.showLiveTracking) listOf(DeliveryAction.Track) else listOf(DeliveryAction.ViewOnly)
}

class DriverDeliveryPolicy(
    private val capability: DeliveryCapability,
) : DeliveryPolicy {
    override val experienceName: String = "Driver Delivery"

    override fun availableActions(order: DeliveryOrder): List<DeliveryAction> =
        when (order.status) {
            DeliveryStatus.Created -> if (capability.canAcceptDelivery) {
                listOf(DeliveryAction.Accept)
            } else {
                listOf(DeliveryAction.ViewOnly)
            }
            DeliveryStatus.Assigned -> buildList {
                if (capability.canMarkPickedUp) add(DeliveryAction.MarkPickedUp)
                if (capability.showLiveTracking) add(DeliveryAction.Track)
                if (isEmpty()) add(DeliveryAction.ViewOnly)
            }
            DeliveryStatus.PickedUp -> buildList {
                if (capability.canMarkDelivered) add(DeliveryAction.MarkDelivered)
                if (capability.showLiveTracking) add(DeliveryAction.Track)
                if (isEmpty()) add(DeliveryAction.ViewOnly)
            }
            DeliveryStatus.Delivered,
            DeliveryStatus.Cancelled -> listOf(DeliveryAction.ViewOnly)
        }

    override fun canOpenDeliveryDetails(order: DeliveryOrder): Boolean = true
}

class AdminDeliveryPolicy(
    private val capability: DeliveryCapability,
) : DeliveryPolicy {
    override val experienceName: String = "Admin Delivery"

    override fun availableActions(order: DeliveryOrder): List<DeliveryAction> =
        when (order.status) {
            DeliveryStatus.Created,
            DeliveryStatus.Assigned -> buildList {
                if (capability.canCancelDelivery) add(DeliveryAction.Cancel)
                if (capability.canEditAddress) add(DeliveryAction.EditAddress)
                if (capability.showLiveTracking) add(DeliveryAction.Track)
                if (isEmpty()) add(DeliveryAction.ViewOnly)
            }
            DeliveryStatus.PickedUp -> if (capability.showLiveTracking) {
                listOf(DeliveryAction.Track)
            } else {
                listOf(DeliveryAction.ViewOnly)
            }
            DeliveryStatus.Delivered,
            DeliveryStatus.Cancelled -> listOf(DeliveryAction.ViewOnly)
        }

    override fun canOpenDeliveryDetails(order: DeliveryOrder): Boolean = true
}

class MerchantDeliveryPolicy(
    private val capability: DeliveryCapability,
) : DeliveryPolicy {
    override val experienceName: String = "Merchant Delivery"

    override fun availableActions(order: DeliveryOrder): List<DeliveryAction> =
        if (capability.showLiveTracking && order.status != DeliveryStatus.Delivered && order.status != DeliveryStatus.Cancelled) {
            listOf(DeliveryAction.Track)
        } else {
            listOf(DeliveryAction.ViewOnly)
        }

    override fun canOpenDeliveryDetails(order: DeliveryOrder): Boolean = true
}

data object ReadOnlyDeliveryPolicy : DeliveryPolicy {
    override val experienceName: String = "Read Only Delivery"

    override fun availableActions(order: DeliveryOrder): List<DeliveryAction> =
        listOf(DeliveryAction.ViewOnly)

    override fun canOpenDeliveryDetails(order: DeliveryOrder): Boolean = true
}

data object DisabledDeliveryPolicy : DeliveryPolicy {
    override val experienceName: String = "Delivery Disabled"

    override fun availableActions(order: DeliveryOrder): List<DeliveryAction> =
        emptyList()

    override fun canOpenDeliveryDetails(order: DeliveryOrder): Boolean = false
}

class DeliveryPolicyResolver {
    fun resolve(context: AppContext): DeliveryPolicy {
        val delivery = context.capabilities.delivery ?: return DisabledDeliveryPolicy

        return when (delivery.mode) {
            DeliveryMode.Customer -> CustomerDeliveryPolicy(delivery)
            DeliveryMode.Driver -> DriverDeliveryPolicy(delivery)
            DeliveryMode.Admin -> AdminDeliveryPolicy(delivery)
            DeliveryMode.Merchant -> MerchantDeliveryPolicy(delivery)
            DeliveryMode.ReadOnly -> ReadOnlyDeliveryPolicy
        }
    }
}
