@MainActor
extension CommerceFeatureRegistry {
    static let appTwo = CommerceFeatureRegistry(
        modules: [
            OrdersFeatureModule.module,
            ListsFeatureModule.module,
            DeliveryFeatureModule.module,
        ]
    )
}
