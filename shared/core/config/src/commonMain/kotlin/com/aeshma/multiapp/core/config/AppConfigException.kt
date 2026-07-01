package com.aeshma.multiapp.core.config

import com.aeshma.multiapp.core.model.AppId
import com.aeshma.multiapp.core.model.BusinessUnitId
import com.aeshma.multiapp.core.model.ExperienceId
import com.aeshma.multiapp.core.model.ProductId

sealed class AppConfigException(message: String) : IllegalArgumentException(message)

class UnknownAppException(appId: AppId) :
    AppConfigException("No app definition is registered for '${appId.externalName}'.")

class UnknownProductException(productId: ProductId) :
    AppConfigException("No product definition is registered for '${productId.externalName}'.")

class UnknownExperienceException(experienceId: ExperienceId) :
    AppConfigException("No experience definition is registered for '${experienceId.value}'.")

class UnknownBusinessUnitException(businessUnitId: BusinessUnitId) :
    AppConfigException("No business unit definition is registered for '${businessUnitId.value}'.")

class UnsupportedExperienceException(productId: ProductId, experienceId: ExperienceId) :
    AppConfigException("'${experienceId.value}' is not supported by product '${productId.externalName}'.")

class UnknownProfileException(appId: AppId, username: String) :
    AppConfigException("User '$username' is not configured for '${appId.externalName}'.")
