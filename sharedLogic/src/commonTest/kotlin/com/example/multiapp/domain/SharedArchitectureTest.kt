package com.example.multiapp.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SharedArchitectureTest {
    private val auth = FakeAuthRepository(FakeNetworkClient())
    private val registry = FeatureRegistry.default()
    private val resolver = DeliveryPolicyResolver()

    @Test
    fun externalAppNamesAreParsedCentrally() {
        assertEquals(AppId.AppOne, AppId.fromExternalName("appone"))
        assertEquals(AppId.AppTwo, AppId.fromExternalName(" AppTwo "))
        assertEquals(AppId("AppThree"), AppId.fromExternalName("AppThree"))
    }

    @Test
    fun appDefaultsComeFromCatalog() {
        val catalog = AppCatalog.default()

        assertEquals("customer", catalog.definition(AppId.AppOne).defaultUsername)
        assertEquals("admin", catalog.definition(AppId.AppTwo).defaultUsername)
    }

    @Test
    fun catalogAcceptsAnAppWithoutChangingCoreLogic() {
        val appThree = AppId("AppThree")
        val catalog = AppCatalog(
            definitions = listOf(
                AppDefinition(
                    id = appThree,
                    displayName = "App Three",
                    configKey = "appThree",
                    defaultUsername = "viewer",
                    defaultUserType = UserType.Customer,
                    profiles = mapOf(
                        "viewer" to UserProfile(
                            userType = UserType.Customer,
                            capabilities = UserCapabilities(
                                delivery = null,
                                reports = null,
                                payments = null,
                            ),
                        ),
                    ),
                ),
            ),
        )

        val context = catalog.contextFor(appThree, "viewer")

        assertEquals(appThree, context.appId)
        assertEquals("appThree-viewer", context.userId)
        assertEquals(listOf(FeatureId.Home, FeatureId.Profile), registry.availableFeatures(context).map { it.id })
    }

    @Test
    fun appOneCustomerFeatures() {
        val context = auth.contextFor(AppId.AppOne, "customer")

        assertEquals(
            listOf(FeatureId.Home, FeatureId.Delivery, FeatureId.Payments, FeatureId.Profile),
            registry.availableFeatures(context).map { it.id },
        )
    }

    @Test
    fun appOneDriverFeatures() {
        val context = auth.contextFor(AppId.AppOne, "driver")

        assertEquals(
            listOf(FeatureId.Home, FeatureId.Delivery, FeatureId.Profile),
            registry.availableFeatures(context).map { it.id },
        )
    }

    @Test
    fun appTwoAdminFeatures() {
        val context = auth.contextFor(AppId.AppTwo, "admin")

        assertEquals(
            listOf(FeatureId.Home, FeatureId.Delivery, FeatureId.Reports, FeatureId.Profile),
            registry.availableFeatures(context).map { it.id },
        )
    }

    @Test
    fun appTwoMerchantFeatures() {
        val context = auth.contextFor(AppId.AppTwo, "merchant")

        assertEquals(
            listOf(FeatureId.Home, FeatureId.Delivery, FeatureId.Reports, FeatureId.Profile),
            registry.availableFeatures(context).map { it.id },
        )
    }

    @Test
    fun nodUserOnlyGetsHomeAndProfile() {
        val context = auth.contextFor(AppId.AppOne, "nod")

        assertEquals(
            listOf(FeatureId.Home, FeatureId.Profile),
            registry.availableFeatures(context).map { it.id },
        )
        assertIs<DisabledDeliveryPolicy>(resolver.resolve(context))
    }

    @Test
    fun deliveryPolicyResolverSelectsExpectedPolicies() {
        assertIs<CustomerDeliveryPolicy>(resolver.resolve(auth.contextFor(AppId.AppOne, "customer")))
        assertIs<DriverDeliveryPolicy>(resolver.resolve(auth.contextFor(AppId.AppOne, "driver")))
        assertIs<AdminDeliveryPolicy>(resolver.resolve(auth.contextFor(AppId.AppTwo, "admin")))
        assertIs<MerchantDeliveryPolicy>(resolver.resolve(auth.contextFor(AppId.AppTwo, "merchant")))
        assertIs<ReadOnlyDeliveryPolicy>(resolver.resolve(auth.contextFor(AppId.AppTwo, "readonly")))
        assertIs<DisabledDeliveryPolicy>(resolver.resolve(auth.contextFor(AppId.AppTwo, "nod")))
    }

    @Test
    fun customerDeliveryActions() {
        val policy = resolver.resolve(auth.contextFor(AppId.AppOne, "customer"))

        assertEquals(
            listOf(DeliveryAction.Cancel, DeliveryAction.EditAddress, DeliveryAction.Track),
            policy.availableActions(order(DeliveryStatus.Created)),
        )
        assertEquals(
            listOf(DeliveryAction.Track),
            policy.availableActions(order(DeliveryStatus.Assigned)),
        )
    }

    @Test
    fun driverDeliveryActions() {
        val policy = resolver.resolve(auth.contextFor(AppId.AppOne, "driver"))

        assertEquals(
            listOf(DeliveryAction.Accept),
            policy.availableActions(order(DeliveryStatus.Created)),
        )
        assertEquals(
            listOf(DeliveryAction.MarkPickedUp, DeliveryAction.Track),
            policy.availableActions(order(DeliveryStatus.Assigned)),
        )
        assertEquals(
            listOf(DeliveryAction.MarkDelivered, DeliveryAction.Track),
            policy.availableActions(order(DeliveryStatus.PickedUp)),
        )
    }

    @Test
    fun adminDeliveryActions() {
        val policy = resolver.resolve(auth.contextFor(AppId.AppTwo, "admin"))

        assertEquals(
            listOf(DeliveryAction.Cancel, DeliveryAction.EditAddress, DeliveryAction.Track),
            policy.availableActions(order(DeliveryStatus.Created)),
        )
        assertEquals(
            listOf(DeliveryAction.Cancel, DeliveryAction.EditAddress, DeliveryAction.Track),
            policy.availableActions(order(DeliveryStatus.Assigned)),
        )
    }

    @Test
    fun readOnlyAndDisabledDeliveryActions() {
        val readOnlyPolicy = resolver.resolve(auth.contextFor(AppId.AppOne, "readonly"))
        val disabledPolicy = resolver.resolve(auth.contextFor(AppId.AppTwo, "nod"))

        DeliveryStatus.entries.forEach { status ->
            assertEquals(listOf(DeliveryAction.ViewOnly), readOnlyPolicy.availableActions(order(status)))
            assertEquals(emptyList(), disabledPolicy.availableActions(order(status)))
        }
    }

    private fun order(status: DeliveryStatus): DeliveryOrder =
        DeliveryOrder(id = "test", title = "Test order", status = status)
}
