// swift-tools-version: 6.0

import PackageDescription

let package = Package(
    name: "SharedIOSArchitecture",
    platforms: [
        .iOS(.v18),
        .macOS(.v14),
    ],
    products: [
        .library(name: "SharedIOSCore", targets: ["SharedIOSCore"]),
        .library(name: "FeatureOrders", targets: ["FeatureOrders"]),
        .library(name: "FeatureLists", targets: ["FeatureLists"]),
        .library(name: "FeatureCatalog", targets: ["FeatureCatalog"]),
        .library(name: "FeatureProductDetails", targets: ["FeatureProductDetails"]),
        .library(name: "FeatureDelivery", targets: ["FeatureDelivery"]),
        .library(
            name: "AppOneIOSFeatures",
            targets: ["SharedIOSCore", "FeatureOrders", "FeatureCatalog", "FeatureProductDetails", "FeatureDelivery"]
        ),
        .library(
            name: "AppTwoIOSFeatures",
            targets: ["SharedIOSCore", "FeatureOrders", "FeatureLists", "FeatureDelivery"]
        ),
        .library(
            name: "SuperAppIOSFeatures",
            targets: [
                "SharedIOSCore",
                "FeatureOrders",
                "FeatureLists",
                "FeatureCatalog",
                "FeatureProductDetails",
                "FeatureDelivery",
            ]
        ),
    ],
    dependencies: [
        .package(
            url: "https://github.com/pointfreeco/swift-composable-architecture",
            exact: "1.25.5"
        ),
    ],
    targets: [
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
        .target(
            name: "FeatureOrders",
            dependencies: ["SharedIOSCore"],
            path: "SharedIOS/Features/Orders"
        ),
        .target(
            name: "FeatureLists",
            dependencies: ["SharedIOSCore"],
            path: "SharedIOS/Features/Lists"
        ),
        .target(
            name: "FeatureCatalog",
            dependencies: ["SharedIOSCore"],
            path: "SharedIOS/Features/Catalog"
        ),
        .target(
            name: "FeatureProductDetails",
            dependencies: ["SharedIOSCore"],
            path: "SharedIOS/Features/ProductDetails"
        ),
        .target(
            name: "FeatureDelivery",
            dependencies: ["SharedIOSCore"],
            path: "SharedIOS/Features/Delivery"
        ),
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
)
