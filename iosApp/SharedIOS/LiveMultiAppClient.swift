import SharedLogic

private actor IOSAppGateway {
    private let compositionRoot: IOSAppCompositionRoot
    nonisolated let appName: String
    nonisolated let defaultUsername: String
    nonisolated let supportedUsernames: [String]

    init(appIdName: String) {
        let compositionRoot = IOSAppCompositionRoot(appIdName: appIdName)
        self.compositionRoot = compositionRoot
        appName = compositionRoot.appName
        defaultUsername = compositionRoot.defaultUsername
        supportedUsernames = compositionRoot.supportedUsernames
    }

    func login(username: String) async throws -> NativeSessionSnapshot {
        let snapshot = try await compositionRoot.login(username: username)
        return NativeSessionSnapshot(
            userSummary: snapshot.userSummary,
            availableFeatures: snapshot.availableFeatures.map {
                NativeFeature(
                    id: $0.id,
                    title: $0.title,
                    requiredPermission: $0.requiredPermission,
                    enabledTweaks: $0.enabledTweaks
                )
            },
            deliveryExperienceName: snapshot.deliveryExperienceName,
            deliveryOrders: snapshot.deliveryOrders.map {
                NativeDeliveryOrder(
                    id: $0.id,
                    title: $0.title,
                    status: $0.status,
                    actions: $0.actions
                )
            }
        )
    }

    func logout() {
        compositionRoot.logout()
    }
}

extension MultiAppClient {
    static func live(appIdName: String) -> Self {
        let gateway = IOSAppGateway(appIdName: appIdName)

        return Self(
            appName: gateway.appName,
            defaultUsername: gateway.defaultUsername,
            supportedUsernames: gateway.supportedUsernames,
            login: { username in
                try await gateway.login(username: username)
            },
            logout: {
                await gateway.logout()
            }
        )
    }
}
