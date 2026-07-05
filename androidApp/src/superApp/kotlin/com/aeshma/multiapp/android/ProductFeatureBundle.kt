package com.aeshma.multiapp.android

import com.aeshma.multiapp.core.model.FeatureDefinitionSpec
import com.aeshma.multiapp.feature.catalog.CatalogFeature
import com.aeshma.multiapp.feature.delivery.DeliveryFeature
import com.aeshma.multiapp.feature.lists.ListsFeature
import com.aeshma.multiapp.feature.orders.OrdersFeature
import com.aeshma.multiapp.feature.productdetails.ProductDetailsFeature

object ProductFeatureBundle {
    val featureDefinitions: List<FeatureDefinitionSpec> = listOf(
        OrdersFeature.definition,
        ListsFeature.definition,
        CatalogFeature.definition,
        ProductDetailsFeature.definition,
        DeliveryFeature.definition,
    )
}
