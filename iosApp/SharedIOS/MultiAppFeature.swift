import ComposableArchitecture

@Reducer
struct MultiAppFeature {
    @ObservableState
    struct State: Equatable {
        var appName: String
        var supportedUsernames: [String]
        var selectedUsername: String
        var loggedInUserSummary: String?
        var availableFeatures: [NativeFeature] = []
        var loginError: String?
        var selectedScreen: NativeScreen = .login
        var deliveryExperienceName = "Delivery Disabled"
        var deliveryOrders: [NativeDeliveryOrder] = []
        var isLoading = false
    }

    enum Action: Equatable {
        case usernameChanged(String)
        case loginTapped
        case loginSucceeded(NativeSessionSnapshot)
        case loginFailed(String)
        case logoutTapped
        case featureTapped(String)
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
                        await send(.loginSucceeded(try await client.login(username)))
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
                state.selectedScreen = .features
                return .none

            case let .loginFailed(message):
                state.isLoading = false
                state.loginError = message
                return .none

            case .logoutTapped:
                state.loggedInUserSummary = nil
                state.availableFeatures = []
                state.loginError = nil
                state.deliveryExperienceName = "Delivery Disabled"
                state.deliveryOrders = []
                state.isLoading = false
                state.selectedScreen = .login
                return .concatenate(
                    .cancel(id: CancelID.login),
                    .run { [client] _ in await client.logout() }
                )

            case let .featureTapped(featureId):
                state.selectedScreen = .feature(featureId)
                return .none

            case .backTapped:
                state.selectedScreen = .features
                return .none
            }
        }
    }
}
