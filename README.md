# Kotlin Multiplatform Multi-App Architecture

This sample builds standalone products and a Super App product from one Kotlin Multiplatform codebase:

| Product | Android application ID | iOS bundle ID | Product ID | Experiences |
|---|---|---|---|---|
| AppOne | `com.aeshma.appone` | `com.aeshma.appone` | `AppOneStandalone` | AppOne |
| AppTwo | `com.aeshma.apptwo` | `com.aeshma.apptwo` | `AppTwoStandalone` | AppTwo |
| Super App | `com.aeshma.superapp` | `com.aeshma.superapp` | `SuperApp` | AppOne, AppTwo |

Standalone apps still create focused binaries for one app experience. The Super App is an installable product that acts as a gateway to multiple precompiled app experiences.

The selected binary injects a `ProductId`. Shared Kotlin configuration resolves that product into one or more supported `AppId` experiences. A standalone product auto-enters its only experience; the Super App shows an experience picker before login.

The feature implementations are present in the shared binary. This sample demonstrates runtime capability-based availability, not per-app binary exclusion.

## Architecture In One View

```text
Android flavor / iOS target
    -> injects ProductId
    -> ProductCatalog resolves supported AppId experiences
    -> standalone products auto-select, Super App shows a picker
    -> Metro creates the selected experience runtime
    -> login calls AuthRepository
    -> AppCatalog resolves AppId + username
    -> AppContext contains typed capabilities
    -> FeatureRegistry selects visible features
    -> DeliveryPolicyResolver selects delivery behavior
    -> SharedSessionSnapshot crosses the iOS boundary
    -> TCA stores presentation state
    -> SwiftUI renders the returned features and actions
```

```mermaid
flowchart TD
    Product["Android flavor or iOS target"] --> ProductId["ProductId"]
    ProductId --> ProductCatalog["ProductCatalog"]
    ProductCatalog --> AppId["Selected AppId experience"]
    AppId --> Runtime["Metro AppRuntime"]
    Runtime --> Login["Login with username"]
    Login --> Auth["AuthRepository"]
    Auth --> Catalog["AppCatalog"]
    Catalog --> Context["AppContext and capabilities"]
    Context --> Registry["FeatureRegistry"]
    Context --> Resolver["DeliveryPolicyResolver"]
    Registry --> Features["Available features"]
    Resolver --> Policy["Delivery policy"]
    Policy --> Actions["Allowed actions by order state"]
    Features --> UI["Compose or SwiftUI"]
    Actions --> UI
```

The central rule is:

> `ProductId` selects the installed product. `AppId` selects the active experience. Capabilities select features. Policies select behavior. Native UI renders the result.

## The Five Decisions

| Decision | Question | Implemented by |
|---|---|---|
| Product selection | Which installed product is running? | Android flavor or iOS target |
| Experience selection | Which app experience is active inside this product? | `ProductCatalog` and the Super App picker |
| Profile resolution | Which configuration belongs to this app and user? | `AppCatalog` |
| Feature selection | Which features may the user enter? | `FeatureRegistry` |
| Behavior selection | What may the user do inside a feature? | `DeliveryPolicyResolver` and `DeliveryPolicy` |

These decisions are separate deliberately. Screens do not contain scattered checks for app names, usernames, or roles.

## 1. Product Selection

The platform packaging layer supplies the product identity.

Android flavors define `BuildConfig.PRODUCT_ID` in `androidApp/build.gradle.kts`:

```kotlin
productFlavors {
    create("appOne") {
        applicationId = "com.aeshma.appone"
        buildConfigField("String", "PRODUCT_ID", "\"AppOneStandalone\"")
    }
    create("appTwo") {
        applicationId = "com.aeshma.apptwo"
        buildConfigField("String", "PRODUCT_ID", "\"AppTwoStandalone\"")
    }
    create("superApp") {
        applicationId = "com.aeshma.superapp"
        buildConfigField("String", "PRODUCT_ID", "\"SuperApp\"")
    }
}
```

`MainActivity` passes that identity into shared Compose UI:

```kotlin
ProductApp(productId = ProductId.fromExternalName(BuildConfig.PRODUCT_ID))
```

iOS has three native entry points:

```swift
// iosApp/AppOne/AppOneApp.swift
private let store = MultiAppStoreFactory.make(appIdName: "AppOne")

// iosApp/AppTwo/AppTwoApp.swift
private let store = MultiAppStoreFactory.make(appIdName: "AppTwo")

// iosApp/AppSuper/SuperAppApp.swift
private let productRoot = IOSProductCompositionRoot(productIdName: "SuperApp")
```

Each standalone `@main` app owns its store and passes it to `MultiAppRootView`. The Super App owns a small gateway view that reads supported experiences from shared Kotlin, then creates the selected experience store with the same `MultiAppStoreFactory` path used by standalone apps.

`ProductId` is used at the packaging boundary. `AppId` is used after a product has selected an experience. They should stay separate: a Super App is not itself an `AppId`; it is a product that may launch multiple `AppId` experiences.

### ProductCatalog

`ProductCatalog` maps installable products to their supported experiences:

```kotlin
ProductDefinition(
    id = ProductId.SuperApp,
    displayName = "Super App",
    supportedExperiences = setOf(AppId.AppOne, AppId.AppTwo),
    defaultExperience = null,
    showsExperiencePicker = true,
)
```

`ProductRuntime` validates every selected experience against this allowlist. That keeps standalone apps locked to one experience while allowing the Super App to route into AppOne or AppTwo.

### Hard-Coded Experience And Business Unit Config

The sample now models app experiences separately from product packaging. The hard-coded matrix is intentionally small, but it is structured like config so it can move to remote config later.

| Stable app ID | Experience display | Theme | Sites | Supported BU |
|---|---|---|---|---|
| `AppOne` | `Newport&Buckhead` | `SSMG Boutique Theme` | `BHNP` | `SSMG` |
| `AppTwo` | `Shop` | `Broadline Theme` | `USBL` | `USBL` |

The matching business-unit config is:

| Business unit | Allowed experiences |
|---|---|
| `SSMG` | `AppOne` / `Newport&Buckhead` |
| `USBL` | `AppTwo` / `Shop` |

The important filter is the intersection:

```text
available experiences =
    product.supportedExperiences
        ∩ businessUnit.allowedExperiences
        ∩ experience.supportedBusinessUnits
```

That means:

- `AppOneStandalone` can only expose `Newport&Buckhead`.
- `AppTwoStandalone` can only expose `Shop`.
- `SuperApp` knows both experiences, then filters them by the selected/authenticated BU.

For now the Super App UI has a demo BU picker. In production, that value should come from login and the picker would only render the experiences allowed for the authenticated user.

Roles and permissions are also represented as config-shaped objects:

```kotlin
PermissionTemplate(
    role = RoleId.CustomerAdmin,
    grantedCapabilities = CommerceCapabilities.of(
        "orders.view",
        "orders.edit",
        "lists.edit",
        "catalog.recommendations",
        "delivery.map",
    ),
)
```

`ProductRuntime.resolvedCommerceCapabilities(...)` treats role templates and explicit user permissions as user grants, then intersects those grants with business-unit allowed capabilities and experience supported capabilities. Feature registries and feature policies should read from that resolved set instead of checking raw roles in UI code.

## 2. AppCatalog And AppContext

`AppCatalog` is still part of the current architecture and is required by the sample. It is not an obsolete step and it is not a service locator.

Its focused responsibility is:

```text
(AppId, username) -> validated AppDefinition/UserProfile -> AppContext
```

Each `AppDefinition` contains the app metadata, default username, and available user profiles. Each `UserProfile` contains a `UserType` and typed `UserCapabilities`.

```kotlin
data class AppDefinition(
    val id: AppId,
    val displayName: String,
    val configKey: String,
    val defaultUsername: String,
    val profiles: Map<String, UserProfile>,
)
```

During login, `LocalAuthRepository` delegates profile lookup to the catalog:

```kotlin
class LocalAuthRepository(
    private val appCatalog: AppCatalog,
) : AuthRepository {
    override suspend fun login(appId: AppId, username: String): AppContext =
        appCatalog.contextFor(appId, username)
}
```

The catalog validates the app, normalizes the username, resolves the profile, and creates the session context:

```kotlin
fun contextFor(appId: AppId, username: String): AppContext {
    val definition = definition(appId)
    val normalizedUsername = username.normalizedUsername()
    val profile = definition.profileFor(normalizedUsername)

    return AppContext(
        appId = definition.id,
        userId = "${definition.configKey}-$normalizedUsername",
        userType = profile.userType,
        capabilities = profile.capabilities,
        businessUnitId = businessUnitFor(definition.id),
        roles = rolesFor(profile, normalizedUsername),
        explicitPermissions = explicitPermissionsFor(profile, normalizedUsername),
        commerceCapabilities = commerceCapabilitiesFor(...),
    )
}
```

`AppContext` is the authenticated source of truth used by downstream shared logic:

```kotlin
data class AppContext(
    val appId: AppId,
    val userId: String,
    val userType: UserType,
    val capabilities: UserCapabilities,
    val businessUnitId: BusinessUnitId,
    val roles: Set<RoleId>,
    val explicitPermissions: Set<PermissionId>,
    val commerceCapabilities: CommerceCapabilities,
)
```

The old typed `UserCapabilities` remains because the delivery policy sample still demonstrates strategy-based behavior. The commerce feature list now reads from `commerceCapabilities`, which is resolved by intersecting user grants with experience and business-unit capability ceilings.

```text
effective capabilities =
    (role template grants ∪ explicit user permissions)
        ∩ businessUnit.allowedCapabilities
        ∩ experience.supportedCapabilities
```

In production, `LocalAuthRepository` could be replaced by a remote repository that maps an API response into the same `AppContext`. The rest of the app should continue to read `AppContext` rather than raw network payloads.

## 3. Permission-String Capabilities And Feature Selection

A commerce capability is a permission string such as `orders.view`, `lists.edit`, or `delivery.map`, represented by the shared `PermissionId` value class. This keeps the sample close to the real authorization shape while still giving Kotlin call sites a stable type.

```kotlin
data class CommerceCapabilities(
    val permissions: Set<PermissionId> = emptySet(),
)
```

`FeatureRegistry` now uses permission-string capabilities to select visible features and expose enabled feature tweaks:

```kotlin
private val features = listOf(
    FeatureDescriptor(
        id = FeatureId.Orders,
        title = "Orders",
        requiredPermission = PermissionId.OrdersView,
        tweakPermissions = listOf(
            PermissionId.OrdersEdit,
            PermissionId.OrdersNotifications,
        ),
    ),
    FeatureDescriptor(
        id = FeatureId.Delivery,
        title = "Delivery",
        requiredPermission = PermissionId.DeliveryView,
        tweakPermissions = listOf(
            PermissionId.DeliveryEdit,
            PermissionId.DeliveryProgress,
            PermissionId.DeliveryStatus,
            PermissionId.DeliveryMap,
            PermissionId.DeliveryInvoices,
        ),
    ),
)
```

Feature selection answers: **may this feature be shown and opened?** Feature tweaks answer: **which sub-capabilities should be enabled inside that feature?**

The dummy `Home`, `Profile`, `Reports`, and `Payments` examples have been replaced by commerce features:

| Feature | Required permission | Tweaks |
|---|---|
| Orders | `orders.view` | `orders.edit`, `orders.notifications` |
| Lists | `lists.view` | `lists.edit`, `lists.purchaseHistory` |
| Catalog | `catalog.view` | `catalog.recommendations` |
| Product Details | `pdp.view` | `pdp.internalDetails` |
| Delivery | `delivery.view` | `delivery.edit`, `delivery.progress`, `delivery.status`, `delivery.map`, `delivery.invoices` |

The UI may switch on a stable `FeatureId` to navigate to the appropriate screen. It should not reproduce role checks or business-unit checks to decide visibility; those belong in the config resolution path that produces `commerceCapabilities`.

## 4. Policies And Experiences

A policy is a replaceable business-rule object. All delivery experiences implement the same contract:

```kotlin
interface DeliveryPolicy {
    val experienceName: String
    fun availableActions(order: DeliveryOrder): List<DeliveryAction>
    fun canOpenDeliveryDetails(order: DeliveryOrder): Boolean
}
```

Feature availability and experience behavior are different:

- `FeatureRegistry` decides whether Delivery is visible.
- `DeliveryPolicyResolver` selects how Delivery behaves.
- The selected `DeliveryPolicy` calculates actions for each order state.

```kotlin
class DeliveryPolicyResolver {
    fun resolve(context: AppContext): DeliveryPolicy {
        val delivery = context.capabilities.delivery
            ?: return DisabledDeliveryPolicy

        return when (delivery.mode) {
            DeliveryMode.Customer -> CustomerDeliveryPolicy(delivery)
            DeliveryMode.Driver -> DriverDeliveryPolicy(delivery)
            DeliveryMode.Admin -> AdminDeliveryPolicy(delivery)
            DeliveryMode.Merchant -> MerchantDeliveryPolicy(delivery)
            DeliveryMode.ReadOnly -> ReadOnlyDeliveryPolicy
        }
    }
}
```

The resolver examines `DeliveryMode`, not `AppId`. Another app can reuse an existing delivery experience by assigning the same capability mode.

The policy then combines capability flags with current domain state. For example, a created order may produce:

| Experience | Possible actions |
|---|---|
| Customer | Cancel, EditAddress, Track |
| Driver | Accept |
| Admin | Cancel, EditAddress, Track |
| Merchant | Track |
| ReadOnly | ViewOnly |

This is the Strategy pattern: calling code depends on one interface while the resolver supplies the correct implementation.

## Product Runtime, Shared Session And Metro

`ProductRuntime` is the layer above an individual app runtime. It owns the selected `ProductDefinition`, exposes the supported app experiences, and validates that every selected `AppId` belongs to that product.

```kotlin
class ProductRuntime internal constructor(
    val productDefinition: ProductDefinition,
    private val appCatalog: AppCatalog,
) {
    val supportedExperiences = productDefinition.supportedExperiences
        .map(appCatalog::definition)

    fun appRuntimeFor(appId: AppId): AppRuntime
}
```

Android uses this through `ProductApp(productId)`. Standalone products have a `defaultExperience`, so they go straight to the existing login/features flow with the configured experience title/theme. The Super App has no default experience, so the UI shows the gateway picker first and filters the picker by demo BU.

Metro creates the shared object graph in `AppGraph`:

```text
AppGraph
  AppCatalog
  AuthRepository
  FeatureRegistry
  DeliveryPolicyResolver
  DeliveryRepository
  AnalyticsClient
  AppSession
```

`AppSession` is the shared application-facing API. It logs in, stores the current `AppContext`, exposes available features, resolves the delivery policy, returns sample orders, tracks analytics, and clears the context on logout.

Metro performs construction and dependency injection for a selected app experience. It does not decide product routing, feature availability, or business behavior; those responsibilities remain in `ProductCatalog`, the registry, and policies.

## iOS: KMP, TCA, Dependencies And SKIE

The iOS application is native SwiftUI backed by shared Kotlin business logic.

### Ownership Boundary

| Shared KMP owns | Native iOS owns |
|---|---|
| Product, app definitions and profile lookup | AppOne/AppTwo/SuperApp entry targets |
| `AppCatalog` and `AppContext` | SwiftUI views and controls |
| Capabilities and feature selection | TCA state, actions and navigation |
| Delivery policies and actions | Native snapshot models |
| Session lifecycle and analytics | KMP client bridge and actor isolation |
| Metro dependency graph | Point-Free dependency injection |

SwiftUI does not contain AppOne/AppTwo permission matrices or delivery business rules.

### iOS Login Sequence

```mermaid
sequenceDiagram
    participant View as "SwiftUI"
    participant Store as "TCA Store"
    participant Client as "MultiAppClient actor"
    participant Root as "IOSAppCompositionRoot"
    participant Session as "AppSession"
    participant Auth as "AuthRepository"
    participant Catalog as "AppCatalog"

    View->>Store: "loginTapped"
    Store->>Client: "login(username)"
    Client->>Root: "SKIE async login"
    Root->>Session: "login(appId, username)"
    Session->>Auth: "login(appId, username)"
    Auth->>Catalog: "contextFor(appId, username)"
    Catalog-->>Session: "AppContext"
    Session-->>Root: "features, policy and orders"
    Root-->>Client: "SharedSessionSnapshot"
    Client-->>Store: "NativeSessionSnapshot"
    Store-->>View: "updated State"
```

`IOSAppCompositionRoot` is the small facade-like boundary exposed to Swift. It owns the same shared runtime used by Android and returns immutable platform-friendly snapshots:

```kotlin
class IOSAppCompositionRoot(appIdName: String) {
    private val runtime = createAppRuntime(AppId.fromExternalName(appIdName))

    suspend fun login(username: String): SharedSessionSnapshot {
        val context = runtime.session.login(runtime.appId, username)
        return snapshotMapper.map(context, runtime.session)
    }

    fun logout() {
        runtime.session.logout()
    }
}
```

`SessionSnapshotMapper` obtains visible features and delivery actions from the shared session. Swift receives results, not rule objects that it must reinterpret.

### iOS Super App Gateway Sequence

The iOS Super App target uses the same shared product catalog as Android:

```mermaid
sequenceDiagram
    participant SuperApp as "SuperAppApp"
    participant ProductRoot as "IOSProductCompositionRoot"
    participant ProductRuntime as "ProductRuntime"
    participant StoreFactory as "MultiAppStoreFactory"
    participant Experience as "MultiAppRootView"

    SuperApp->>ProductRoot: "IOSProductCompositionRoot(SuperApp)"
    ProductRoot->>ProductRuntime: "supportedExperiences"
    ProductRuntime-->>ProductRoot: "Newport&Buckhead, Shop"
    ProductRoot-->>SuperApp: "SharedAppExperience list"
    SuperApp->>ProductRoot: "supportedExperiencesForBusinessUnit(BU)"
    SuperApp->>SuperApp: "user selects experience"
    SuperApp->>StoreFactory: "make(appIdName)"
    StoreFactory-->>Experience: "StoreOf<MultiAppFeature>"
```

The gateway screen lives in `iosApp/AppSuper/SuperAppApp.swift`. It does not duplicate the product catalog in Swift; it asks shared Kotlin which experiences the `SuperApp` product supports and which of those are allowed for the selected/authenticated BU.

### What Each iOS Tool Does

- **SwiftUI** renders screens and sends user events.
- **TCA State** stores the values currently displayed by SwiftUI.
- **TCA Action** represents an event such as login, logout, or feature selection.
- **TCA Reducer** updates state and starts asynchronous effects.
- **Point-Free Dependencies** injects `MultiAppClient` into the reducer and allows tests to replace it.
- **SKIE** exposes Kotlin `suspend` login as a normal Swift `async` function.
- **`IOSAppGateway` actor** serializes access to the mutable shared `AppSession`.

The native path is:

```text
AppOneApp/AppTwoApp
    -> MultiAppRootView
    -> TCA Store with live MultiAppClient
    -> IOSAppGateway actor
    -> IOSAppCompositionRoot in SharedLogic.framework
    -> AppSession and shared business rules
    -> snapshot mapped to native Swift models
    -> reducer updates State
    -> SwiftUI re-renders
```

The Super App adds one gateway step before that same experience path:

```text
SuperAppApp
    -> IOSProductCompositionRoot(productIdName = "SuperApp")
    -> shared ProductCatalog returns AppOne and AppTwo
    -> user selects an experience
    -> MultiAppStoreFactory.make(appIdName)
    -> existing MultiAppRootView/TCA/KMP login flow
```

The current delivery view displays the action names selected by shared policies. Executing complete delivery workflows is outside this sample.

## Module Structure

```text
androidApp/                    Android shell and product flavors
iosApp/
  AppOne/                     AppOne SwiftUI entry point and assets
  AppTwo/                     AppTwo SwiftUI entry point and assets
  AppSuper/                   Super App SwiftUI gateway entry point and assets
  SharedIOS/
    NativeModels.swift        Native presentation snapshots
    MultiAppClient.swift      TCA dependency contract
    LiveMultiAppClient.swift  Actor-isolated KMP implementation
    MultiAppStoreFactory.swift Native store and dependency composition
    MultiAppFeature.swift     TCA reducer, state and actions
    MultiAppView.swift        SwiftUI views
  SharedIOSTests/             TCA reducer tests
shared/
  core/model/                 App, commerce capability and feature contracts
  core/config/                Definitions, profiles, experience/BU config, presets and catalog
  core/analytics/             Analytics abstraction
  features/delivery/          Delivery domain, repository and policies
  application/                Product runtime, Metro graph, session and iOS composition roots
  ui/                         Shared Compose presentation used by Android
```

## End-To-End Examples

### AppOne Customer

```text
AppOne target/flavor
    -> ProductId("AppOneStandalone")
    -> default AppId("AppOne")
    -> login("customer")
    -> AppCatalog selects AppOne customer profile
    -> AppContext receives SSMG BU, Customer role and resolved commerce permissions
    -> FeatureRegistry returns Orders, Lists, Catalog, Product Details and Delivery
    -> DeliveryPolicyResolver returns CustomerDeliveryPolicy
    -> SwiftUI/Compose renders those features, enabled tweaks and customer actions
```

### AppTwo Merchant

```text
AppTwo target/flavor
    -> ProductId("AppTwoStandalone")
    -> default AppId("AppTwo")
    -> login("merchant")
    -> AppCatalog selects AppTwo merchant profile
    -> AppContext receives USBL BU, Customer Admin role and resolved commerce permissions
    -> FeatureRegistry returns Orders, Lists, Catalog, Product Details and Delivery
    -> DeliveryPolicyResolver returns MerchantDeliveryPolicy
    -> SwiftUI/Compose renders those features, enabled tweaks and merchant actions
```

### Super App Customer

```text
SuperApp flavor/target
    -> ProductId("SuperApp")
    -> ProductCatalog exposes AppOne and AppTwo
    -> user selects a business unit
    -> Super App filters available experiences by product, BU and experience config
    -> user selects AppOne / Newport&Buckhead
    -> login("customer")
    -> AppCatalog selects AppOne customer profile
    -> AppContext receives SSMG BU, Customer role and resolved commerce permissions
    -> FeatureRegistry returns permission-driven commerce features
```

## Running Android

Prerequisites: Android Studio, Android SDK 36, JDK 21, and an emulator or device running API 30 or newer.

1. Open the repository root in Android Studio.
2. Wait for Gradle sync.
3. Open **View > Tool Windows > Build Variants**.
4. Select `appOneDebug`, `appTwoDebug`, or `superAppDebug` for `androidApp`.
5. Select the `androidApp` run configuration and a device.
6. Click **Run**.

Command-line builds:

```bash
./gradlew :androidApp:assembleAppOneDebug
./gradlew :androidApp:assembleAppTwoDebug
./gradlew :androidApp:assembleSuperAppDebug
```

## Running iOS

Prerequisites: full Xcode, an iOS Simulator runtime, JDK 21, and permission for Xcode to resolve and run the pinned Point-Free package macros.

1. Open `iosApp/iosApp.xcodeproj` in Xcode.
2. Select the `AppOne`, `AppTwo`, or `SuperApp` scheme.
3. Select an iPhone simulator.
4. Press `Cmd+R`.

All iOS targets build `SharedLogic.framework` with:

```bash
./gradlew :shared:application:embedAndSignAppleFrameworkForXcode
```

If Kotlin/Swift symbols are stale, use **Product > Clean Build Folder** and rebuild. Confirm that the Gradle framework build phase completes successfully.

Command-line simulator builds:

```bash
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
  xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme AppOne \
  -destination 'generic/platform=iOS Simulator' \
  -skipMacroValidation \
  CODE_SIGNING_ALLOWED=NO build

DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
  xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme AppTwo \
  -destination 'generic/platform=iOS Simulator' \
  -skipMacroValidation \
  CODE_SIGNING_ALLOWED=NO build

DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
  xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme SuperApp \
  -destination 'generic/platform=iOS Simulator' \
  -skipMacroValidation \
  CODE_SIGNING_ALLOWED=NO build
```

## Testing

Run shared tests:

```bash
./gradlew \
  :shared:core:config:testAndroidHostTest \
  :shared:features:delivery:testAndroidHostTest \
  :shared:application:testAndroidHostTest
```

Run native TCA reducer tests:

```bash
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
  xcrun swift test --package-path iosApp
```

The tests cover catalog lookup and validation, capability-based feature selection, policy selection and actions, Metro composition, session lifecycle, iOS composition metadata, TCA login/logout, cancellation, and navigation.

## Extending The Architecture

### Add A New App

1. Add an `AppId`.
2. Create an `AppDefinition` with profiles and capability presets.
3. Register it in `defaultAppDefinitions()`.
4. Decide which products can launch it and update `defaultProductDefinitions()`.
5. Add an Android flavor only if this app should also ship as a standalone product.
6. Add an iOS target and entry point only if this app should also ship as a standalone product.
7. Add catalog and platform wiring tests.

No new feature or policy branch is needed when the app reuses existing capabilities.

### Add A New Feature

1. Add a stable `FeatureId`.
2. Add typed capability data.
3. Add a descriptor to `FeatureRegistry`.
4. Create a feature module when it has meaningful independent domain logic.
5. Add its repository, policy, or use cases.
6. Bind dependencies in `AppGraph`.
7. Add Compose and SwiftUI presentation.
8. Add shared and platform tests.

## Rules To Preserve

1. Platform packaging injects product identity.
2. `ProductCatalog` resolves supported experiences; `AppCatalog` resolves app and profile configuration.
3. `AppContext` is the typed authenticated source of truth.
4. Capabilities control access; role names do not act as permission checks.
5. `FeatureRegistry` controls feature visibility.
6. Policies control behavior inside a feature.
7. Native UI renders shared decisions instead of rebuilding them.
8. Metro constructs shared dependencies; TCA Dependencies injects native clients.
9. Unknown apps and profiles fail explicitly.
10. Stable IDs remain separate from display text.
11. `ProductId`, `AppId`, and tenant/customer IDs stay separate.
