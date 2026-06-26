package com.aeshma.multiapp.application

import com.aeshma.multiapp.core.model.AppContext
import com.aeshma.multiapp.core.model.FeatureId
import com.aeshma.multiapp.core.model.PermissionId

class FeatureDescriptor(
    val id: FeatureId,
    val title: String,
    val requiredPermission: PermissionId,
    val tweakPermissions: List<PermissionId> = emptyList(),
    private val availability: (AppContext) -> Boolean,
) {
    fun isAvailable(context: AppContext): Boolean = availability(context)

    fun enabledTweaks(context: AppContext): List<PermissionId> =
        tweakPermissions.filter(context.commerceCapabilities::has)
}

class FeatureRegistry {
    private val features = listOf(
        FeatureDescriptor(
            id = FeatureId.Orders,
            title = "Orders",
            requiredPermission = PermissionId.OrdersView,
            tweakPermissions = listOf(
                PermissionId.OrdersEdit,
                PermissionId.OrdersNotifications,
            ),
        ) { PermissionId.OrdersView in it.commerceCapabilities },
        FeatureDescriptor(
            id = FeatureId.Lists,
            title = "Lists",
            requiredPermission = PermissionId.ListsView,
            tweakPermissions = listOf(
                PermissionId.ListsEdit,
                PermissionId.ListsPurchaseHistory,
            ),
        ) { PermissionId.ListsView in it.commerceCapabilities },
        FeatureDescriptor(
            id = FeatureId.Catalog,
            title = "Catalog",
            requiredPermission = PermissionId.CatalogView,
            tweakPermissions = listOf(PermissionId.CatalogRecommendations),
        ) { PermissionId.CatalogView in it.commerceCapabilities },
        FeatureDescriptor(
            id = FeatureId.ProductDetails,
            title = "Product Details",
            requiredPermission = PermissionId.PdpView,
            tweakPermissions = listOf(PermissionId.PdpInternalDetails),
        ) { PermissionId.PdpView in it.commerceCapabilities },
        FeatureDescriptor(
            id = FeatureId.Delivery,
            title = "Delivery",
            requiredPermission = PermissionId.DeliveryView,
            tweakPermissions = listOf(
                PermissionId.DeliveryEdit,
                PermissionId.DeliveryProgress,
                PermissionId.DeliveryStatus,
                PermissionId.DeliveryMap,
                PermissionId.DeliveryInvoices,
            ),
        ) { PermissionId.DeliveryView in it.commerceCapabilities },
    )

    fun availableFeatures(context: AppContext): List<FeatureDescriptor> =
        features.filter { it.isAvailable(context) }
}
