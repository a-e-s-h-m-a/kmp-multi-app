import SwiftUI

@main
struct SuperAppApp: App {
    private let store = MultiAppStoreFactory.makeProduct(
        productIdName: ProductFeatureBundles.superApp.productId,
        buildFeatureBundle: ProductFeatureBundles.superApp.bundledFeatures
    )

    var body: some Scene {
        WindowGroup {
            MultiAppRootView(store: store)
        }
    }
}
