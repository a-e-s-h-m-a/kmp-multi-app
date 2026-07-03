import ComposableArchitecture
import SwiftUI

protocol CommerceFeatureModule {
    var id: String { get }
    var tabSystemImage: String { get }

    func makeView(feature: NativeFeature, store: StoreOf<MultiAppFeature>) -> AnyView
}

struct AnyCommerceFeatureModule: CommerceFeatureModule {
    let id: String
    let tabSystemImage: String
    private let viewFactory: (NativeFeature, StoreOf<MultiAppFeature>) -> AnyView

    init(
        id: String,
        tabSystemImage: String,
        viewFactory: @escaping (NativeFeature, StoreOf<MultiAppFeature>) -> AnyView
    ) {
        self.id = id
        self.tabSystemImage = tabSystemImage
        self.viewFactory = viewFactory
    }

    func makeView(feature: NativeFeature, store: StoreOf<MultiAppFeature>) -> AnyView {
        viewFactory(feature, store)
    }
}

struct CommerceFeatureRegistry {
    private let modules: [String: AnyCommerceFeatureModule]

    init(modules: [AnyCommerceFeatureModule]) {
        self.modules = Dictionary(uniqueKeysWithValues: modules.map { ($0.id, $0) })
    }

    func module(for feature: NativeFeature) -> AnyCommerceFeatureModule {
        modules[feature.id] ?? GenericCommerceFeatureModule.make(id: feature.id, tabSystemImage: "square")
    }
}

extension CommerceFeatureRegistry {
    static let shared = CommerceFeatureRegistry(
        modules: [
            OrdersFeatureModule.module,
            ListsFeatureModule.module,
            CatalogFeatureModule.module,
            ProductDetailsFeatureModule.module,
            DeliveryFeatureModule.module,
        ]
    )
}

