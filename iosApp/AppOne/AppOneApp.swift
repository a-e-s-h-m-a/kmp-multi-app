import SwiftUI

@main
struct AppOneApp: App {
    private let store = MultiAppStoreFactory.makeProduct(
        productIdName: ProductFeatureBundles.appOne.productId,
        buildFeatureBundle: ProductFeatureBundles.appOne.bundledFeatures
    )

    var body: some Scene {
        WindowGroup {
            MultiAppRootView(store: store)
        }
    }
}
