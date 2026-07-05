import ComposableArchitecture
#if SWIFT_PACKAGE
import SharedIOSCore
#endif
import SwiftUI

public struct DeliveryFeatureContentView: View {
    let feature: NativeFeature
    let store: StoreOf<MultiAppFeature>

    public init(feature: NativeFeature, store: StoreOf<MultiAppFeature>) {
        self.feature = feature
        self.store = store
    }

    public var body: some View {
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
