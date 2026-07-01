package com.aeshma.multiapp.core.model

data class BusinessUnitId(val value: String) {
    init {
        require(value.isNotBlank()) { "Business unit id cannot be blank." }
    }

    companion object {
        val SSMG = BusinessUnitId("SSMG")
        val USBL = BusinessUnitId("USBL")
        val CABL = BusinessUnitId("CABL")

        fun fromExternalName(value: String): BusinessUnitId = BusinessUnitId(value.trim().uppercase())
    }
}

data class PermissionId(val value: String) {
    init {
        require(value.isNotBlank()) { "Permission id cannot be blank." }
    }

    companion object {
        val OrdersView = PermissionId("orders.view")
        val OrdersEdit = PermissionId("orders.edit")
        val OrdersNotifications = PermissionId("orders.notifications")
        val ListsView = PermissionId("lists.view")
        val ListsEdit = PermissionId("lists.edit")
        val ListsPurchaseHistory = PermissionId("lists.purchaseHistory")
        val CatalogView = PermissionId("catalog.view")
        val CatalogRecommendations = PermissionId("catalog.recommendations")
        val PdpView = PermissionId("pdp.view")
        val PdpInternalDetails = PermissionId("pdp.internalDetails")
        val DeliveryView = PermissionId("delivery.view")
        val DeliveryEdit = PermissionId("delivery.edit")
        val DeliveryProgress = PermissionId("delivery.progress")
        val DeliveryStatus = PermissionId("delivery.status")
        val DeliveryMap = PermissionId("delivery.map")
        val DeliveryInvoices = PermissionId("delivery.invoices")
    }
}

data class RoleId(val value: String) {
    init {
        require(value.isNotBlank()) { "Role id cannot be blank." }
    }

    companion object {
        val Customer = RoleId("CUSTOMER")
        val CustomerAdmin = RoleId("CUSTOMER_ADMIN")
        val DemoCustomer = RoleId("DEMO_CUSTOMER")
        val DeliveryUser = RoleId("DELIVERY_USER")
        val SalesAssociate = RoleId("SA")
        val CustomerServiceRepresentative = RoleId("CSR")
        val InternalUser = RoleId("INTERNAL_USER")
        val CsrOpco = RoleId("CSR_OPCO")
    }
}

data class CommerceCapabilities(
    val permissions: Set<PermissionId> = emptySet(),
) {
    operator fun contains(permission: PermissionId): Boolean = permission in permissions

    fun has(permission: PermissionId): Boolean = permission in permissions

    fun plus(other: CommerceCapabilities): CommerceCapabilities =
        CommerceCapabilities(permissions + other.permissions)

    fun intersect(other: CommerceCapabilities): CommerceCapabilities =
        CommerceCapabilities(permissions.intersect(other.permissions))

    companion object {
        fun of(vararg permission: PermissionId): CommerceCapabilities =
            CommerceCapabilities(permission.toSet())

        fun of(vararg permission: String): CommerceCapabilities =
            CommerceCapabilities(permission.map(::PermissionId).toSet())

        fun none(): CommerceCapabilities = CommerceCapabilities()
    }
}
