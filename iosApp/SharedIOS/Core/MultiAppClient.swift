import ComposableArchitecture
import Foundation

struct MultiAppClient: Sendable {
    let appName: String
    let defaultUsername: String
    let supportedUsernames: [String]
    var resolveExperienceOptions: @Sendable (_ username: String) async throws -> [NativeResolvedExperienceOption]
    var launchExperience: @Sendable (_ optionId: String, _ username: String) async throws -> NativeSessionSnapshot
    var login: @Sendable (_ username: String) async throws -> NativeSessionSnapshot
    var logout: @Sendable () async -> Void
}

private enum MultiAppClientKey: DependencyKey {
    static let liveValue = MultiAppClient(
        appName: "Unconfigured",
        defaultUsername: "",
        supportedUsernames: [],
        resolveExperienceOptions: { _ in throw MultiAppClientError.notConfigured },
        launchExperience: { _, _ in throw MultiAppClientError.notConfigured },
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

enum MultiAppClientError: LocalizedError {
    case notConfigured
    case noResolvedExperience

    var errorDescription: String? {
        switch self {
        case .notConfigured:
            "MultiAppClient must be configured at the iOS composition root."
        case .noResolvedExperience:
            "No experience was resolved for this login."
        }
    }
}
