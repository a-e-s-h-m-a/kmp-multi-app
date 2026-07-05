package com.aeshma.multiapp.application

import com.aeshma.multiapp.core.model.FeatureDefinitionSpec

internal expect fun platformFeatureDefinitions(): List<FeatureDefinitionSpec>
