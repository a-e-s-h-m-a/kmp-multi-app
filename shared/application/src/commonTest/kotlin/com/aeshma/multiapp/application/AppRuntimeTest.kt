package com.aeshma.multiapp.application

import com.aeshma.multiapp.core.model.AppId
import com.aeshma.multiapp.core.model.FeatureId
import com.aeshma.multiapp.core.config.AppCatalog
import com.aeshma.multiapp.core.config.defaultAppDefinitions
import kotlin.test.Test
import kotlin.test.assertEquals

class AppRuntimeTest {
    @Test
    fun metroGraphCreatesRuntimeForSelectedApp() {
        val runtime = createAppRuntime(AppId.fromExternalName("apptwo"))

        assertEquals(AppId.AppTwo, runtime.appId)
        assertEquals("admin", runtime.appDefinition.defaultUsername)
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
            Triple(AppId.AppOne, "customer", listOf("home", "delivery", "payments", "profile")),
            Triple(AppId.AppOne, "driver", listOf("home", "delivery", "profile")),
            Triple(AppId.AppTwo, "admin", listOf("home", "delivery", "reports", "profile")),
            Triple(AppId.AppTwo, "nod", listOf("home", "profile")),
        )

        cases.forEach { (appId, username, expectedFeatures) ->
            val actual = registry.availableFeatures(catalog.contextFor(appId, username))
                .map { it.id.value }
            assertEquals(expectedFeatures, actual, "$appId / $username")
        }
    }
}
