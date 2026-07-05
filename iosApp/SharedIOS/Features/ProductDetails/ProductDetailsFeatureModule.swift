#if SWIFT_PACKAGE
import SharedIOSCore
#endif

@MainActor
public enum ProductDetailsFeatureModule {
    public static let module = GenericCommerceFeatureModule.make(
        id: "product-details",
        tabSystemImage: "tag"
    )
}
