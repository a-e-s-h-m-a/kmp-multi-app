import SwiftUI

@main
struct AppTwoApp: App {
    private let store = MultiAppStoreFactory.makeProduct(productIdName: "AppTwoStandalone")

    var body: some Scene {
        WindowGroup {
            MultiAppRootView(store: store)
        }
    }
}
