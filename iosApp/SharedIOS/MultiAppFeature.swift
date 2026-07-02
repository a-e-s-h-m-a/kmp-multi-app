import ComposableArchitecture

@Reducer
struct MultiAppFeature {
    @ObservableState
    struct State: Equatable {
        var appName: String
        var buildFeatureBundle: [String] = []
        var supportedUsernames: [String]
        var selectedUsername: String
        var loggedInUserSummary: String?
        var resolvedExperienceOptions: [NativeResolvedExperienceOption] = []
        var selectedExperience: NativeResolvedExperienceOption?
        var selectedTheme: NativeTheme = .operations
        var availableFeatures: [NativeFeature] = []
        var loginError: String?
        var selectedScreen: NativeScreen = .login
        var deliveryExperienceName = "Delivery Disabled"
        var deliveryOrders: [NativeDeliveryOrder] = []
        var lastActionResult = "No action has been triggered yet."
        var isLoading = false
    }

    enum Action: Equatable {
        case usernameChanged(String)
        case loginTapped
        case resolveSucceeded([NativeResolvedExperienceOption])
        case experienceTapped(String)
        case loginSucceeded(NativeSessionSnapshot)
        case loginFailed(String)
        case logoutTapped
        case previousTapped
        case featureTapped(String)
        case deliveryActionTapped(orderId: String, action: String)
        case backTapped
    }

    private enum CancelID {
        case login
    }

    @Dependency(\.multiAppClient) private var client

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case let .usernameChanged(username):
                state.selectedUsername = username
                return .none

            case .loginTapped:
                state.isLoading = true
                state.loginError = nil
                let username = state.selectedUsername

                return .run { [client] send in
                    do {
                        await send(.resolveSucceeded(try await client.resolveExperienceOptions(username)))
                    } catch {
                        guard !Task.isCancelled else { return }
                        await send(.loginFailed(error.localizedDescription))
                    }
                }
                .cancellable(id: CancelID.login, cancelInFlight: true)

            case let .resolveSucceeded(options):
                state.isLoading = false
                state.resolvedExperienceOptions = options
                if options.count == 1, let option = options.first {
                    state.isLoading = true
                    state.selectedExperience = option
                    state.selectedTheme = option.defaultTheme
                    let username = state.selectedUsername
                    return .run { [client] send in
                        do {
                            await send(.loginSucceeded(try await client.launchExperience(option.id, username)))
                        } catch {
                            guard !Task.isCancelled else { return }
                            await send(.loginFailed(error.localizedDescription))
                        }
                    }
                    .cancellable(id: CancelID.login, cancelInFlight: true)
                }
                state.selectedScreen = .experienceSwitcher
                return .none

            case let .experienceTapped(optionId):
                guard let option = state.resolvedExperienceOptions.first(where: { $0.id == optionId }) else {
                    return .none
                }
                state.isLoading = true
                state.loginError = nil
                state.selectedExperience = option
                state.selectedTheme = option.defaultTheme
                let username = state.selectedUsername
                return .run { [client] send in
                    do {
                        await send(.loginSucceeded(try await client.launchExperience(option.id, username)))
                    } catch {
                        guard !Task.isCancelled else { return }
                        await send(.loginFailed(error.localizedDescription))
                    }
                }
                .cancellable(id: CancelID.login, cancelInFlight: true)

            case let .loginSucceeded(snapshot):
                state.isLoading = false
                state.loggedInUserSummary = snapshot.userSummary
                state.availableFeatures = snapshot.availableFeatures
                state.deliveryExperienceName = snapshot.deliveryExperienceName
                state.deliveryOrders = snapshot.deliveryOrders
                state.lastActionResult = "No action has been triggered yet."
                state.selectedScreen = .features
                return .none

            case let .loginFailed(message):
                state.isLoading = false
                state.loginError = message
                return .none

            case .logoutTapped:
                state.loggedInUserSummary = nil
                state.resolvedExperienceOptions = []
                state.selectedExperience = nil
                state.availableFeatures = []
                state.loginError = nil
                state.deliveryExperienceName = "Delivery Disabled"
                state.deliveryOrders = []
                state.lastActionResult = "No action has been triggered yet."
                state.isLoading = false
                state.selectedScreen = .login
                return .concatenate(
                    .cancel(id: CancelID.login),
                    .run { [client] _ in await client.logout() }
                )

            case .previousTapped:
                switch state.selectedScreen {
                case .experienceSwitcher:
                    state.resolvedExperienceOptions = []
                    state.selectedExperience = nil
                    state.loginError = nil
                    state.selectedScreen = .login
                    return .cancel(id: CancelID.login)

                case .features:
                    state.loggedInUserSummary = nil
                    state.selectedExperience = nil
                    state.availableFeatures = []
                    state.deliveryExperienceName = "Delivery Disabled"
                    state.deliveryOrders = []
                    state.lastActionResult = "No action has been triggered yet."
                    state.isLoading = false
                    if state.resolvedExperienceOptions.count > 1 {
                        state.selectedScreen = .experienceSwitcher
                    } else {
                        state.resolvedExperienceOptions = []
                        state.selectedScreen = .login
                    }
                    return .run { [client] _ in await client.logout() }

                case .feature:
                    state.selectedScreen = .features
                    return .none

                case .login:
                    return .none
                }

            case let .featureTapped(featureId):
                state.selectedScreen = .feature(featureId)
                return .none

            case let .deliveryActionTapped(orderId, action):
                guard let index = state.deliveryOrders.firstIndex(where: { $0.id == orderId }) else {
                    return .none
                }
                let nextStatus = action.nextDeliveryStatus(from: state.deliveryOrders[index].status)
                state.deliveryOrders[index].status = nextStatus
                state.deliveryOrders[index].actions = state.deliveryOrders[index].actions.actions(after: action, status: nextStatus)
                state.lastActionResult = "\(action) applied to \(orderId); status is now \(nextStatus)."
                return .none

            case .backTapped:
                state.selectedScreen = .features
                return .none
            }
        }
    }
}

private extension String {
    func nextDeliveryStatus(from currentStatus: String) -> String {
        switch self {
        case "Cancel":
            "Cancelled"
        case "Accept":
            "Assigned"
        case "MarkPickedUp":
            "PickedUp"
        case "MarkDelivered":
            "Delivered"
        default:
            currentStatus
        }
    }
}

private extension [String] {
    func actions(after action: String, status: String) -> [String] {
        let tracks = contains("Track")
        let next: [String]
        switch (action, status) {
        case ("Accept", "Assigned"):
            next = ["MarkPickedUp"]
        case ("MarkPickedUp", "PickedUp"):
            next = ["MarkDelivered"]
        case ("Cancel", "Cancelled"), ("MarkDelivered", "Delivered"):
            next = ["ViewOnly"]
        default:
            next = filter { $0 != action }
        }
        let withTracking = tracks && !["Delivered", "Cancelled"].contains(status) && !next.contains("Track")
            ? next + ["Track"]
            : next
        return withTracking.isEmpty ? ["ViewOnly"] : withTracking
    }
}
