#if SWIFT_PACKAGE
import SharedIOSCore
#endif

@MainActor
public enum OrdersFeatureModule {
    public static let module = GenericCommerceFeatureModule.make(
        id: "orders",
        tabSystemImage: "doc.text"
    )
}
