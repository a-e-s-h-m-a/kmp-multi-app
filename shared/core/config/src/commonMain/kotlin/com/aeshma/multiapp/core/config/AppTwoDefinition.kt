package com.aeshma.multiapp.core.config

import com.aeshma.multiapp.core.model.AppId
import com.aeshma.multiapp.core.model.UserType

fun appTwoDefinition(): AppDefinition = AppDefinition(
    id = AppId.AppTwo,
    displayName = "AppTwo",
    configKey = "appTwo",
    defaultUsername = "admin",
    profiles = linkedMapOf(
        "admin" to UserProfile(UserType.Admin, CapabilityPresets.admin()),
        "merchant" to UserProfile(UserType.Merchant, CapabilityPresets.merchant()),
        "readonly" to UserProfile(UserType.Merchant, CapabilityPresets.readOnly()),
        "driver" to UserProfile(UserType.Merchant, CapabilityPresets.none()),
        "customer" to UserProfile(UserType.Merchant, CapabilityPresets.none()),
        "nod" to UserProfile(UserType.Merchant, CapabilityPresets.none()),
    ),
)

fun defaultAppDefinitions(): List<AppDefinition> =
    listOf(appOneDefinition(), appTwoDefinition())
