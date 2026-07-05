#if SWIFT_PACKAGE
import SharedIOSCore
#endif
import SwiftUI

@MainActor
public enum DeliveryFeatureModule {
    public static let module = AnyCommerceFeatureModule(
        id: "delivery",
        tabSystemImage: "truck.box",
        viewFactory: { feature, store in
            AnyView(DeliveryFeatureContentView(feature: feature, store: store))
        }
    )
}
