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
                    Button {
                        store.send(.featureTapped(feature.id))
                    } label: {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(feature.title)
                                .font(.headline)
                            Text("Requires: \(feature.requiredPermission)")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            if !feature.enabledTweaks.isEmpty {
                                Text("Tweaks: \(feature.enabledTweaks.joined(separator: ", "))")
                                    .font(.caption2)
                                    .foregroundStyle(.secondary)
                            }
                        }
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

    private var feature: NativeFeature? {
        store.availableFeatures.first { $0.id == featureId }
    }

    var body: some View {
        List {
            if let feature {
                Section("Permissions") {
                    Text("Required: \(feature.requiredPermission)")
                    if feature.enabledTweaks.isEmpty {
                        Text("No optional tweaks enabled")
                            .foregroundStyle(.secondary)
                    } else {
                        ForEach(feature.enabledTweaks, id: \.self) { tweak in
                            Text(tweak)
                        }
                    }
                }
            }

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
