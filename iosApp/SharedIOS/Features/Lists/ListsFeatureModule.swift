#if SWIFT_PACKAGE
import SharedIOSCore
#endif

@MainActor
public enum ListsFeatureModule {
    public static let module = GenericCommerceFeatureModule.make(
        id: "lists",
        tabSystemImage: "list.bullet.rectangle"
    )
}
