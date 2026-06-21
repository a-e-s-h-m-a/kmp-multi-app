import ComposableArchitecture
import Foundation

struct MultiAppClient: Sendable {
    let appName: String
    let defaultUsername: String
    let supportedUsernames: [String]
    var login: @Sendable (_ username: String) async throws -> NativeSessionSnapshot
    var logout: @Sendable () async -> Void
}

private enum MultiAppClientKey: DependencyKey {
    static let liveValue = MultiAppClient(
        appName: "Unconfigured",
        defaultUsername: "",
        supportedUsernames: [],
        login: { _ in throw MultiAppClientError.notConfigured },
        logout: {}
    )
}

extension DependencyValues {
    var multiAppClient: MultiAppClient {
        get { self[MultiAppClientKey.self] }
        set { self[MultiAppClientKey.self] = newValue }
    }
}

private enum MultiAppClientError: LocalizedError {
    case notConfigured

    var errorDescription: String? {
        "MultiAppClient must be configured at the iOS composition root."
    }
}
