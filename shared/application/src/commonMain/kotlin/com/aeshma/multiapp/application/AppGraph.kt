package com.aeshma.multiapp.application

import com.aeshma.multiapp.core.analytics.AnalyticsClient
import com.aeshma.multiapp.core.analytics.ConsoleAnalyticsClient
import com.aeshma.multiapp.core.config.AppCatalog
import com.aeshma.multiapp.core.config.AuthRepository
import com.aeshma.multiapp.core.config.LocalAuthRepository
import com.aeshma.multiapp.core.config.defaultAppDefinitions
import com.aeshma.multiapp.core.model.AppId
import com.aeshma.multiapp.core.model.FeatureDefinitionSpec
import com.aeshma.multiapp.feature.delivery.DeliveryPolicyResolver
import com.aeshma.multiapp.feature.delivery.DeliveryRepository
import com.aeshma.multiapp.feature.delivery.SampleDeliveryRepository
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory

@DependencyGraph
interface AppGraph {
    val appId: AppId
    val appCatalog: AppCatalog
    val featureDefinitions: List<FeatureDefinitionSpec>
    val session: AppSession

    @Provides
    fun provideAuthRepository(appCatalog: AppCatalog): AuthRepository =
        LocalAuthRepository(appCatalog)

    @Provides
    fun provideAnalyticsClient(): AnalyticsClient = ConsoleAnalyticsClient()

    @Provides
    fun provideFeatureRegistry(featureDefinitions: List<FeatureDefinitionSpec>): FeatureRegistry =
        FeatureRegistry(featureDefinitions)

    @Provides
    fun provideDeliveryPolicyResolver(): DeliveryPolicyResolver = DeliveryPolicyResolver()

    @Provides
    fun provideDeliveryRepository(): DeliveryRepository = SampleDeliveryRepository()

    @Provides
    fun provideAppSession(
        authRepository: AuthRepository,
        featureRegistry: FeatureRegistry,
        deliveryPolicyResolver: DeliveryPolicyResolver,
        deliveryRepository: DeliveryRepository,
        analyticsClient: AnalyticsClient,
    ): AppSession = AppSession(
        authRepository = authRepository,
        featureRegistry = featureRegistry,
        deliveryPolicyResolver = deliveryPolicyResolver,
        deliveryRepository = deliveryRepository,
        analyticsClient = analyticsClient,
    )

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides appId: AppId,
            @Provides appCatalog: AppCatalog,
            @Provides featureDefinitions: List<FeatureDefinitionSpec>,
        ): AppGraph
    }
}

class AppRuntime internal constructor(graph: AppGraph) {
    val appId: AppId = graph.appCatalog.definition(graph.appId).id
    val appDefinition = graph.appCatalog.definition(appId)
    val session: AppSession = graph.session
}

fun createAppRuntime(
    appId: AppId,
    appCatalog: AppCatalog = AppCatalog(defaultAppDefinitions()),
    featureDefinitions: List<FeatureDefinitionSpec> = emptyList(),
): AppRuntime =
    AppRuntime(createGraphFactory<AppGraph.Factory>().create(appId, appCatalog, featureDefinitions))
