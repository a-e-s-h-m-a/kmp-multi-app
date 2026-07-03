package com.aeshma.multiapp.application

import com.aeshma.multiapp.core.model.AppContext
import com.aeshma.multiapp.core.model.CommerceFeatureModule
import com.aeshma.multiapp.core.model.FeatureActionDefinition
import com.aeshma.multiapp.core.model.FeatureDefinitionSpec
import com.aeshma.multiapp.core.model.FeatureId
import com.aeshma.multiapp.core.model.FeaturePermissionRow
import com.aeshma.multiapp.core.model.FeatureUiBlock
import com.aeshma.multiapp.core.model.PermissionId
import com.aeshma.multiapp.feature.catalog.CatalogFeature
import com.aeshma.multiapp.feature.delivery.DeliveryFeature
import com.aeshma.multiapp.feature.lists.ListsFeature
import com.aeshma.multiapp.feature.orders.OrdersFeature
import com.aeshma.multiapp.feature.productdetails.ProductDetailsFeature

class FeatureDescriptor(
    val definition: FeatureDefinitionSpec,
    private val availability: (AppContext) -> Boolean,
) {
    val id: FeatureId = definition.id
    val title: String = definition.title
    val requiredPermission: PermissionId = definition.requiredPermission
    val tweakPermissions: List<PermissionId> = definition.tweakPermissions
    val permissionRows: List<FeaturePermissionRow> = definition.permissionRows
    val actions: List<FeatureActionDefinition> = definition.actions
    val uiBlocks: List<FeatureUiBlock> = definition.uiBlocks

    fun isAvailable(context: AppContext): Boolean = availability(context)

    fun enabledTweaks(context: AppContext): List<PermissionId> =
        tweakPermissions.filter(context.commerceCapabilities::has)
}

class FeatureRegistry {
    private val features = listOf(
        descriptor(OrdersFeature),
        descriptor(ListsFeature),
        descriptor(CatalogFeature),
        descriptor(ProductDetailsFeature),
        descriptor(DeliveryFeature),
    )

    fun availableFeatures(context: AppContext): List<FeatureDescriptor> =
        features.filter { it.id in context.supportedFeatures && it.isAvailable(context) }

    private fun descriptor(definition: FeatureDefinitionSpec): FeatureDescriptor =
        FeatureDescriptor(definition) { definition.requiredPermission in it.commerceCapabilities }

    private fun descriptor(module: CommerceFeatureModule): FeatureDescriptor =
        descriptor(module.definition)
}
