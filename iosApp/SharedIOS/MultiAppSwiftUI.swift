import SwiftUI
import SharedLogic

// Source sample for the two real iOS targets:
// AppOne target calls MultiAppRootView(appIdName: "AppOne")
// AppTwo target calls MultiAppRootView(appIdName: "AppTwo")
//
// The structure mirrors TCA without taking a dependency on TCA yet:
// State, Action, Environment/Dependencies, and a reducer-like send method.

enum NativeScreen: Equatable {
    case login
    case features
    case feature(String)
}

struct NativeAppState {
    var appIdName: String
    var selectedUsername: String
    var loggedInUserSummary: String?
    var availableFeatures: [String] = []
    var selectedScreen: NativeScreen = .login
    var deliveryExperienceName: String = "Delivery Disabled"
    var deliveryOrders: [NativeDeliveryOrder] = []
}

enum NativeAppAction {
    case usernameChanged(String)
    case loginTapped
    case logoutTapped
    case featureTapped(String)
    case backTapped
}

struct NativeDeliveryOrder: Identifiable {
    let id: String
    let title: String
    let status: String
    let actions: [String]
}

struct NativeAppEnvironment {
    private let facade: IOSAppFacade

    init(appIdName: String) {
        facade = IOSAppFacade(appIdName: appIdName)
    }

    var appIdName: String { facade.appName }
    var defaultUsername: String { facade.defaultUsername }

    func login(username: String) async throws -> NativeSessionSnapshot {
        let snapshot = try await withCheckedThrowingContinuation { continuation in
            facade.login(username: username) { snapshot, error in
                if let error {
                    continuation.resume(throwing: error)
                } else if let snapshot {
                    continuation.resume(returning: snapshot)
                }
            }
        }

        return NativeSessionSnapshot(
            userSummary: snapshot.userSummary,
            availableFeatures: snapshot.availableFeatures,
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
}

struct NativeSessionSnapshot {
    let userSummary: String
    let availableFeatures: [String]
    let deliveryExperienceName: String
    let deliveryOrders: [NativeDeliveryOrder]
}

@MainActor
final class NativeAppStore: ObservableObject {
    @Published private(set) var state: NativeAppState
    private let environment: NativeAppEnvironment

    init(environment: NativeAppEnvironment) {
        self.environment = environment
        self.state = NativeAppState(
            appIdName: environment.appIdName,
            selectedUsername: environment.defaultUsername
        )
    }

    func send(_ action: NativeAppAction) {
        switch action {
        case let .usernameChanged(username):
            state.selectedUsername = username
        case .loginTapped:
            Task {
                do {
                    let snapshot = try await environment.login(username: state.selectedUsername)
                    state.loggedInUserSummary = snapshot.userSummary
                    state.availableFeatures = snapshot.availableFeatures
                    state.deliveryExperienceName = snapshot.deliveryExperienceName
                    state.deliveryOrders = snapshot.deliveryOrders
                    state.selectedScreen = .features
                } catch {
                    state.loggedInUserSummary = error.localizedDescription
                }
            }
        case .logoutTapped:
            state.loggedInUserSummary = nil
            state.availableFeatures = []
            state.selectedScreen = .login
        case let .featureTapped(feature):
            state.selectedScreen = .feature(feature)
        case .backTapped:
            state.selectedScreen = .features
        }
    }
}

struct MultiAppRootView: View {
    @StateObject private var store: NativeAppStore

    init(appIdName: String) {
        _store = StateObject(
            wrappedValue: NativeAppStore(
                environment: NativeAppEnvironment(appIdName: appIdName)
            )
        )
    }

    var body: some View {
        NavigationStack {
            switch store.state.selectedScreen {
            case .login:
                LoginView(state: store.state, send: store.send)
            case .features:
                FeatureListView(state: store.state, send: store.send)
            case let .feature(feature):
                FeatureDetailView(feature: feature, state: store.state, send: store.send)
            }
        }
    }
}

private struct LoginView: View {
    let state: NativeAppState
    let send: (NativeAppAction) -> Void
    private let users = ["customer", "driver", "admin", "merchant", "readonly", "nod"]

    var body: some View {
        List {
            Section(state.appIdName) {
                Picker("Username", selection: Binding(
                    get: { state.selectedUsername },
                    set: { send(.usernameChanged($0)) }
                )) {
                    ForEach(users, id: \.self) { user in
                        Text(user)
                    }
                }
                Button("Login") {
                    send(.loginTapped)
                }
            }
        }
        .navigationTitle(state.appIdName)
    }
}

private struct FeatureListView: View {
    let state: NativeAppState
    let send: (NativeAppAction) -> Void

    var body: some View {
        List {
            Section(state.loggedInUserSummary ?? "") {
                ForEach(state.availableFeatures, id: \.self) { feature in
                    Button(feature) {
                        send(.featureTapped(feature))
                    }
                }
            }
            Button("Logout") {
                send(.logoutTapped)
            }
        }
        .navigationTitle("Features")
    }
}

private struct FeatureDetailView: View {
    let feature: String
    let state: NativeAppState
    let send: (NativeAppAction) -> Void

    var body: some View {
        List {
            if feature == "Delivery" {
                Section(state.deliveryExperienceName) {
                    ForEach(state.deliveryOrders) { order in
                        VStack(alignment: .leading, spacing: 8) {
                            Text(order.title).font(.headline)
                            Text("\(order.id) / \(order.status)").foregroundStyle(.secondary)
                            Text(order.actions.joined(separator: ", "))
                        }
                    }
                }
            } else {
                Text(feature)
            }
        }
        .navigationTitle(feature)
        .toolbar {
            Button("Back") {
                send(.backTapped)
            }
        }
    }
}
