package com.aeshma.multiapp.core.model

data class FeatureId(val value: String) {
    init {
        require(value.isNotBlank()) { "Feature id cannot be blank." }
    }

    companion object {
        val Home = FeatureId("home")
        val Delivery = FeatureId("delivery")
        val Profile = FeatureId("profile")
        val Reports = FeatureId("reports")
        val Payments = FeatureId("payments")
    }
}
