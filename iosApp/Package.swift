// swift-tools-version: 6.0

import Foundation
import PackageDescription

private struct FeatureBundleConfig: Decodable {
    let featureSwiftTargets: [String: SwiftFeatureTarget]
    let products: [ProductConfig]
}

private struct SwiftFeatureTarget: Decodable {
    let libraryName: String
    let targetName: String
    let path: String
}

private struct ProductConfig: Decodable {
    let iosFeatureProductName: String
    let bundledFeatures: [String]
}

private func loadFeatureBundleConfig() -> FeatureBundleConfig {
    let packageDirectory = URL(fileURLWithPath: #filePath).deletingLastPathComponent()
    let configURL = packageDirectory
        .deletingLastPathComponent()
        .appendingPathComponent("config/product-feature-bundles.json")

    do {
        let data = try Data(contentsOf: configURL)
        return try JSONDecoder().decode(FeatureBundleConfig.self, from: data)
    } catch {
        fatalError("Unable to load feature bundle config at \(configURL.path): \(error)")
    }
}

private let featureBundleConfig = loadFeatureBundleConfig()
private let swiftFeatures = featureBundleConfig.featureSwiftTargets

private func featureTarget(for featureId: String) -> SwiftFeatureTarget {
    guard let feature = swiftFeatures[featureId] else {
        fatalError("Missing featureSwiftTargets entry for \(featureId).")
    }
    return feature
}

private let packageProducts: [Product] = [
    .library(name: "SharedIOSCore", targets: ["SharedIOSCore"]),
] +
    swiftFeatures.values
        .sorted { $0.libraryName < $1.libraryName }
        .map { feature in
            Product.library(name: feature.libraryName, targets: [feature.targetName])
        } +
    featureBundleConfig.products.map { product in
        Product.library(
            name: product.iosFeatureProductName,
            targets: ["SharedIOSCore"] + product.bundledFeatures.map { featureTarget(for: $0).targetName }
        )
    }

private let packageTargets: [Target] = [
    .target(
        name: "SharedIOSCore",
        dependencies: [
            .product(
                name: "ComposableArchitecture",
                package: "swift-composable-architecture"
            ),
        ],
        path: "SharedIOS/Core",
        exclude: [
            "LiveMultiAppClient.swift",
            "MultiAppStoreFactory.swift",
        ]
    ),
] +
    swiftFeatures.values
        .sorted { $0.targetName < $1.targetName }
        .map { feature in
            Target.target(
                name: feature.targetName,
                dependencies: ["SharedIOSCore"],
                path: feature.path
            )
        } +
    [
        .testTarget(
            name: "SharedIOSCoreTests",
            dependencies: [
                "SharedIOSCore",
                .product(
                    name: "ComposableArchitecture",
                    package: "swift-composable-architecture"
                ),
            ],
            path: "SharedIOSTests"
        ),
    ]

let package = Package(
    name: "SharedIOSArchitecture",
    platforms: [
        .iOS(.v18),
        .macOS(.v14),
    ],
    products: packageProducts,
    dependencies: [
        .package(
            url: "https://github.com/pointfreeco/swift-composable-architecture",
            exact: "1.25.5"
        ),
    ],
    targets: packageTargets
)
