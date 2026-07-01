import SwiftUI

@main
struct AppOneApp: App {
    private let store = MultiAppStoreFactory.makeProduct(productIdName: "AppOneStandalone")

    var body: some Scene {
        WindowGroup {
            MultiAppRootView(store: store)
        }
    }
}
