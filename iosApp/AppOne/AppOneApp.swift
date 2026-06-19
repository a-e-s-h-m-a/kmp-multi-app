import SwiftUI

@main
struct AppOneApp: App {
    var body: some Scene {
        WindowGroup {
            MultiAppRootView(appIdName: "AppOne")
        }
    }
}
