package com.aeshma.multiapp.application

import com.aeshma.multiapp.core.model.AppContext
import com.aeshma.multiapp.core.model.FeatureActionDefinition
import com.aeshma.multiapp.core.model.FeatureDefinitionSpec
import com.aeshma.multiapp.core.model.FeatureId
import com.aeshma.multiapp.core.model.FeaturePermissionRow
import com.aeshma.multiapp.core.model.FeatureUiBlock
import com.aeshma.multiapp.core.model.PermissionId

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

class FeatureRegistry(featureDefinitions: List<FeatureDefinitionSpec>) {
    private val features = featureDefinitions.map(::descriptor)

    fun availableFeatures(context: AppContext): List<FeatureDescriptor> =
        features.filter { it.id in context.supportedFeatures && it.isAvailable(context) }

    private fun descriptor(definition: FeatureDefinitionSpec): FeatureDescriptor =
        FeatureDescriptor(definition) { definition.requiredPermission in it.commerceCapabilities }
}
