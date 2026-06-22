import SwiftUI

@main
struct AppOneApp: App {
    private let store = MultiAppStoreFactory.make(appIdName: "AppOne")

    var body: some Scene {
        WindowGroup {
            MultiAppRootView(store: store)
        }
    }
}
