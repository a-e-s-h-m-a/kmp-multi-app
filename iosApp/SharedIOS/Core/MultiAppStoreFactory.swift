import ComposableArchitecture

public enum MultiAppStoreFactory {
    public static func make(appIdName: String) -> StoreOf<MultiAppFeature> {
        let client = MultiAppClient.live(appIdName: appIdName)

        return Store(
            initialState: MultiAppFeature.State(
                appName: client.appName,
                supportedUsernames: client.supportedUsernames,
                selectedUsername: client.defaultUsername
            )
        ) {
            MultiAppFeature()
        } withDependencies: {
            $0.multiAppClient = client
        }
    }

    public static func makeProduct(
        productIdName: String,
        buildFeatureBundle: [String] = []
    ) -> StoreOf<MultiAppFeature> {
        let client = MultiAppClient.liveProduct(productIdName: productIdName)

        return Store(
            initialState: MultiAppFeature.State(
                appName: client.appName,
                buildFeatureBundle: buildFeatureBundle,
                supportedUsernames: client.supportedUsernames,
                selectedUsername: client.defaultUsername
            )
        ) {
            MultiAppFeature()
        } withDependencies: {
            $0.multiAppClient = client
        }
    }
}
