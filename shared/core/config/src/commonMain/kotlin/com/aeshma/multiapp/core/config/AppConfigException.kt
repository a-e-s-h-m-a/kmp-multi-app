package com.aeshma.multiapp.core.config

import com.aeshma.multiapp.core.model.AppId

sealed class AppConfigException(message: String) : IllegalArgumentException(message)

class UnknownAppException(appId: AppId) :
    AppConfigException("No app definition is registered for '${appId.externalName}'.")

class UnknownProfileException(appId: AppId, username: String) :
    AppConfigException("User '$username' is not configured for '${appId.externalName}'.")
