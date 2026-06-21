package com.aeshma.multiapp.feature.delivery

interface DeliveryRepository {
    fun orders(): List<DeliveryOrder>
}

class SampleDeliveryRepository : DeliveryRepository {
    override fun orders(): List<DeliveryOrder> = listOf(
        DeliveryOrder("DEL-1001", "Grocery drop-off", DeliveryStatus.Created),
        DeliveryOrder("DEL-1002", "Pharmacy pickup", DeliveryStatus.Assigned),
        DeliveryOrder("DEL-1003", "Cafe order", DeliveryStatus.PickedUp),
        DeliveryOrder("DEL-1004", "Office lunch", DeliveryStatus.Delivered),
        DeliveryOrder("DEL-1005", "Cancelled parcel", DeliveryStatus.Cancelled),
    )
}
