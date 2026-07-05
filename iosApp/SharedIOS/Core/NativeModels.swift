import Foundation

public enum NativeScreen: Equatable, Sendable {
    case login
    case experienceSwitcher
    case features
    case feature(String)
}

public enum NativeTheme: String, Equatable, Sendable {
    case boutique = "Boutique"
    case broadline = "Broadline"
    case operations = "Operations"
}

public struct NativeResolvedExperienceOption: Identifiable, Equatable, Sendable {
    public let id: String
    public let appId: String
    public let displayName: String
    public let theme: String
    public let allowedSites: [String]
    public let grantLabel: String
    public let businessUnitId: String
    public let roles: [String]
    public let resolvedPermissions: [String]
}

public extension NativeResolvedExperienceOption {
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

public struct NativeFeature: Identifiable, Equatable, Sendable {
    public let id: String
    public let title: String
    public let requiredPermission: String
    public let enabledTweaks: [String]
    public let permissionRows: [NativeFeaturePermissionRow]
    public let actions: [NativeFeatureAction]
    public let uiBlocks: [NativeFeatureUiBlock]
}

public struct NativeFeaturePermissionRow: Equatable, Sendable {
    public let label: String
    public let permission: String
    public let enabled: Bool
}

public struct NativeFeatureAction: Equatable, Sendable {
    public let label: String
    public let requiredPermission: String
    public let result: String
}

public struct NativeFeatureUiBlock: Equatable, Sendable {
    public let title: String
    public let requiredPermission: String
    public let body: String
}

public struct NativeDeliveryOrder: Identifiable, Equatable, Sendable {
    public let id: String
    public let title: String
    public var status: String
    public var actions: [String]
}

public struct NativeSessionSnapshot: Equatable, Sendable {
    public let userSummary: String
    public let availableFeatures: [NativeFeature]
    public let deliveryExperienceName: String
    public let deliveryOrders: [NativeDeliveryOrder]
}
