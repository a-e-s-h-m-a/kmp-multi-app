package com.aeshma.multiapp.feature.lists

import com.aeshma.multiapp.core.model.FeatureActionDefinition
import com.aeshma.multiapp.core.model.FeatureDefinitionSpec
import com.aeshma.multiapp.core.model.FeatureId
import com.aeshma.multiapp.core.model.FeaturePermissionRow
import com.aeshma.multiapp.core.model.PermissionId

object ListsFeature {
    val definition: FeatureDefinitionSpec = FeatureDefinitionSpec(
        id = FeatureId.Lists,
        title = "Lists",
        requiredPermission = PermissionId.ListsView,
        tweakPermissions = listOf(
            PermissionId.ListsEdit,
            PermissionId.ListsPurchaseHistory,
        ),
        permissionRows = listOf(
            FeaturePermissionRow("View lists", PermissionId.ListsView),
            FeaturePermissionRow("Edit lists", PermissionId.ListsEdit),
            FeaturePermissionRow("Purchase history", PermissionId.ListsPurchaseHistory),
        ),
        actions = listOf(
            FeatureActionDefinition("Open", PermissionId.ListsView, "List opened."),
            FeatureActionDefinition("Rename", PermissionId.ListsEdit, "List rename saved locally."),
            FeatureActionDefinition("History", PermissionId.ListsPurchaseHistory, "Purchase history filter applied."),
        ),
    )
}
