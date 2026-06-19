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
    let appIdName: String

    func login(username: String) async -> NativeSessionSnapshot {
        // In the full target wiring, this is where the SwiftUI app owns the native
        // composition root and delegates login/feature/policy decisions to SharedLogic.
        // SKIE can make these KMP calls feel more Swifty later.
        let normalized = username.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let features: [String]
        let experience: String

        switch (appIdName, normalized) {
        case ("AppOne", "driver"):
            features = ["Home", "Delivery", "Profile"]
            experience = "Driver Delivery"
        case ("AppOne", "readonly"):
            features = ["Home", "Delivery", "Profile"]
            experience = "Read Only Delivery"
        case ("AppTwo", "admin"):
            features = ["Home", "Delivery", "Reports", "Profile"]
            experience = "Admin Delivery"
        case ("AppTwo", "merchant"):
            features = ["Home", "Delivery", "Reports", "Profile"]
            experience = "Merchant Delivery"
        case (_, "nod"):
            features = ["Home", "Profile"]
            experience = "Delivery Disabled"
        default:
            if appIdName == "AppOne" {
                features = ["Home", "Delivery", "Payments", "Profile"]
                experience = "Customer Delivery"
            } else {
                features = ["Home", "Profile"]
                experience = "Delivery Disabled"
            }
        }

        return NativeSessionSnapshot(
            userSummary: "\(normalized) in \(appIdName)",
            availableFeatures: features,
            deliveryExperienceName: experience,
            deliveryOrders: sampleOrders(for: experience)
        )
    }

    private func sampleOrders(for experience: String) -> [NativeDeliveryOrder] {
        [
            NativeDeliveryOrder(id: "DEL-1001", title: "Grocery drop-off", status: "Created", actions: actions(experience, "Created")),
            NativeDeliveryOrder(id: "DEL-1002", title: "Pharmacy pickup", status: "Assigned", actions: actions(experience, "Assigned")),
            NativeDeliveryOrder(id: "DEL-1003", title: "Cafe order", status: "PickedUp", actions: actions(experience, "PickedUp")),
            NativeDeliveryOrder(id: "DEL-1004", title: "Office lunch", status: "Delivered", actions: ["ViewOnly"])
        ]
    }

    private func actions(_ experience: String, _ status: String) -> [String] {
        switch (experience, status) {
        case ("Customer Delivery", "Created"):
            return ["Cancel", "EditAddress", "Track"]
        case ("Customer Delivery", "Assigned"), ("Customer Delivery", "PickedUp"):
            return ["Track"]
        case ("Driver Delivery", "Created"):
            return ["Accept"]
        case ("Driver Delivery", "Assigned"):
            return ["MarkPickedUp", "Track"]
        case ("Driver Delivery", "PickedUp"):
            return ["MarkDelivered", "Track"]
        case ("Admin Delivery", "Created"), ("Admin Delivery", "Assigned"):
            return ["Cancel", "EditAddress", "Track"]
        case ("Merchant Delivery", _):
            return ["Track"]
        case ("Delivery Disabled", _):
            return []
        default:
            return ["ViewOnly"]
        }
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
            selectedUsername: environment.appIdName == "AppOne" ? "customer" : "admin"
        )
    }

    func send(_ action: NativeAppAction) {
        switch action {
        case let .usernameChanged(username):
            state.selectedUsername = username
        case .loginTapped:
            Task {
                let snapshot = await environment.login(username: state.selectedUsername)
                state.loggedInUserSummary = snapshot.userSummary
                state.availableFeatures = snapshot.availableFeatures
                state.deliveryExperienceName = snapshot.deliveryExperienceName
                state.deliveryOrders = snapshot.deliveryOrders
                state.selectedScreen = .features
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
