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
            path: "SharedIOS",
            exclude: [
                "LiveMultiAppClient.swift",
                "MultiAppStoreFactory.swift",
                "MultiAppView.swift",
            ]
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
