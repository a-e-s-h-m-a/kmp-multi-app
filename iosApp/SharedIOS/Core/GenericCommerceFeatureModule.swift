import SwiftUI

@MainActor
public enum GenericCommerceFeatureModule {
    public static func make(id: String, tabSystemImage: String) -> AnyCommerceFeatureModule {
        AnyCommerceFeatureModule(
            id: id,
            tabSystemImage: tabSystemImage,
            viewFactory: { feature, store in
                AnyView(GenericFeatureContentView(feature: feature, theme: store.selectedTheme))
            }
        )
    }
}
