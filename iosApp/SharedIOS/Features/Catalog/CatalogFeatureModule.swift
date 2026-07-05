#if SWIFT_PACKAGE
import SharedIOSCore
#endif

@MainActor
public enum CatalogFeatureModule {
    public static let module = GenericCommerceFeatureModule.make(
        id: "catalog",
        tabSystemImage: "square.grid.2x2"
    )
}
