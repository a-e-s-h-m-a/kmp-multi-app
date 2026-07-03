import ComposableArchitecture
import SwiftUI

struct FeatureDetailView: View {
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
            } else if let feature {
                GenericFeatureContentRows(feature: feature, theme: store.selectedTheme)
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

