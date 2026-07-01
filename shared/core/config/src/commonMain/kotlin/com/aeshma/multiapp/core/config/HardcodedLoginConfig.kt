package com.aeshma.multiapp.core.config

import com.aeshma.multiapp.core.model.BusinessUnitId
import com.aeshma.multiapp.core.model.CommerceCapabilities
import com.aeshma.multiapp.core.model.DeliveryCapability
import com.aeshma.multiapp.core.model.DeliveryMode
import com.aeshma.multiapp.core.model.PermissionId
import com.aeshma.multiapp.core.model.ProductId
import com.aeshma.multiapp.core.model.ReportsCapability
import com.aeshma.multiapp.core.model.RoleId
import com.aeshma.multiapp.core.model.UserCapabilities
import com.aeshma.multiapp.core.model.UserType
import kotlin.random.Random

data class SimulatedLoginGrant(
    val id: String,
    val label: String,
    val businessUnitId: BusinessUnitId,
    val userType: UserType,
    val roles: Set<RoleId>,
    val explicitPermissions: Set<PermissionId>,
    val capabilities: UserCapabilities,
)

object HardcodedLoginConfig {
    private val grants = listOf(
        SimulatedLoginGrant(
            id = "boutique-admin",
            label = "Boutique Admin",
            businessUnitId = BusinessUnitId.SSMG,
            userType = UserType.Admin,
            roles = setOf(RoleId.CustomerAdmin, RoleId.InternalUser),
            explicitPermissions = setOf(PermissionId.PdpInternalDetails),
            capabilities = UserCapabilities(
                delivery = DeliveryCapability(
                    mode = DeliveryMode.Admin,
                    canCancelDelivery = true,
                    canEditAddress = true,
                    showLiveTracking = true,
                ),
                reports = ReportsCapability(
                    canViewReports = true,
                    canViewGlobalReports = false,
                    canViewMerchantReports = true,
                ),
            ),
        ),
        SimulatedLoginGrant(
            id = "broadline-operator",
            label = "Broadline Operator",
            businessUnitId = BusinessUnitId.USBL,
            userType = UserType.Merchant,
            roles = setOf(RoleId.CustomerAdmin, RoleId.CustomerServiceRepresentative),
            explicitPermissions = setOf(PermissionId.DeliveryMap, PermissionId.DeliveryInvoices),
            capabilities = UserCapabilities(
                delivery = DeliveryCapability(
                    mode = DeliveryMode.Merchant,
                    showLiveTracking = true,
                ),
                reports = ReportsCapability(
                    canViewReports = true,
                    canViewGlobalReports = true,
                    canViewMerchantReports = false,
                ),
            ),
        ),
        SimulatedLoginGrant(
            id = "boutique-customer",
            label = "Boutique Buyer",
            businessUnitId = BusinessUnitId.SSMG,
            userType = UserType.Customer,
            roles = setOf(RoleId.Customer, RoleId.DemoCustomer),
            explicitPermissions = setOf(PermissionId.DeliveryStatus),
            capabilities = UserCapabilities(
                delivery = DeliveryCapability(
                    mode = DeliveryMode.Customer,
                    canCancelDelivery = true,
                    showLiveTracking = false,
                ),
            ),
        ),
        SimulatedLoginGrant(
            id = "broadline-driver",
            label = "Broadline Driver",
            businessUnitId = BusinessUnitId.USBL,
            userType = UserType.Driver,
            roles = setOf(RoleId.DeliveryUser),
            explicitPermissions = setOf(
                PermissionId.DeliveryView,
                PermissionId.DeliveryProgress,
                PermissionId.DeliveryStatus,
                PermissionId.DeliveryMap,
            ),
            capabilities = UserCapabilities(
                delivery = DeliveryCapability(
                    mode = DeliveryMode.Driver,
                    canAcceptDelivery = true,
                    canMarkPickedUp = true,
                    canMarkDelivered = true,
                    showLiveTracking = true,
                ),
            ),
        ),
        SimulatedLoginGrant(
            id = "canada-admin",
            label = "Canada Admin",
            businessUnitId = BusinessUnitId.CABL,
            userType = UserType.Admin,
            roles = setOf(RoleId.CustomerAdmin, RoleId.CsrOpco),
            explicitPermissions = setOf(
                PermissionId.OrdersNotifications,
                PermissionId.PdpInternalDetails,
                PermissionId.DeliveryInvoices,
            ),
            capabilities = UserCapabilities(
                delivery = DeliveryCapability(
                    mode = DeliveryMode.Admin,
                    canCancelDelivery = true,
                    canEditAddress = true,
                    showLiveTracking = true,
                ),
                reports = ReportsCapability(
                    canViewReports = true,
                    canViewGlobalReports = true,
                    canViewMerchantReports = true,
                ),
            ),
        ),
    )

    fun loginGrants(productId: ProductId, username: String): List<SimulatedLoginGrant> {
        val normalizedUsername = username.trim().lowercase()
        val seed = normalizedUsername.fold(17) { hash, char -> hash * 31 + char.code }
        val random = Random(seed)
        return when (productId) {
            ProductId.AppOneStandalone -> listOf(grants.randomFor(BusinessUnitId.USBL, random))
            ProductId.AppTwoStandalone -> listOf(grants.randomFor(BusinessUnitId.SSMG, random))
            ProductId.SuperApp -> listOf(
                grants.randomFor(
                    businessUnitId = if (normalizedUsername == "admin") {
                        BusinessUnitId.CABL
                    } else {
                        listOf(BusinessUnitId.SSMG, BusinessUnitId.USBL, BusinessUnitId.CABL).random(random)
                    },
                    random = random,
                ),
            )
            else -> listOf(
                grants.randomFor(
                    businessUnitId = listOf(BusinessUnitId.SSMG, BusinessUnitId.USBL, BusinessUnitId.CABL).random(random),
                    random = random,
                ),
            )
        }
    }

    private fun List<SimulatedLoginGrant>.randomFor(
        businessUnitId: BusinessUnitId,
        random: Random,
    ): SimulatedLoginGrant = filter { it.businessUnitId == businessUnitId }.random(random)
}

fun SimulatedLoginGrant.explicitCommerceCapabilities(): CommerceCapabilities =
    CommerceCapabilities(explicitPermissions)
