import ComposableArchitecture
import SwiftUI

struct MultiAppRootView: View {
    let store: StoreOf<MultiAppFeature>

    var body: some View {
        NavigationStack {
            switch store.selectedScreen {
            case .login:
                LoginView(store: store)
            case .experienceSwitcher:
                ExperienceSwitcherView(store: store)
            case .features:
                FeatureListView(store: store)
            case let .feature(featureId):
                FeatureDetailView(featureId: featureId, store: store)
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
            if let selectedExperience = store.selectedExperience {
                Section("Experience") {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(selectedExperience.displayName)
                            .font(.headline)
                        Text("\(selectedExperience.grantLabel) / \(selectedExperience.businessUnitId)")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                        Text("Roles: \(selectedExperience.roles.joined(separator: ", "))")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
            }

            Section(store.loggedInUserSummary ?? "") {
                Text("Theme: \(store.selectedTheme.rawValue)")
                    .foregroundStyle(.secondary)

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

            Button("Back") {
                store.send(.previousTapped)
            }

            Button("Logout", role: .destructive) {
                store.send(.logoutTapped)
            }
        }
        .navigationTitle("Features")
        .scrollContentBackground(.hidden)
        .background(store.selectedTheme.backgroundColor)
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
                    Text(store.lastActionResult)
                        .foregroundStyle(store.selectedTheme.primaryColor)
                        .font(.subheadline.weight(.semibold))

                    ForEach(store.deliveryOrders) { order in
                        VStack(alignment: .leading, spacing: 8) {
                            Text(order.title)
                                .font(.headline)
                            Text("\(order.id) / \(order.status)")
                                .foregroundStyle(.secondary)
                            HStack {
                                ForEach(order.actions, id: \.self) { action in
                                    Button(action) {
                                        store.send(.deliveryActionTapped(orderId: order.id, action: action))
                                    }
                                    .buttonStyle(.bordered)
                                }
                            }
                        }
                    }
                }
            } else {
                Text(title)
            }
        }
        .navigationTitle(title)
        .scrollContentBackground(.hidden)
        .background(store.selectedTheme.backgroundColor)
        .toolbar {
            Button("Back") {
                store.send(.previousTapped)
            }
        }
    }
}

private extension NativeTheme {
    var primaryColor: Color {
        switch self {
        case .boutique:
            Color(red: 0.45, green: 0.32, blue: 0.12)
        case .broadline:
            Color(red: 0.08, green: 0.36, blue: 0.63)
        case .operations:
            Color(red: 0.22, green: 0.42, blue: 0.13)
        }
    }

    var backgroundColor: Color {
        switch self {
        case .boutique:
            Color(red: 1.00, green: 0.98, blue: 0.95)
        case .broadline:
            Color(red: 0.97, green: 0.98, blue: 1.00)
        case .operations:
            Color(red: 0.97, green: 0.98, blue: 0.96)
        }
    }
}
