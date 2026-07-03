import SwiftUI

enum GenericCommerceFeatureModule {
    static func make(id: String, tabSystemImage: String) -> AnyCommerceFeatureModule {
        AnyCommerceFeatureModule(
            id: id,
            tabSystemImage: tabSystemImage,
            viewFactory: { feature, store in
                AnyView(GenericFeatureContentView(feature: feature, theme: store.selectedTheme))
            }
        )
    }
}

