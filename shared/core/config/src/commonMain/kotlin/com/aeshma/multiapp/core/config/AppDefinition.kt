package com.aeshma.multiapp.core.config

import com.aeshma.multiapp.core.model.AppId
import com.aeshma.multiapp.core.model.UserCapabilities
import com.aeshma.multiapp.core.model.UserType

data class UserProfile(
    val userType: UserType,
    val capabilities: UserCapabilities,
)

data class AppDefinition(
    val id: AppId,
    val displayName: String,
    val configKey: String,
    val defaultUsername: String,
    val profiles: Map<String, UserProfile>,
) {
    private val normalizedProfiles = profiles.mapKeys { (username, _) -> username.normalizedUsername() }

    init {
        require(displayName.isNotBlank()) { "Display name cannot be blank." }
        require(configKey.isNotBlank()) { "Config key cannot be blank." }
        require(normalizedProfiles.size == profiles.size) { "Profile usernames must be unique." }
        require(defaultUsername.normalizedUsername() in normalizedProfiles) {
            "Default user '$defaultUsername' is not configured for ${id.externalName}."
        }
    }

    val supportedUsernames: List<String>
        get() = normalizedProfiles.keys.toList()

    fun profileFor(username: String): UserProfile =
        normalizedProfiles[username.normalizedUsername()]
            ?: throw UnknownProfileException(id, username)
}

internal fun String.normalizedUsername(): String = trim().lowercase()
