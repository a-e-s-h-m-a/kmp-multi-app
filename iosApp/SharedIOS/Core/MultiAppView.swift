import ComposableArchitecture
import SwiftUI

public struct MultiAppRootView: View {
    let store: StoreOf<MultiAppFeature>
    let featureRegistry: CommerceFeatureRegistry

    public init(
        store: StoreOf<MultiAppFeature>,
        featureRegistry: CommerceFeatureRegistry
    ) {
        self.store = store
        self.featureRegistry = featureRegistry
    }

    public var body: some View {
        NavigationStack {
            switch store.selectedScreen {
            case .login:
                LoginView(store: store)
            case .experienceSwitcher:
                ExperienceSwitcherView(store: store)
            case .features:
                FeatureTabShellView(store: store, featureRegistry: featureRegistry)
            case let .feature(featureId):
                FeatureDetailView(
                    featureId: featureId,
                    store: store,
                    featureRegistry: featureRegistry
                )
            }
        }
        .tint(store.selectedTheme.primaryColor)
    }
}

private struct LoginView: View {
    let store: StoreOf<MultiAppFeature>

    var body: some View {
        List {
            Section(store.appName) {
                if !store.buildFeatureBundle.isEmpty {
                    Text("Build bundle: \(store.buildFeatureBundle.joined(separator: ", "))")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

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

private struct ExperienceSwitcherView: View {
    let store: StoreOf<MultiAppFeature>

    var body: some View {
        List {
            if let loginError = store.loginError {
                Text(loginError)
                    .foregroundStyle(.red)
            }

            Section("Available experiences") {
                ForEach(store.resolvedExperienceOptions) { option in
                    Button {
                        store.send(.experienceTapped(option.id))
                    } label: {
                        VStack(alignment: .leading, spacing: 6) {
                            Text(option.displayName)
                                .font(.headline)
                            Text("\(option.grantLabel) / \(option.businessUnitId)")
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                            Text("Theme: \(option.defaultTheme.rawValue)")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            Text("Sites: \(option.allowedSites.joined(separator: ", "))")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            Text("Roles: \(option.roles.joined(separator: ", "))")
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                            Text("Resolved permissions: \(option.resolvedPermissions.joined(separator: ", "))")
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                        }
                    }
                    .disabled(store.isLoading)
                }
            }

            Button("Back to login") {
                store.send(.previousTapped)
            }
        }
        .navigationTitle(store.appName)
        .scrollContentBackground(.hidden)
        .background(store.selectedTheme.backgroundColor)
    }
}
