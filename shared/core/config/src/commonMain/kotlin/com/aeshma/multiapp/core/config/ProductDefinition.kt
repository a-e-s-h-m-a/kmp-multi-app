package com.aeshma.multiapp.core.config

import com.aeshma.multiapp.core.model.AppId
import com.aeshma.multiapp.core.model.ProductId

data class ProductDefinition(
    val id: ProductId,
    val displayName: String,
    val supportedExperiences: Set<AppId>,
    val defaultExperience: AppId?,
    val showsExperiencePicker: Boolean,
) {
    init {
        require(displayName.isNotBlank()) { "Display name cannot be blank." }
        require(supportedExperiences.isNotEmpty()) { "At least one experience must be supported." }
        require(defaultExperience == null || defaultExperience in supportedExperiences) {
            "Default experience must be one of the supported experiences."
        }
        require(showsExperiencePicker || defaultExperience != null) {
            "Products without an experience picker must define a default experience."
        }
    }
}
