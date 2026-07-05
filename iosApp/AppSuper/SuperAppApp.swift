import SwiftUI

@main
struct SuperAppApp: App {
    private let store = MultiAppStoreFactory.makeProduct(
        productIdName: ProductFeatureBundles.superApp.productId,
        buildFeatureBundle: ProductFeatureBundles.superApp.bundledFeatures
    )
    private let featureRegistry = CommerceFeatureRegistry.superApp

    var body: some Scene {
        WindowGroup {
            MultiAppRootView(store: store, featureRegistry: featureRegistry)
        }
    }
}
