import SwiftUI

@main
struct AppOneApp: App {
    private let store = MultiAppStoreFactory.makeProduct(
        productIdName: ProductFeatureBundles.appOne.productId,
        buildFeatureBundle: ProductFeatureBundles.appOne.bundledFeatures
    )
    private let featureRegistry = CommerceFeatureRegistry.appOne

    var body: some Scene {
        WindowGroup {
            MultiAppRootView(store: store, featureRegistry: featureRegistry)
        }
    }
}
