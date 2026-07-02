import Foundation

enum NativeScreen: Equatable, Sendable {
    case login
    case experienceSwitcher
    case features
    case feature(String)
}

enum NativeTheme: String, Equatable, Sendable {
    case boutique = "Boutique"
    case broadline = "Broadline"
    case operations = "Operations"
}

struct NativeResolvedExperienceOption: Identifiable, Equatable, Sendable {
    let id: String
    let appId: String
    let displayName: String
    let theme: String
    let allowedSites: [String]
    let grantLabel: String
    let businessUnitId: String
    let roles: [String]
    let resolvedPermissions: [String]
}

extension NativeResolvedExperienceOption {
    var defaultTheme: NativeTheme {
        if theme.localizedCaseInsensitiveContains("boutique") || displayName == "Newport&Buckhead" {
            .boutique
        } else if theme.localizedCaseInsensitiveContains("broadline") || displayName == "Shop" {
            .broadline
        } else {
            .operations
        }
    }
}

struct NativeFeature: Identifiable, Equatable, Sendable {
    let id: String
    let title: String
    let requiredPermission: String
    let enabledTweaks: [String]
    let permissionRows: [NativeFeaturePermissionRow]
    let actions: [NativeFeatureAction]
    let uiBlocks: [NativeFeatureUiBlock]
}

struct NativeFeaturePermissionRow: Equatable, Sendable {
    let label: String
    let permission: String
    let enabled: Bool
}

struct NativeFeatureAction: Equatable, Sendable {
    let label: String
    let requiredPermission: String
    let result: String
}

struct NativeFeatureUiBlock: Equatable, Sendable {
    let title: String
    let requiredPermission: String
    let body: String
}

struct NativeDeliveryOrder: Identifiable, Equatable, Sendable {
    let id: String
    let title: String
    var status: String
    var actions: [String]
}

struct NativeSessionSnapshot: Equatable, Sendable {
    let userSummary: String
    let availableFeatures: [NativeFeature]
    let deliveryExperienceName: String
    let deliveryOrders: [NativeDeliveryOrder]
}
