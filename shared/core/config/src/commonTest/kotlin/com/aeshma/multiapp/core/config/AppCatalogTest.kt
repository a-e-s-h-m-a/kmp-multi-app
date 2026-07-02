package com.aeshma.multiapp.core.config

import com.aeshma.multiapp.core.model.AppId
import com.aeshma.multiapp.core.model.BusinessUnitId
import com.aeshma.multiapp.core.model.CommerceCapabilities
import com.aeshma.multiapp.core.model.ExperienceId
import com.aeshma.multiapp.core.model.FeatureId
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
            setOf(ExperienceId.Shop),
            productCatalog.definition(ProductId.AppOneStandalone).supportedExperiences,
        )
        assertEquals(
            setOf(ExperienceId.NewportBuckhead, ExperienceId.Shop),
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

        assertEquals("Newport&Buckhead", experienceCatalog.definition(ExperienceId.NewportBuckhead).displayName)
        assertEquals("SSMG Boutique Theme", experienceCatalog.definition(ExperienceId.NewportBuckhead).theme)
        assertEquals(
            setOf(BusinessUnitId.SSMG, BusinessUnitId.CABL),
            experienceCatalog.definition(ExperienceId.NewportBuckhead).supportedBusinessUnits,
        )

        assertEquals("Shop", experienceCatalog.definition(ExperienceId.Shop).displayName)
        assertEquals("Broadline Theme", experienceCatalog.definition(ExperienceId.Shop).theme)
        assertEquals(
            setOf(BusinessUnitId.USBL, BusinessUnitId.CABL),
            experienceCatalog.definition(ExperienceId.Shop).supportedBusinessUnits,
        )
    }

    @Test
    fun businessUnitCatalogDefinesAllowedExperienceCombinations() {
        val businessUnitCatalog = BusinessUnitCatalog(defaultBusinessUnitDefinitions())

        assertEquals(setOf(ExperienceId.NewportBuckhead), businessUnitCatalog.definition(BusinessUnitId.SSMG).allowedExperiences)
        assertEquals(setOf(ExperienceId.Shop), businessUnitCatalog.definition(BusinessUnitId.USBL).allowedExperiences)
        assertEquals(
            setOf(ExperienceId.NewportBuckhead, ExperienceId.Shop),
            businessUnitCatalog.definition(BusinessUnitId.CABL).allowedExperiences,
        )
    }

    @Test
    fun catalogSupportsAThirdAppWithoutCoreChanges() {
        val appThree = AppId("AppThree")
        val appThreeExperience = ExperienceId("app-three-experience")
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
                        id = appThreeExperience,
                        hostAppId = appThree,
                        displayName = "App Three Experience",
                        url = null,
                        logo = null,
                        allowedSites = setOf("APP3"),
                        theme = "App Three Theme",
                        supportedFeatures = setOf(FeatureId.Orders),
                        supportedCapabilities = CommerceCapabilities.of("orders.view"),
                        supportedBusinessUnits = setOf(appThreeBu),
                    ),
                ),
            ),
            businessUnitCatalog = BusinessUnitCatalog(
                listOf(
                    BusinessUnitDefinition(
                        id = appThreeBu,
                        allowedExperiences = setOf(appThreeExperience),
                        allowedCapabilities = CommerceCapabilities.of("catalog.view"),
                    ),
                ),
            ),
        )

        assertEquals(appThree, customCatalog.contextFor(appThree, "viewer").appId)
    }
}
