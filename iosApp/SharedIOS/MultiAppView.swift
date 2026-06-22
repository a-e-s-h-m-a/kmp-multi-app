import ComposableArchitecture
import SwiftUI

struct MultiAppRootView: View {
    let store: StoreOf<MultiAppFeature>

    var body: some View {
        NavigationStack {
            switch store.selectedScreen {
            case .login:
                LoginView(store: store)
            case .features:
                FeatureListView(store: store)
            case let .feature(featureId):
                FeatureDetailView(featureId: featureId, store: store)
            }
        }
    }
}

private struct LoginView: View {
    let store: StoreOf<MultiAppFeature>

    var body: some View {
        List {
            Section(store.appName) {
                Picker(
                    "Username",
                    selection: Binding(
                        get: { store.selectedUsername },
                        set: { store.send(.usernameChanged($0)) }
                    )
                ) {
                    ForEach(store.supportedUsernames, id: \.self) { user in
                        Text(user)
                    }
                }

                Button {
                    store.send(.loginTapped)
                } label: {
                    if store.isLoading {
                        ProgressView()
                    } else {
                        Text("Login")
                    }
                }
                .disabled(store.isLoading)
            }

            if let loginError = store.loginError {
                Text(loginError)
                    .foregroundStyle(.red)
            }
        }
        .navigationTitle(store.appName)
    }
}

private struct FeatureListView: View {
    let store: StoreOf<MultiAppFeature>

    var body: some View {
        List {
            Section(store.loggedInUserSummary ?? "") {
                ForEach(store.availableFeatures) { feature in
                    Button(feature.title) {
                        store.send(.featureTapped(feature.id))
                    }
                }
            }

            Button("Logout", role: .destructive) {
                store.send(.logoutTapped)
            }
        }
        .navigationTitle("Features")
    }
}

private struct FeatureDetailView: View {
    let featureId: String
    let store: StoreOf<MultiAppFeature>

    private var title: String {
        store.availableFeatures.first { $0.id == featureId }?.title ?? featureId
    }

    var body: some View {
        List {
            if featureId == "delivery" {
                Section(store.deliveryExperienceName) {
                    ForEach(store.deliveryOrders) { order in
                        VStack(alignment: .leading, spacing: 8) {
                            Text(order.title)
                                .font(.headline)
                            Text("\(order.id) / \(order.status)")
                                .foregroundStyle(.secondary)
                            Text(order.actions.joined(separator: ", "))
                        }
                    }
                }
            } else {
                Text(title)
            }
        }
        .navigationTitle(title)
        .toolbar {
            Button("Back") {
                store.send(.backTapped)
            }
        }
    }
}
