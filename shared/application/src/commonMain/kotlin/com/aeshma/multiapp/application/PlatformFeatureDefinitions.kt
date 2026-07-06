package com.aeshma.multiapp.application

import com.aeshma.multiapp.core.model.FeatureDefinitionSpec
import com.aeshma.multiapp.core.model.FeatureRuntimeContributor

internal expect fun platformFeatureDefinitions(): List<FeatureDefinitionSpec>

internal expect fun platformFeatureRuntimeContributors(): List<FeatureRuntimeContributor>
