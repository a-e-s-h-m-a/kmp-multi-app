package com.aeshma.multiapp.core.model

data class FeatureId(val value: String) {
    init {
        require(value.isNotBlank()) { "Feature id cannot be blank." }
    }

    companion object {
        val Orders = FeatureId("orders")
        val Lists = FeatureId("lists")
        val Catalog = FeatureId("catalog")
        val ProductDetails = FeatureId("product-details")
        val Delivery = FeatureId("delivery")
    }
}

data class FeaturePermissionRow(
    val label: String,
    val permission: PermissionId,
)

data class FeatureActionDefinition(
    val label: String,
    val requiredPermission: PermissionId,
    val result: String,
)

data class FeatureUiBlock(
    val title: String,
    val requiredPermission: PermissionId,
    val body: String,
)

data class FeatureDefinitionSpec(
    val id: FeatureId,
    val title: String,
    val requiredPermission: PermissionId,
    val tweakPermissions: List<PermissionId> = emptyList(),
    val permissionRows: List<FeaturePermissionRow> = emptyList(),
    val actions: List<FeatureActionDefinition> = emptyList(),
    val uiBlocks: List<FeatureUiBlock> = emptyList(),
)
