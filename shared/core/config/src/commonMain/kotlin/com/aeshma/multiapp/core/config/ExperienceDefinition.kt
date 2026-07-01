package com.aeshma.multiapp.core.config

import com.aeshma.multiapp.core.model.AppId
import com.aeshma.multiapp.core.model.BusinessUnitId
import com.aeshma.multiapp.core.model.CommerceCapabilities
import com.aeshma.multiapp.core.model.ExperienceId
import com.aeshma.multiapp.core.model.RoleId

data class ExperienceDefinition(
    val id: ExperienceId,
    val hostAppId: AppId,
    val displayName: String,
    val url: String?,
    val logo: String?,
    val allowedSites: Set<String>,
    val theme: String,
    val supportedCapabilities: CommerceCapabilities,
    val supportedBusinessUnits: Set<BusinessUnitId>,
) {
    init {
        require(displayName.isNotBlank()) { "Experience display name cannot be blank." }
        require(theme.isNotBlank()) { "Experience theme cannot be blank." }
        require(supportedBusinessUnits.isNotEmpty()) { "Experience must support at least one business unit." }
    }
}

data class BusinessUnitDefinition(
    val id: BusinessUnitId,
    val allowedExperiences: Set<ExperienceId>,
    val allowedCapabilities: CommerceCapabilities,
) {
    init {
        require(allowedExperiences.isNotEmpty()) { "Business unit must allow at least one experience." }
    }
}

data class PermissionTemplate(
    val role: RoleId,
    val grantedCapabilities: CommerceCapabilities,
)
