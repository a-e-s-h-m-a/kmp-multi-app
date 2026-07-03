import SwiftUI

enum DeliveryFeatureModule {
    static let module = AnyCommerceFeatureModule(
        id: "delivery",
        tabSystemImage: "truck.box",
        viewFactory: { feature, store in
            AnyView(DeliveryFeatureContentView(feature: feature, store: store))
        }
    )
}

