package com.aeshma.multiapp.feature.delivery

import com.aeshma.multiapp.core.model.AppContext
import com.aeshma.multiapp.core.model.AppId
import com.aeshma.multiapp.core.model.DeliveryCapability
import com.aeshma.multiapp.core.model.DeliveryMode
import com.aeshma.multiapp.core.model.UserCapabilities
import com.aeshma.multiapp.core.model.UserType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DeliveryPolicyTest {
    private val resolver = DeliveryPolicyResolver()

    @Test
    fun resolverDependsOnCapabilityRatherThanAppIdentity() {
        val appThreeCustomer = context(
            AppId("AppThree"),
            DeliveryCapability(mode = DeliveryMode.Customer, canCancelDelivery = true),
        )

        assertIs<CustomerDeliveryPolicy>(resolver.resolve(appThreeCustomer))
    }

    @Test
    fun driverActionsFollowOrderState() {
        val policy = resolver.resolve(
            context(
                AppId.AppOne,
                DeliveryCapability(
                    mode = DeliveryMode.Driver,
                    showLiveTracking = true,
                    canAcceptDelivery = true,
                    canMarkPickedUp = true,
                    canMarkDelivered = true,
                ),
            ),
        )

        assertEquals(listOf(DeliveryAction.Accept), policy.availableActions(order(DeliveryStatus.Created)))
        assertEquals(
            listOf(DeliveryAction.MarkPickedUp, DeliveryAction.Track),
            policy.availableActions(order(DeliveryStatus.Assigned)),
        )
    }

    private fun context(appId: AppId, delivery: DeliveryCapability): AppContext = AppContext(
        appId = appId,
        userId = "test-user",
        userType = UserType.Customer,
        capabilities = UserCapabilities(delivery = delivery),
    )

    private fun order(status: DeliveryStatus): DeliveryOrder =
        DeliveryOrder("test", "Test", status)
}
