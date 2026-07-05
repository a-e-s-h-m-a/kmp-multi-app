import SharedLogic

private func nativeSnapshot(_ snapshot: SharedSessionSnapshot) -> NativeSessionSnapshot {
    NativeSessionSnapshot(
        userSummary: snapshot.userSummary,
        availableFeatures: snapshot.availableFeatures.map {
            NativeFeature(
                id: $0.id,
                title: $0.title,
                requiredPermission: $0.requiredPermission,
                enabledTweaks: $0.enabledTweaks,
                permissionRows: $0.permissionRows.map {
                    NativeFeaturePermissionRow(
                        label: $0.label,
                        permission: $0.permission,
                        enabled: $0.enabled
                    )
                },
                actions: $0.actions.map {
                    NativeFeatureAction(
                        label: $0.label,
                        requiredPermission: $0.requiredPermission,
                        result: $0.result
                    )
                },
                uiBlocks: $0.uiBlocks.map {
                    NativeFeatureUiBlock(
                        title: $0.title,
                        requiredPermission: $0.requiredPermission,
                        body: $0.body
                    )
                }
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
        nativeSnapshot(try await compositionRoot.login(username: username))
    }

    func logout() {
        compositionRoot.logout()
    }
}

private actor IOSProductGateway {
    private let compositionRoot: IOSProductCompositionRoot
    nonisolated let appName: String
    nonisolated let defaultUsername: String
    nonisolated let supportedUsernames: [String]

    init(productIdName: String) {
        let compositionRoot = IOSProductCompositionRoot(productIdName: productIdName)
        self.compositionRoot = compositionRoot
        appName = compositionRoot.productName
        defaultUsername = compositionRoot.defaultUsername
        supportedUsernames = compositionRoot.supportedUsernames
    }

    func resolveExperienceOptions(username: String) -> [NativeResolvedExperienceOption] {
        compositionRoot.resolvedExperienceOptions(username: username).map {
            NativeResolvedExperienceOption(
                id: $0.id,
                appId: $0.appId,
                displayName: $0.displayName,
                theme: $0.theme,
                allowedSites: $0.allowedSites,
                grantLabel: $0.grantLabel,
                businessUnitId: $0.businessUnitId,
                roles: $0.roles,
                resolvedPermissions: $0.resolvedPermissions
            )
        }
    }

    func launchExperience(optionId: String, username: String) throws -> NativeSessionSnapshot {
        nativeSnapshot(compositionRoot.launchResolvedExperience(optionId: optionId, username: username))
    }

    func logout() {
        compositionRoot.supportedExperiences.forEach {
            compositionRoot.logout(appIdName: $0.id)
        }
    }
}

extension MultiAppClient {
    static func live(appIdName: String) -> Self {
        let gateway = IOSAppGateway(appIdName: appIdName)

        return Self(
            appName: gateway.appName,
            defaultUsername: gateway.defaultUsername,
            supportedUsernames: gateway.supportedUsernames,
            resolveExperienceOptions: { _ in
                [
                    NativeResolvedExperienceOption(
                        id: appIdName,
                        appId: appIdName,
                        displayName: gateway.appName,
                        theme: "Standalone",
                        allowedSites: [],
                        grantLabel: "Standalone Login",
                        businessUnitId: "",
                        roles: [],
                        resolvedPermissions: []
                    )
                ]
            },
            launchExperience: { _, username in
                try await gateway.login(username: username)
            },
            login: { username in
                try await gateway.login(username: username)
            },
            logout: {
                await gateway.logout()
            }
        )
    }

    static func liveProduct(productIdName: String) -> Self {
        let gateway = IOSProductGateway(productIdName: productIdName)

        return Self(
            appName: gateway.appName,
            defaultUsername: gateway.defaultUsername,
            supportedUsernames: gateway.supportedUsernames,
            resolveExperienceOptions: { username in
                await gateway.resolveExperienceOptions(username: username)
            },
            launchExperience: { optionId, username in
                try await gateway.launchExperience(optionId: optionId, username: username)
            },
            login: { username in
                let options = await gateway.resolveExperienceOptions(username: username)
                guard let option = options.first else {
                    throw MultiAppClientError.noResolvedExperience
                }
                return try await gateway.launchExperience(optionId: option.id, username: username)
            },
            logout: {
                await gateway.logout()
            }
        )
    }
}
