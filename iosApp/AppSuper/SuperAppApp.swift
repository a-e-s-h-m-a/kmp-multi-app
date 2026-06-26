import ComposableArchitecture
import SharedLogic
import SwiftUI

@main
struct SuperAppApp: App {
    var body: some Scene {
        WindowGroup {
            SuperAppRootView()
        }
    }
}

private struct SuperAppRootView: View {
    private let productName: String
    private let experiences: [SuperAppExperience]

    @State private var selectedExperience: SuperAppExperience?
    @State private var selectedStore: StoreOf<MultiAppFeature>?

    init(productRoot: IOSProductCompositionRoot = IOSProductCompositionRoot(productIdName: "SuperApp")) {
        productName = productRoot.productName
        experiences = productRoot.supportedExperiences.map(SuperAppExperience.init)
    }

    var body: some View {
        if let selectedExperience, let selectedStore {
            VStack(spacing: 0) {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(productName)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        Text(selectedExperience.displayName)
                            .font(.headline)
                    }

                    Spacer()

                    Button("Switch") {
                        self.selectedExperience = nil
                        self.selectedStore = nil
                    }
                    .buttonStyle(.bordered)
                }
                .padding()
                .background(.bar)

                MultiAppRootView(store: selectedStore)
            }
        } else {
            NavigationStack {
                List {
                    Section {
                        ForEach(experiences) { experience in
                            Button {
                                selectedExperience = experience
                                selectedStore = MultiAppStoreFactory.make(appIdName: experience.id)
                            } label: {
                                VStack(alignment: .leading, spacing: 6) {
                                    Text(experience.displayName)
                                        .font(.headline)
                                    Text("Default user: \(experience.defaultUsername)")
                                        .font(.subheadline)
                                        .foregroundStyle(.secondary)
                                    Text("Users: \(experience.supportedUsernames.joined(separator: ", "))")
                                        .font(.caption)
                                        .foregroundStyle(.secondary)
                                }
                            }
                        }
                    } header: {
                        Text("Available experiences")
                    } footer: {
                        Text("These experiences come from the shared KMP ProductCatalog for the SuperApp product.")
                    }
                }
                .navigationTitle(productName)
            }
        }
    }
}

private struct SuperAppExperience: Identifiable, Equatable {
    let id: String
    let displayName: String
    let defaultUsername: String
    let supportedUsernames: [String]

    init(_ sharedExperience: SharedAppExperience) {
        id = sharedExperience.id
        displayName = sharedExperience.displayName
        defaultUsername = sharedExperience.defaultUsername
        supportedUsernames = sharedExperience.supportedUsernames
    }
}
