package com.aeshma.multiapp.feature.delivery

data class DeliveryOrder(
    val id: String,
    val title: String,
    val status: DeliveryStatus,
)

enum class DeliveryStatus {
    Created,
    Assigned,
    PickedUp,
    Delivered,
    Cancelled,
}

enum class DeliveryAction {
    Create,
    Cancel,
    EditAddress,
    Track,
    Accept,
    MarkPickedUp,
    MarkDelivered,
    ViewOnly,
}
