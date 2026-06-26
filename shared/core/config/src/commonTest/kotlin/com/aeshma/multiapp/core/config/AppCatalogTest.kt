package com.aeshma.multiapp.core.config

import com.aeshma.multiapp.core.model.AppId
import com.aeshma.multiapp.core.model.BusinessUnitId
import com.aeshma.multiapp.core.model.CommerceCapabilities
import com.aeshma.multiapp.core.model.ProductId
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
    fun productCatalogDefinesStandaloneAndSuperAppProducts() {
        val productCatalog = ProductCatalog(defaultProductDefinitions())

        assertEquals(
            setOf(AppId.AppOne),
            productCatalog.definition(ProductId.AppOneStandalone).supportedExperiences,
        )
        assertEquals(
            setOf(AppId.AppOne, AppId.AppTwo),
            productCatalog.definition(ProductId.fromExternalName(" superapp ")).supportedExperiences,
        )
        assertNull(productCatalog.definition(ProductId.SuperApp).defaultExperience)
    }

    @Test
    fun unknownProductsHaveATypeSpecificFailure() {
        val productCatalog = ProductCatalog(defaultProductDefinitions())

        assertFailsWith<UnknownProductException> {
            productCatalog.definition(ProductId("MissingProduct"))
        }
    }

    @Test
    fun hardCodedExperienceCatalogDefinesBoutiqueAndShopExperiences() {
        val experienceCatalog = ExperienceCatalog(defaultExperienceDefinitions())

        assertEquals("Newport&Buckhead", experienceCatalog.definition(AppId.AppOne).displayName)
        assertEquals("SSMG Boutique Theme", experienceCatalog.definition(AppId.AppOne).theme)
        assertEquals(setOf(BusinessUnitId.SSMG), experienceCatalog.definition(AppId.AppOne).supportedBusinessUnits)

        assertEquals("Shop", experienceCatalog.definition(AppId.AppTwo).displayName)
        assertEquals("Broadline Theme", experienceCatalog.definition(AppId.AppTwo).theme)
        assertEquals(setOf(BusinessUnitId.USBL), experienceCatalog.definition(AppId.AppTwo).supportedBusinessUnits)
    }

    @Test
    fun businessUnitCatalogDefinesAllowedExperienceCombinations() {
        val businessUnitCatalog = BusinessUnitCatalog(defaultBusinessUnitDefinitions())

        assertEquals(setOf(AppId.AppOne), businessUnitCatalog.definition(BusinessUnitId.SSMG).allowedExperiences)
        assertEquals(setOf(AppId.AppTwo), businessUnitCatalog.definition(BusinessUnitId.USBL).allowedExperiences)
    }

    @Test
    fun catalogSupportsAThirdAppWithoutCoreChanges() {
        val appThree = AppId("AppThree")
        val appThreeBu = BusinessUnitId("APP_THREE_BU")
        val customCatalog = AppCatalog(
            definitions = listOf(
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
            experienceCatalog = ExperienceCatalog(
                listOf(
                    ExperienceDefinition(
                        appId = appThree,
                        displayName = "App Three Experience",
                        url = null,
                        logo = null,
                        allowedSites = setOf("APP3"),
                        theme = "App Three Theme",
                        supportedCapabilities = CommerceCapabilities.of("orders.view"),
                        supportedBusinessUnits = setOf(appThreeBu),
                    ),
                ),
            ),
            businessUnitCatalog = BusinessUnitCatalog(
                listOf(
                    BusinessUnitDefinition(
                        id = appThreeBu,
                        allowedExperiences = setOf(appThree),
                        allowedCapabilities = CommerceCapabilities.of("catalog.view"),
                    ),
                ),
            ),
        )

        assertEquals(appThree, customCatalog.contextFor(appThree, "viewer").appId)
    }
}
