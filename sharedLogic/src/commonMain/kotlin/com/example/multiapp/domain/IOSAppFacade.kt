package com.example.multiapp.domain

data class IOSDeliveryOrderSnapshot(
    val id: String,
    val title: String,
    val status: String,
    val actions: List<String>,
)

data class IOSSessionSnapshot(
    val userSummary: String,
    val availableFeatures: List<String>,
    val deliveryExperienceName: String,
    val deliveryOrders: List<IOSDeliveryOrderSnapshot>,
)

class IOSAppFacade(appIdName: String) {
    private val root = IOSAppCompositionRoot(AppId.fromExternalName(appIdName))
    private val session = root.session

    val appName: String = root.shared.appDefinition.displayName
    val defaultUsername: String = root.shared.appDefinition.defaultUsername

    suspend fun login(username: String): IOSSessionSnapshot {
        val context = session.login(root.shared.appId, username)
        val policy = session.deliveryPolicy()

        return IOSSessionSnapshot(
            userSummary = "${context.userId} (${context.userType.name})",
            availableFeatures = session.availableFeatures().map(FeatureDescriptor::title),
            deliveryExperienceName = policy.experienceName,
            deliveryOrders = session.sampleDeliveryOrders().map { order ->
                IOSDeliveryOrderSnapshot(
                    id = order.id,
                    title = order.title,
                    status = order.status.name,
                    actions = policy.availableActions(order).map(DeliveryAction::name),
                )
            },
        )
    }
}
