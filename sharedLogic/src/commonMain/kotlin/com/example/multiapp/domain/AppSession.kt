package com.example.multiapp.domain

class AppSession(
    private val authRepository: AuthRepository,
    private val featureRegistry: FeatureRegistry,
    private val deliveryPolicyResolver: DeliveryPolicyResolver,
    private val analyticsClient: AppAwareAnalyticsClient,
) {
    var currentContext: AppContext? = null
        private set

    suspend fun login(appId: AppId, username: String): AppContext {
        val context = authRepository.login(appId, username)
        currentContext = context
        analyticsClient.track(
            AnalyticsEvent(
                name = "login_completed",
                properties = mapOf("username" to username),
            ),
        )
        return context
    }

    fun availableFeatures(): List<FeatureDescriptor> =
        featureRegistry.availableFeatures(requireContext())

    fun deliveryPolicy(): DeliveryPolicy =
        deliveryPolicyResolver.resolve(requireContext())

    fun sampleDeliveryOrders(): List<DeliveryOrder> =
        listOf(
            DeliveryOrder(id = "DEL-1001", title = "Grocery drop-off", status = DeliveryStatus.Created),
            DeliveryOrder(id = "DEL-1002", title = "Pharmacy pickup", status = DeliveryStatus.Assigned),
            DeliveryOrder(id = "DEL-1003", title = "Cafe order", status = DeliveryStatus.PickedUp),
            DeliveryOrder(id = "DEL-1004", title = "Office lunch", status = DeliveryStatus.Delivered),
            DeliveryOrder(id = "DEL-1005", title = "Cancelled parcel", status = DeliveryStatus.Cancelled),
        )

    fun trackFeatureOpened(featureId: FeatureId) {
        analyticsClient.trackFeature(
            featureId = featureId,
            name = "${featureId.name.lowercase()}_screen_opened",
        )
    }

    private fun requireContext(): AppContext =
        currentContext ?: error("Call login before requesting session data.")
}

class AppCompositionRoot(
    val appId: AppId,
    private val appCatalog: AppCatalog = AppCatalog.default(),
) {
    val appDefinition: AppDefinition = appCatalog.definition(appId)
    private val networkClient: NetworkClient = FakeNetworkClient()
    private val authRepository: FakeAuthRepository = FakeAuthRepository(networkClient, appCatalog)
    private val featureRegistry: FeatureRegistry = FeatureRegistry.default()
    private val deliveryPolicyResolver: DeliveryPolicyResolver = DeliveryPolicyResolver()
    private val consoleAnalyticsClient = ConsoleAnalyticsClient()
    private val analyticsClient = AppAwareAnalyticsClient(
        delegate = consoleAnalyticsClient,
        contextProvider = { session.currentContext },
    )

    val session: AppSession by lazy {
        AppSession(
            authRepository = authRepository,
            featureRegistry = featureRegistry,
            deliveryPolicyResolver = deliveryPolicyResolver,
            analyticsClient = analyticsClient,
        )
    }

    fun analyticsEvents(): List<AnalyticsEvent> =
        consoleAnalyticsClient.trackedEvents()
}

class AndroidAppCompositionRoot(appId: AppId) {
    val shared = AppCompositionRoot(appId)
    val session: AppSession = shared.session
}

class IOSAppCompositionRoot(appId: AppId) {
    val shared = AppCompositionRoot(appId)
    val session: AppSession = shared.session
}
