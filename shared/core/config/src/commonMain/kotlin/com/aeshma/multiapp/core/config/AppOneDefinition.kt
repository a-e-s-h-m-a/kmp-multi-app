package com.aeshma.multiapp.core.config

import com.aeshma.multiapp.core.model.AppId
import com.aeshma.multiapp.core.model.UserType

fun appOneDefinition(): AppDefinition = AppDefinition(
    id = AppId.AppOne,
    displayName = "AppOne",
    configKey = "appOne",
    defaultUsername = "customer",
    profiles = linkedMapOf(
        "customer" to UserProfile(UserType.Customer, CapabilityPresets.customer()),
        "driver" to UserProfile(UserType.Driver, CapabilityPresets.driver()),
        "readonly" to UserProfile(UserType.Customer, CapabilityPresets.readOnly()),
        "admin" to UserProfile(UserType.Customer, CapabilityPresets.none()),
        "merchant" to UserProfile(UserType.Customer, CapabilityPresets.none()),
        "nod" to UserProfile(UserType.Customer, CapabilityPresets.none()),
    ),
)
