import SwiftUI

public struct FeatureSummaryRow: View {
    let feature: NativeFeature

    public init(feature: NativeFeature) {
        self.feature = feature
    }

    public var body: some View {
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

public struct FeatureSummarySection: View {
    let feature: NativeFeature

    public init(feature: NativeFeature) {
        self.feature = feature
    }

    public var body: some View {
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
