package com.aeshma.multiapp.core.config

import com.aeshma.multiapp.core.model.AppId
import com.aeshma.multiapp.core.model.ProductId

sealed class AppConfigException(message: String) : IllegalArgumentException(message)

class UnknownAppException(appId: AppId) :
    AppConfigException("No app definition is registered for '${appId.externalName}'.")

class UnknownProductException(productId: ProductId) :
    AppConfigException("No product definition is registered for '${productId.externalName}'.")

class UnsupportedExperienceException(productId: ProductId, appId: AppId) :
    AppConfigException("'${appId.externalName}' is not supported by product '${productId.externalName}'.")

class UnknownProfileException(appId: AppId, username: String) :
    AppConfigException("User '$username' is not configured for '${appId.externalName}'.")
