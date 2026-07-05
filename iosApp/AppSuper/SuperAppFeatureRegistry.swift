@MainActor
extension CommerceFeatureRegistry {
    static let superApp = CommerceFeatureRegistry(
        modules: [
            OrdersFeatureModule.module,
            ListsFeatureModule.module,
            CatalogFeatureModule.module,
            ProductDetailsFeatureModule.module,
            DeliveryFeatureModule.module,
        ]
    )
}
