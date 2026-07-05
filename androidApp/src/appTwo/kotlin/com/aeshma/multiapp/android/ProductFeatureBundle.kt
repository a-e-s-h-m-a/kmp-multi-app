package com.aeshma.multiapp.android

import com.aeshma.multiapp.core.model.FeatureDefinitionSpec
import com.aeshma.multiapp.feature.delivery.DeliveryFeature
import com.aeshma.multiapp.feature.lists.ListsFeature
import com.aeshma.multiapp.feature.orders.OrdersFeature

object ProductFeatureBundle {
    val featureDefinitions: List<FeatureDefinitionSpec> = listOf(
        OrdersFeature.definition,
        ListsFeature.definition,
        DeliveryFeature.definition,
    )
}
