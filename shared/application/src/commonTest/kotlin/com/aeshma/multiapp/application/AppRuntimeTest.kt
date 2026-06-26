package com.aeshma.multiapp.application

import com.aeshma.multiapp.core.analytics.ConsoleAnalyticsClient
import com.aeshma.multiapp.core.config.UnsupportedExperienceException
import com.aeshma.multiapp.core.config.LocalAuthRepository
import com.aeshma.multiapp.core.model.AppId
import com.aeshma.multiapp.core.model.BusinessUnitId
import com.aeshma.multiapp.core.model.FeatureId
import com.aeshma.multiapp.core.config.AppCatalog
import com.aeshma.multiapp.core.config.defaultAppDefinitions
import com.aeshma.multiapp.core.model.ProductId
import com.aeshma.multiapp.core.model.RoleId
import com.aeshma.multiapp.feature.delivery.DeliveryPolicyResolver
import com.aeshma.multiapp.feature.delivery.SampleDeliveryRepository
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AppRuntimeTest {
    @Test
    fun metroGraphCreatesRuntimeForSelectedApp() {
        val runtime = createAppRuntime(AppId.fromExternalName("apptwo"))

        assertEquals(AppId.AppTwo, runtime.appId)
        assertEquals("admin", runtime.appDefinition.defaultUsername)
    }

    @Test
    fun standaloneProductAutoTargetsSingleExperience() {
        val runtime = createProductRuntime(ProductId.AppOneStandalone)

        assertEquals(ProductId.AppOneStandalone, runtime.productId)
        assertEquals(AppId.AppOne, runtime.defaultExperience)
        assertEquals(listOf(AppId.AppOne), runtime.supportedExperiences.map { it.id })
        assertEquals(AppId.AppOne, runtime.appRuntimeFor(AppId.AppOne).appId)
    }

    @Test
    fun superAppCanLaunchMultipleExperiences() {
        val runtime = createProductRuntime(ProductId.fromExternalName("superapp"))

        assertEquals(ProductId.SuperApp, runtime.productId)
        assertEquals(null, runtime.defaultExperience)
        assertEquals(listOf(AppId.AppOne, AppId.AppTwo), runtime.supportedExperiences.map { it.id })
        assertEquals(listOf("Newport&Buckhead", "Shop"), runtime.supportedExperienceDefinitions.map { it.displayName })
        assertEquals("customer", runtime.appRuntimeFor(AppId.AppOne).appDefinition.defaultUsername)
        assertEquals("admin", runtime.appRuntimeFor(AppId.AppTwo).appDefinition.defaultUsername)
    }

    @Test
    fun productRuntimeFiltersExperiencesByBusinessUnit() {
        val superApp = createProductRuntime(ProductId.SuperApp)
        val appOne = createProductRuntime(ProductId.AppOneStandalone)
        val appTwo = createProductRuntime(ProductId.AppTwoStandalone)

        assertEquals(listOf("Newport&Buckhead"), superApp.allowedExperiencesFor(BusinessUnitId.SSMG).map { it.displayName })
        assertEquals(listOf("Shop"), superApp.allowedExperiencesFor(BusinessUnitId.USBL).map { it.displayName })
        assertEquals(listOf("Newport&Buckhead"), appOne.allowedExperiencesFor(BusinessUnitId.SSMG).map { it.displayName })
        assertEquals(emptyList(), appOne.allowedExperiencesFor(BusinessUnitId.USBL))
        assertEquals(listOf("Shop"), appTwo.allowedExperiencesFor(BusinessUnitId.USBL).map { it.displayName })
    }

    @Test
    fun commerceCapabilitiesCombineBusinessUnitExperienceRoleAndExplicitCapabilities() {
        val runtime = createProductRuntime(ProductId.SuperApp)

        val capabilities = runtime.resolvedCommerceCapabilities(
            appId = AppId.AppTwo,
            businessUnitId = BusinessUnitId.USBL,
            roles = setOf(RoleId.CustomerAdmin),
        )

        assertEquals(true, "orders.view" in capabilities.permissions.map { it.value })
        assertEquals(true, "orders.edit" in capabilities.permissions.map { it.value })
        assertEquals(true, "delivery.map" in capabilities.permissions.map { it.value })
        assertEquals(true, "pdp.internalDetails" in capabilities.permissions.map { it.value })
    }

    @Test
    fun productRuntimeRejectsUnsupportedExperiences() {
        val runtime = createProductRuntime(ProductId.AppOneStandalone)

        assertFailsWith<UnsupportedExperienceException> {
            runtime.appRuntimeFor(AppId.AppTwo)
        }
    }

    @Test
    fun iosCompositionRootsUseTargetSpecificConfiguration() {
        val appOne = IOSAppCompositionRoot("AppOne")
        val appTwo = IOSAppCompositionRoot("AppTwo")

        assertEquals("Newport&Buckhead", appOne.appName)
        assertEquals("customer", appOne.defaultUsername)
        assertEquals("Shop", appTwo.appName)
        assertEquals("admin", appTwo.defaultUsername)
    }

    @Test
    fun featureIdsAreStableKeys() {
        assertEquals("delivery", FeatureId.Delivery.value)
    }

    @Test
    fun featureAvailabilityIsTableDriven() {
        val catalog = AppCatalog(defaultAppDefinitions())
        val registry = FeatureRegistry()
        val cases = listOf(
            Triple(AppId.AppOne, "customer", listOf("orders", "lists", "catalog", "product-details", "delivery")),
            Triple(AppId.AppOne, "driver", listOf("orders", "lists", "catalog", "product-details", "delivery")),
            Triple(AppId.AppTwo, "admin", listOf("orders", "lists", "catalog", "product-details", "delivery")),
            Triple(AppId.AppTwo, "nod", listOf("orders", "lists", "catalog", "product-details", "delivery")),
        )

        cases.forEach { (appId, username, expectedFeatures) ->
            val actual = registry.availableFeatures(catalog.contextFor(appId, username))
                .map { it.id.value }
            assertEquals(expectedFeatures, actual, "$appId / $username")
        }
    }

    @Test
    fun logoutClearsContextAndTracksSessionLifecycle() {
        val catalog = AppCatalog(defaultAppDefinitions())
        val analytics = ConsoleAnalyticsClient()
        val session = AppSession(
            authRepository = LocalAuthRepository(catalog),
            featureRegistry = FeatureRegistry(),
            deliveryPolicyResolver = DeliveryPolicyResolver(),
            deliveryRepository = SampleDeliveryRepository(),
            analyticsClient = analytics,
        )

        runImmediately { session.login(AppId.AppOne, "customer") }
        assertNotNull(session.currentContext)

        session.logout()

        assertNull(session.currentContext)
        assertFailsWith<SessionNotStartedException> { session.availableFeatures() }
        assertEquals(
            listOf("login_completed", "logout_completed"),
            analytics.trackedEvents().map { it.name },
        )
    }

    private fun <T> runImmediately(block: suspend () -> T): T {
        var outcome: Result<T>? = null
        block.startCoroutine(
            object : Continuation<T> {
                override val context = EmptyCoroutineContext

                override fun resumeWith(result: Result<T>) {
                    outcome = result
                }
            },
        )
        return checkNotNull(outcome) { "Expected the local suspend operation to complete immediately." }.getOrThrow()
    }
}
