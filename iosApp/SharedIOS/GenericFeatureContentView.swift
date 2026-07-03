import SwiftUI

struct GenericFeatureContentView: View {
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

struct GenericFeatureContentRows: View {
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

