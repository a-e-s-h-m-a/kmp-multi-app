import ComposableArchitecture
import SwiftUI

struct FeatureTabShellView: View {
    let store: StoreOf<MultiAppFeature>
    private let featureRegistry = CommerceFeatureRegistry.shared
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
                ContentUnavailableView(
                    "No features",
                    systemImage: "square.grid.2x2",
                    description: Text("The resolved experience has no features for this login.")
                )
            } else {
                TabView(selection: $selectedTab) {
                    ForEach(Array(tabFeatures.enumerated()), id: \.element.id) { index, feature in
                        FeatureTabContentView(feature: feature, store: store, featureRegistry: featureRegistry)
                            .tag(index)
                            .tabItem {
                                Label(
                                    feature.title,
                                    systemImage: featureRegistry.module(for: feature).tabSystemImage
                                )
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
    let featureRegistry: CommerceFeatureRegistry

    var body: some View {
        featureRegistry.module(for: feature).makeView(feature: feature, store: store)
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
