import Foundation

enum NativeScreen: Equatable, Sendable {
    case login
    case features
    case feature(String)
}

struct NativeFeature: Identifiable, Equatable, Sendable {
    let id: String
    let title: String
    let requiredPermission: String
    let enabledTweaks: [String]
}

struct NativeDeliveryOrder: Identifiable, Equatable, Sendable {
    let id: String
    let title: String
    let status: String
    let actions: [String]
}

struct NativeSessionSnapshot: Equatable, Sendable {
    let userSummary: String
    let availableFeatures: [NativeFeature]
    let deliveryExperienceName: String
    let deliveryOrders: [NativeDeliveryOrder]
}
