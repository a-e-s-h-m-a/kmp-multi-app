package com.aeshma.multiapp.feature.catalog

import com.aeshma.multiapp.core.model.FeatureActionDefinition
import com.aeshma.multiapp.core.model.FeatureDefinitionSpec
import com.aeshma.multiapp.core.model.FeatureId
import com.aeshma.multiapp.core.model.FeaturePermissionRow
import com.aeshma.multiapp.core.model.PermissionId

object CatalogFeature {
    val definition: FeatureDefinitionSpec = FeatureDefinitionSpec(
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
    )
}
