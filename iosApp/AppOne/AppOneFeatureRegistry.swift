@MainActor
extension CommerceFeatureRegistry {
    static let appOne = CommerceFeatureRegistry(
        modules: [
            OrdersFeatureModule.module,
            CatalogFeatureModule.module,
            ProductDetailsFeatureModule.module,
            DeliveryFeatureModule.module,
        ]
    )
}
