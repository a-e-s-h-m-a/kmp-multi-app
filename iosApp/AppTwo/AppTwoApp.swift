import SwiftUI

@main
struct AppTwoApp: App {
    var body: some Scene {
        WindowGroup {
            MultiAppRootView(appIdName: "AppTwo")
        }
    }
}
