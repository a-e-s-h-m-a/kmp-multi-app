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
    private let productRoot: IOSProductCompositionRoot
    private let productName: String
    private let businessUnits: [String]

    @State private var selectedBusinessUnit: String
    @State private var selectedExperience: SuperAppExperience?
    @State private var selectedStore: StoreOf<MultiAppFeature>?

    init(productRoot: IOSProductCompositionRoot = IOSProductCompositionRoot(productIdName: "SuperApp")) {
        self.productRoot = productRoot
        productName = productRoot.productName
        let businessUnits = productRoot.businessUnits.map(\.id)
        self.businessUnits = businessUnits
        _selectedBusinessUnit = State(initialValue: businessUnits.first ?? "")
    }

    private var experiences: [SuperAppExperience] {
        productRoot
            .supportedExperiencesForBusinessUnit(businessUnitIdName: selectedBusinessUnit)
            .map(SuperAppExperience.init)
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
                        Picker("Business unit", selection: $selectedBusinessUnit) {
                            ForEach(businessUnits, id: \.self) { businessUnit in
                                Text(businessUnit)
                            }
                        }
                    } footer: {
                        Text("In production this value comes from login. This sample lets you switch it to exercise the hard-coded combinations.")
                    }

                    Section {
                        ForEach(experiences) { experience in
                            Button {
                                selectedExperience = experience
                                selectedStore = MultiAppStoreFactory.make(appIdName: experience.id)
                            } label: {
                                VStack(alignment: .leading, spacing: 6) {
                                    Text(experience.displayName)
                                        .font(.headline)
                                    Text(experience.theme)
                                        .font(.subheadline)
                                        .foregroundStyle(.secondary)
                                    Text("Sites: \(experience.allowedSites.joined(separator: ", "))")
                                        .font(.caption)
                                        .foregroundStyle(.secondary)
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
    let theme: String
    let allowedSites: [String]
    let defaultUsername: String
    let supportedUsernames: [String]

    init(_ sharedExperience: SharedAppExperience) {
        id = sharedExperience.id
        displayName = sharedExperience.displayName
        theme = sharedExperience.theme
        allowedSites = sharedExperience.allowedSites
        defaultUsername = sharedExperience.defaultUsername
        supportedUsernames = sharedExperience.supportedUsernames
    }
}
