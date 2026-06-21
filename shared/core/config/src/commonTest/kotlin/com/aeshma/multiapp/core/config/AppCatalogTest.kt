package com.aeshma.multiapp.core.config

import com.aeshma.multiapp.core.model.AppId
import com.aeshma.multiapp.core.model.UserCapabilities
import com.aeshma.multiapp.core.model.UserType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class AppCatalogTest {
    private val catalog = AppCatalog(defaultAppDefinitions())

    @Test
    fun defaultsAndProfilesComeFromDefinitions() {
        assertEquals("customer", catalog.definition(AppId.AppOne).defaultUsername)
        assertEquals("admin", catalog.definition(AppId.AppTwo).defaultUsername)
        assertEquals(UserType.Driver, catalog.contextFor(AppId.AppOne, " DRIVER ").userType)
    }

    @Test
    fun nodIsARegularProfileWithNoCapabilities() {
        val context = catalog.contextFor(AppId.AppOne, "nod")

        assertNull(context.capabilities.delivery)
        assertNull(context.capabilities.reports)
        assertNull(context.capabilities.payments)
    }

    @Test
    fun unknownProfilesFailInsteadOfUsingAnUnrelatedDefault() {
        assertFailsWith<UnknownProfileException> {
            catalog.contextFor(AppId.AppOne, "someone-else")
        }
    }

    @Test
    fun unknownAppsHaveATypeSpecificFailure() {
        assertFailsWith<UnknownAppException> {
            catalog.definition(AppId("MissingApp"))
        }
    }

    @Test
    fun catalogSupportsAThirdAppWithoutCoreChanges() {
        val appThree = AppId("AppThree")
        val customCatalog = AppCatalog(
            listOf(
                AppDefinition(
                    id = appThree,
                    displayName = "App Three",
                    configKey = "appThree",
                    defaultUsername = "viewer",
                    profiles = mapOf(
                        "viewer" to UserProfile(UserType.Customer, UserCapabilities.none()),
                    ),
                ),
            ),
        )

        assertEquals(appThree, customCatalog.contextFor(appThree, "viewer").appId)
    }
}
