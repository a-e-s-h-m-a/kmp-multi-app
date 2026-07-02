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
                FeatureTabShellView(store: store)
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

private struct FeatureTabShellView: View {
    let store: StoreOf<MultiAppFeature>
    @State private var selectedTab = 0

    private var tabFeatures: [NativeFeature] {
        Array(store.availableFeatures.prefix(4))
    }

    private var overflowFeatures: [NativeFeature] {
        Array(store.availableFeatures.dropFirst(4))
    }

    private var hasMoreTab: Bool {
        !overflowFeatures.isEmpty
    }

    var body: some View {
        Group {
            if store.availableFeatures.isEmpty {
                ContentUnavailableView("No features", systemImage: "square.grid.2x2", description: Text("The resolved experience has no features for this login."))
            } else {
                TabView(selection: $selectedTab) {
                    ForEach(Array(tabFeatures.enumerated()), id: \.element.id) { index, feature in
                        FeatureTabContentView(feature: feature, store: store)
                            .tag(index)
                            .tabItem {
                                Label(feature.title, systemImage: feature.tabSystemImage)
                            }
                    }

                    if hasMoreTab {
                        MoreFeaturesView(features: overflowFeatures, store: store)
                            .tag(tabFeatures.count)
                            .tabItem {
                                Label("More", systemImage: "ellipsis.circle")
                            }
                    }
                }
            }
        }
        .navigationTitle(store.appName)
        .toolbar {
            ToolbarItemGroup(placement: .topBarTrailing) {
                Button("Back") {
                    store.send(.previousTapped)
                }
                Button("Logout", role: .destructive) {
                    store.send(.logoutTapped)
                }
            }
        }
        .background(store.selectedTheme.backgroundColor)
    }
}

private struct FeatureTabContentView: View {
    let feature: NativeFeature
    let store: StoreOf<MultiAppFeature>

    var body: some View {
        if feature.id == "delivery" {
            DeliveryFeatureContentView(feature: feature, store: store)
        } else {
            GenericFeatureContentView(feature: feature, theme: store.selectedTheme)
        }
    }
}

private struct MoreFeaturesView: View {
    let features: [NativeFeature]
    let store: StoreOf<MultiAppFeature>

    var body: some View {
        List {
            Section("More features") {
                ForEach(features) { feature in
                    Button {
                        store.send(.featureTapped(feature.id))
                    } label: {
                        FeatureSummaryRow(feature: feature)
                    }
                }
            }
        }
        .scrollContentBackground(.hidden)
        .background(store.selectedTheme.backgroundColor)
    }
}

private struct FeatureSummaryRow: View {
    let feature: NativeFeature

    var body: some View {
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
                FeatureSummarySection(feature: feature)
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
                if let feature {
                    GenericFeatureContentRows(feature: feature, theme: store.selectedTheme)
                } else {
                    Text(title)
                }
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

private struct GenericFeatureContentView: View {
    let feature: NativeFeature
    let theme: NativeTheme

    var body: some View {
        List {
            FeatureSummarySection(feature: feature)
            GenericFeatureContentRows(feature: feature, theme: theme)
        }
        .navigationTitle(feature.title)
        .scrollContentBackground(.hidden)
        .background(theme.backgroundColor)
    }
}

private struct GenericFeatureContentRows: View {
    let feature: NativeFeature
    let theme: NativeTheme
    @State private var lastActionResult = "No action has been triggered yet."

    var body: some View {
        Section("Capability panels") {
            if feature.uiBlocks.isEmpty {
                Text("Capability-driven panels hidden for this login.")
                    .foregroundStyle(.secondary)
            } else {
                ForEach(feature.uiBlocks, id: \.title) { block in
                    VStack(alignment: .leading, spacing: 4) {
                        Text(block.title)
                            .font(.headline)
                        Text(block.body)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                        Text(block.requiredPermission)
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                }
            }
        }

        Section("Actions") {
            Text(lastActionResult)
                .foregroundStyle(theme.primaryColor)
                .font(.subheadline.weight(.semibold))

            if feature.actions.isEmpty {
                Text("No actions allowed for this login.")
                    .foregroundStyle(.secondary)
            } else {
                ForEach(feature.actions, id: \.label) { action in
                    Button(action.label) {
                        lastActionResult = action.result
                    }
                }
            }
        }
    }
}

private struct FeatureSummarySection: View {
    let feature: NativeFeature

    var body: some View {
        Section("Permissions") {
            Text("Required: \(feature.requiredPermission)")
            if feature.permissionRows.isEmpty {
                Text("No permission rows defined")
                    .foregroundStyle(.secondary)
            } else {
                ForEach(feature.permissionRows, id: \.permission) { row in
                    HStack {
                        Text(row.label)
                        Spacer()
                        Text(row.enabled ? "enabled" : "disabled")
                            .foregroundStyle(row.enabled ? .green : .secondary)
                    }
                }
            }
            if !feature.enabledTweaks.isEmpty {
                Text("Tweaks: \(feature.enabledTweaks.joined(separator: ", "))")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
    }
}

private struct DeliveryFeatureContentView: View {
    let feature: NativeFeature
    let store: StoreOf<MultiAppFeature>

    var body: some View {
        List {
            FeatureSummarySection(feature: feature)
            if !feature.uiBlocks.isEmpty {
                Section("Capability panels") {
                    ForEach(feature.uiBlocks, id: \.title) { block in
                        VStack(alignment: .leading, spacing: 4) {
                            Text(block.title)
                                .font(.headline)
                            Text(block.body)
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
            }
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
        }
        .navigationTitle(feature.title)
        .scrollContentBackground(.hidden)
        .background(store.selectedTheme.backgroundColor)
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

private extension NativeFeature {
    var tabSystemImage: String {
        switch id {
        case "orders":
            "doc.text"
        case "lists":
            "list.bullet.rectangle"
        case "catalog":
            "square.grid.2x2"
        case "product-details":
            "tag"
        case "delivery":
            "truck.box"
        default:
            "square"
        }
    }
}
