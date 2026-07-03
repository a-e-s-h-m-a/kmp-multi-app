package com.aeshma.multiapp.feature.catalog

import com.aeshma.multiapp.core.model.FeatureActionDefinition
import com.aeshma.multiapp.core.model.CommerceFeatureModule
import com.aeshma.multiapp.core.model.FeatureDefinitionSpec
import com.aeshma.multiapp.core.model.FeatureId
import com.aeshma.multiapp.core.model.FeaturePermissionRow
import com.aeshma.multiapp.core.model.FeatureUiBlock
import com.aeshma.multiapp.core.model.PermissionId

object CatalogFeature : CommerceFeatureModule {
    override val definition: FeatureDefinitionSpec = FeatureDefinitionSpec(
        id = FeatureId.Catalog,
        title = "Catalog",
        requiredPermission = PermissionId.CatalogView,
        tweakPermissions = listOf(PermissionId.CatalogRecommendations),
        permissionRows = listOf(
            FeaturePermissionRow("View catalog", PermissionId.CatalogView),
            FeaturePermissionRow("Recommendations", PermissionId.CatalogRecommendations),
        ),
        actions = listOf(
            FeatureActionDefinition("Browse", PermissionId.CatalogView, "Catalog browse state updated."),
            FeatureActionDefinition("Recommend", PermissionId.CatalogRecommendations, "Recommendation rail recalculated."),
        ),
        uiBlocks = listOf(
            FeatureUiBlock(
                title = "Catalog grid",
                requiredPermission = PermissionId.CatalogView,
                body = "Shows available products, category chips, and simulated inventory badges.",
            ),
            FeatureUiBlock(
                title = "Recommendation rail",
                requiredPermission = PermissionId.CatalogRecommendations,
                body = "Adds personalized substitute, seasonal, and high-margin product suggestions.",
            ),
        ),
    )
}
