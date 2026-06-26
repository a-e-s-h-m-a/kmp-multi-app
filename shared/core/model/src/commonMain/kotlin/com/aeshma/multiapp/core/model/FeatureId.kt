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
