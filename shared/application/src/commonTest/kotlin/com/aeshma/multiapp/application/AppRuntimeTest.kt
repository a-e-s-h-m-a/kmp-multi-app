package com.aeshma.multiapp.application

import com.aeshma.multiapp.core.analytics.ConsoleAnalyticsClient
import com.aeshma.multiapp.core.config.UnsupportedExperienceException
import com.aeshma.multiapp.core.config.LocalAuthRepository
import com.aeshma.multiapp.core.model.AppId
import com.aeshma.multiapp.core.model.BusinessUnitId
import com.aeshma.multiapp.core.model.ExperienceId
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
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
        assertEquals(ExperienceId.Shop, runtime.defaultExperience)
        assertEquals(listOf("Shop"), runtime.supportedExperienceDefinitions.map { it.displayName })
        assertEquals(AppId.AppOne, runtime.appRuntimeFor(ExperienceId.Shop).appId)
    }

    @Test
    fun superAppCanLaunchMultipleExperiences() {
        val runtime = createProductRuntime(ProductId.fromExternalName("superapp"))

        assertEquals(ProductId.SuperApp, runtime.productId)
        assertEquals(null, runtime.defaultExperience)
        assertEquals(listOf("Newport&Buckhead", "Shop"), runtime.supportedExperienceDefinitions.map { it.displayName })
        assertEquals("admin", runtime.appRuntimeFor(ExperienceId.NewportBuckhead).appDefinition.defaultUsername)
        assertEquals("customer", runtime.appRuntimeFor(ExperienceId.Shop).appDefinition.defaultUsername)
    }

    @Test
    fun productRuntimeFiltersExperiencesByBusinessUnit() {
        val superApp = createProductRuntime(ProductId.SuperApp)
        val appOne = createProductRuntime(ProductId.AppOneStandalone)
        val appTwo = createProductRuntime(ProductId.AppTwoStandalone)

        assertEquals(listOf("Newport&Buckhead"), superApp.allowedExperiencesFor(BusinessUnitId.SSMG).map { it.displayName })
        assertEquals(listOf("Shop"), superApp.allowedExperiencesFor(BusinessUnitId.USBL).map { it.displayName })
        assertEquals(listOf("Newport&Buckhead", "Shop"), superApp.allowedExperiencesFor(BusinessUnitId.CABL).map { it.displayName })
        assertEquals(emptyList(), appOne.allowedExperiencesFor(BusinessUnitId.SSMG))
        assertEquals(listOf("Shop"), appOne.allowedExperiencesFor(BusinessUnitId.USBL).map { it.displayName })
        assertEquals(listOf("Newport&Buckhead"), appTwo.allowedExperiencesFor(BusinessUnitId.SSMG).map { it.displayName })
    }

    @Test
    fun productRuntimeExposesBuildTimeFeatureUnionForSupportedExperiences() {
        val appOne = createProductRuntime(ProductId.AppOneStandalone)
        val appTwo = createProductRuntime(ProductId.AppTwoStandalone)
        val superApp = createProductRuntime(ProductId.SuperApp)

        assertEquals(
            setOf(FeatureId.Orders, FeatureId.Catalog, FeatureId.ProductDetails, FeatureId.Delivery),
            appOne.bundledFeatures,
        )
        assertEquals(
            setOf(FeatureId.Orders, FeatureId.Lists, FeatureId.Delivery),
            appTwo.bundledFeatures,
        )
        assertEquals(
            setOf(FeatureId.Orders, FeatureId.Lists, FeatureId.Catalog, FeatureId.ProductDetails, FeatureId.Delivery),
            superApp.bundledFeatures,
        )
    }

    @Test
    fun commerceCapabilitiesIntersectUserGrantsWithBusinessUnitAndExperienceCeilings() {
        val runtime = createProductRuntime(ProductId.SuperApp)

        val capabilities = runtime.resolvedCommerceCapabilities(
            experienceId = ExperienceId.Shop,
            businessUnitId = BusinessUnitId.USBL,
            roles = setOf(RoleId.CustomerAdmin),
        )
        val permissions = capabilities.permissions.map { it.value }.toSet()

        assertTrue("orders.view" in permissions)
        assertTrue("orders.notifications" in permissions)
        assertTrue("delivery.map" in permissions)
        assertFalse("orders.edit" in permissions)
        assertFalse("pdp.internalDetails" in permissions)
    }

    @Test
    fun productRuntimeResolvesHardcodedLoginGrantsIntoExperienceOptions() {
        val runtime = createProductRuntime(ProductId.SuperApp)

        val options = runtime.resolvedExperienceOptions("customer")

        assertTrue(options.isNotEmpty())
        assertTrue(options.size <= 2)
        options.forEach { option ->
            assertTrue(option.context.commerceCapabilities.permissions.isNotEmpty())
            assertEquals(option.grant.roles, option.context.roles)
        }
    }

    @Test
    fun productRuntimeRejectsUnsupportedExperiences() {
        val runtime = createProductRuntime(ProductId.AppOneStandalone)

        assertFailsWith<UnsupportedExperienceException> {
            runtime.appRuntimeFor(ExperienceId.NewportBuckhead)
        }
    }

    @Test
    fun iosCompositionRootsUseTargetSpecificConfiguration() {
        val appOne = IOSAppCompositionRoot("AppOne")
        val appTwo = IOSAppCompositionRoot("AppTwo")

        assertEquals("Shop", appOne.appName)
        assertEquals("customer", appOne.defaultUsername)
        assertEquals("Newport&Buckhead", appTwo.appName)
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
            Triple(AppId.AppOne, "customer", listOf("orders", "catalog", "product-details", "delivery")),
            Triple(AppId.AppOne, "driver", listOf("delivery")),
            Triple(AppId.AppTwo, "admin", listOf("orders", "lists", "delivery")),
            Triple(AppId.AppTwo, "nod", listOf("orders", "lists", "delivery")),
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
