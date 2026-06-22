import SwiftUI

@main
struct AppTwoApp: App {
    private let store = MultiAppStoreFactory.make(appIdName: "AppTwo")

    var body: some Scene {
        WindowGroup {
            MultiAppRootView(store: store)
        }
    }
}
