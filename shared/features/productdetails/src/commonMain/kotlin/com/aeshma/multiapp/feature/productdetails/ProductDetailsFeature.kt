package com.aeshma.multiapp.feature.productdetails

import com.aeshma.multiapp.core.model.FeatureActionDefinition
import com.aeshma.multiapp.core.model.FeatureDefinitionSpec
import com.aeshma.multiapp.core.model.FeatureId
import com.aeshma.multiapp.core.model.FeaturePermissionRow
import com.aeshma.multiapp.core.model.PermissionId

object ProductDetailsFeature {
    val definition: FeatureDefinitionSpec = FeatureDefinitionSpec(
        id = FeatureId.ProductDetails,
        title = "Product Details",
        requiredPermission = PermissionId.PdpView,
        tweakPermissions = listOf(PermissionId.PdpInternalDetails),
        permissionRows = listOf(
            FeaturePermissionRow("View PDP", PermissionId.PdpView),
            FeaturePermissionRow("Internal details", PermissionId.PdpInternalDetails),
        ),
        actions = listOf(
            FeatureActionDefinition("Open PDP", PermissionId.PdpView, "Product details opened."),
            FeatureActionDefinition("Internal", PermissionId.PdpInternalDetails, "Internal product panel revealed."),
        ),
    )
}
