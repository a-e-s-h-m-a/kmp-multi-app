package com.aeshma.multiapp.feature.productdetails

import com.aeshma.multiapp.core.model.FeatureActionDefinition
import com.aeshma.multiapp.core.model.CommerceFeatureModule
import com.aeshma.multiapp.core.model.FeatureDefinitionSpec
import com.aeshma.multiapp.core.model.FeatureId
import com.aeshma.multiapp.core.model.FeaturePermissionRow
import com.aeshma.multiapp.core.model.FeatureUiBlock
import com.aeshma.multiapp.core.model.PermissionId

object ProductDetailsFeature : CommerceFeatureModule {
    override val definition: FeatureDefinitionSpec = FeatureDefinitionSpec(
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
        uiBlocks = listOf(
            FeatureUiBlock(
                title = "Product summary",
                requiredPermission = PermissionId.PdpView,
                body = "Shows item image placeholder, pack size, price, availability, and add-to-list controls.",
            ),
            FeatureUiBlock(
                title = "Internal product details",
                requiredPermission = PermissionId.PdpInternalDetails,
                body = "Reveals internal margin, sourcing, restricted notes, and support-only product flags.",
            ),
        ),
    )
}
