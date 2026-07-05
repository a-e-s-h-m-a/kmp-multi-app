import ComposableArchitecture
import SwiftUI

struct FeatureDetailView: View {
    let featureId: String
    let store: StoreOf<MultiAppFeature>
    let featureRegistry: CommerceFeatureRegistry

    private var title: String {
        store.availableFeatures.first { $0.id == featureId }?.title ?? featureId
    }

    private var feature: NativeFeature? {
        store.availableFeatures.first { $0.id == featureId }
    }

    var body: some View {
        Group {
            if let feature {
                featureRegistry.module(for: feature).makeView(feature: feature, store: store)
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
