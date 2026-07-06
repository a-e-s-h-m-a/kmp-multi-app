package com.aeshma.multiapp.application

import com.aeshma.multiapp.core.model.FeatureDefinitionSpec
import com.aeshma.multiapp.core.model.FeatureRuntimeContributor

internal actual fun platformFeatureDefinitions(): List<FeatureDefinitionSpec> = emptyList()

internal actual fun platformFeatureRuntimeContributors(): List<FeatureRuntimeContributor> = emptyList()
