import ComposableArchitecture
import SwiftUI

@MainActor
public protocol CommerceFeatureModule {
    var id: String { get }
    var tabSystemImage: String { get }

    func makeView(feature: NativeFeature, store: StoreOf<MultiAppFeature>) -> AnyView
}

@MainActor
public struct AnyCommerceFeatureModule: CommerceFeatureModule {
    public let id: String
    public let tabSystemImage: String
    private let viewFactory: (NativeFeature, StoreOf<MultiAppFeature>) -> AnyView

    public init(
        id: String,
        tabSystemImage: String,
        viewFactory: @escaping (NativeFeature, StoreOf<MultiAppFeature>) -> AnyView
    ) {
        self.id = id
        self.tabSystemImage = tabSystemImage
        self.viewFactory = viewFactory
    }

    public func makeView(feature: NativeFeature, store: StoreOf<MultiAppFeature>) -> AnyView {
        viewFactory(feature, store)
    }
}

@MainActor
public struct CommerceFeatureRegistry {
    private let modules: [String: AnyCommerceFeatureModule]

    public init(modules: [AnyCommerceFeatureModule]) {
        self.modules = Dictionary(uniqueKeysWithValues: modules.map { ($0.id, $0) })
    }

    public func module(for feature: NativeFeature) -> AnyCommerceFeatureModule {
        modules[feature.id] ?? GenericCommerceFeatureModule.make(id: feature.id, tabSystemImage: "square")
    }
}
