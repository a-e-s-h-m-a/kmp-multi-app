import SwiftUI

@main
struct AppTwoApp: App {
    private let store = MultiAppStoreFactory.makeProduct(
        productIdName: ProductFeatureBundles.appTwo.productId,
        buildFeatureBundle: ProductFeatureBundles.appTwo.bundledFeatures
    )
    private let featureRegistry = CommerceFeatureRegistry.appTwo

    var body: some Scene {
        WindowGroup {
            MultiAppRootView(store: store, featureRegistry: featureRegistry)
        }
    }
}
