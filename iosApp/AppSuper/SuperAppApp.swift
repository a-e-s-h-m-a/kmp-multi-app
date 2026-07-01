import SwiftUI

@main
struct SuperAppApp: App {
    private let store = MultiAppStoreFactory.makeProduct(productIdName: "SuperApp")

    var body: some Scene {
        WindowGroup {
            MultiAppRootView(store: store)
        }
    }
}
