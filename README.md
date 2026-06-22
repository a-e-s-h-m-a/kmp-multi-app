# Kotlin Multiplatform Multi-App Architecture

This sample builds two independently installable products from one Kotlin Multiplatform codebase:

| Product | Android application ID | iOS bundle ID | Default user |
|---|---|---|---|
| AppOne | `com.aeshma.appone` | `com.aeshma.appone` | `customer` |
| AppTwo | `com.aeshma.apptwo` | `com.aeshma.apptwo` | `admin` |

This is not one application with an app switcher. Android flavors and iOS targets create different binaries. The selected binary injects an `AppId`; shared Kotlin configuration then determines the available features and behavior for the logged-in user.

The feature implementations are present in the shared binary. This sample demonstrates runtime capability-based availability, not per-app binary exclusion.

## Architecture In One View

```text
Android flavor / iOS target
    -> injects AppId
    -> Metro creates the shared runtime
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
    Product["Android flavor or iOS target"] --> AppId["AppId"]
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

> `AppId` selects configuration. Capabilities select features. Policies select behavior. Native UI renders the result.

## The Four Decisions

| Decision | Question | Implemented by |
|---|---|---|
| Product selection | Which installed app is running? | Android flavor or iOS target |
| Profile resolution | Which configuration belongs to this app and user? | `AppCatalog` |
| Feature selection | Which features may the user enter? | `FeatureRegistry` |
| Experience selection | What may the user do inside a feature? | `DeliveryPolicyResolver` and `DeliveryPolicy` |

These decisions are separate deliberately. Screens do not contain scattered checks for app names, usernames, or roles.

## 1. Product Selection

The platform packaging layer supplies the product identity.

Android flavors define `BuildConfig.APP_ID` in `androidApp/build.gradle.kts`:

```kotlin
productFlavors {
    create("appOne") {
        applicationId = "com.aeshma.appone"
        buildConfigField("String", "APP_ID", "\"AppOne\"")
    }
    create("appTwo") {
        applicationId = "com.aeshma.apptwo"
        buildConfigField("String", "APP_ID", "\"AppTwo\"")
    }
}
```

`MainActivity` passes that identity into shared Compose UI:

```kotlin
App(appId = AppId.fromExternalName(BuildConfig.APP_ID))
```

iOS has two native entry points:

```swift
// iosApp/AppOne/AppOneApp.swift
MultiAppRootView(appIdName: "AppOne")

// iosApp/AppTwo/AppTwoApp.swift
MultiAppRootView(appIdName: "AppTwo")
```

`AppId` is used at composition and configuration boundaries. It should not become a global UI switch.

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
)
```

In production, `LocalAuthRepository` could be replaced by a remote repository that maps an API response into the same `AppContext`. `FeatureRegistry` and delivery policies would not need to change.

## 3. Capabilities And Feature Selection

A capability is typed data describing what a user is allowed to access or do. It is more precise than using a role name as permission logic.

```kotlin
data class UserCapabilities(
    val delivery: DeliveryCapability? = null,
    val reports: ReportsCapability? = null,
    val payments: PaymentsCapability? = null,
)
```

`FeatureRegistry` uses those capabilities to select visible features:

```kotlin
private val features = listOf(
    FeatureDescriptor(FeatureId.Home, "Home") { true },
    FeatureDescriptor(FeatureId.Delivery, "Delivery") {
        it.capabilities.delivery != null
    },
    FeatureDescriptor(FeatureId.Reports, "Reports") {
        it.capabilities.reports?.canViewReports == true
    },
    FeatureDescriptor(FeatureId.Payments, "Payments") {
        it.capabilities.payments?.canMakePayment == true
    },
    FeatureDescriptor(FeatureId.Profile, "Profile") { true },
)
```

Feature selection answers only: **may this feature be shown and opened?**

| Profile | Available features |
|---|---|
| AppOne `customer` | Home, Delivery, Payments, Profile |
| AppOne `driver` | Home, Delivery, Profile |
| AppTwo `admin` | Home, Delivery, Reports, Profile |
| AppTwo `merchant` | Home, Delivery, Reports, Profile |
| `readonly` | Home, Delivery, Profile |
| `nod` or a no-capability profile | Home, Profile |

The UI may switch on a stable `FeatureId` to navigate to the appropriate screen. It should not reproduce capability checks to decide visibility.

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

## Shared Session And Metro

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

Metro performs construction and dependency injection. It does not decide feature availability or business behavior; those responsibilities remain in the registry and policies.

## iOS: KMP, TCA, Dependencies And SKIE

The iOS application is native SwiftUI backed by shared Kotlin business logic.

### Ownership Boundary

| Shared KMP owns | Native iOS owns |
|---|---|
| App definitions and profile lookup | AppOne/AppTwo entry targets |
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

The current delivery view displays the action names selected by shared policies. Executing complete delivery workflows is outside this sample.

## Module Structure

```text
androidApp/                    Android shell and product flavors
iosApp/
  AppOne/                     AppOne SwiftUI entry point and assets
  AppTwo/                     AppTwo SwiftUI entry point and assets
  SharedIOS/
    NativeModels.swift        Native presentation snapshots
    MultiAppClient.swift      TCA dependency contract
    LiveMultiAppClient.swift  Actor-isolated KMP implementation
    MultiAppFeature.swift     TCA reducer, state and actions
    MultiAppView.swift        SwiftUI views
  SharedIOSTests/             TCA reducer tests
shared/
  core/model/                 App, capability and feature contracts
  core/config/                Definitions, profiles, presets and catalog
  core/analytics/             Analytics abstraction
  features/delivery/          Delivery domain, repository and policies
  application/                Metro graph, session and iOS composition root
  ui/                         Shared Compose presentation used by Android
```

## End-To-End Examples

### AppOne Customer

```text
AppOne target/flavor
    -> AppId("AppOne")
    -> login("customer")
    -> AppCatalog selects AppOne customer profile
    -> AppContext receives Customer delivery and Payments capabilities
    -> FeatureRegistry returns Home, Delivery, Payments, Profile
    -> DeliveryPolicyResolver returns CustomerDeliveryPolicy
    -> SwiftUI/Compose renders those features and customer actions
```

### AppTwo Merchant

```text
AppTwo target/flavor
    -> AppId("AppTwo")
    -> login("merchant")
    -> AppCatalog selects AppTwo merchant profile
    -> AppContext receives Merchant delivery and Reports capabilities
    -> FeatureRegistry returns Home, Delivery, Reports, Profile
    -> DeliveryPolicyResolver returns MerchantDeliveryPolicy
    -> SwiftUI/Compose renders those features and merchant actions
```

## Running Android

Prerequisites: Android Studio, Android SDK 36, JDK 21, and an emulator or device running API 30 or newer.

1. Open the repository root in Android Studio.
2. Wait for Gradle sync.
3. Open **View > Tool Windows > Build Variants**.
4. Select `appOneDebug` or `appTwoDebug` for `androidApp`.
5. Select the `androidApp` run configuration and a device.
6. Click **Run**.

Command-line builds:

```bash
./gradlew :androidApp:assembleAppOneDebug
./gradlew :androidApp:assembleAppTwoDebug
```

## Running iOS

Prerequisites: full Xcode, an iOS Simulator runtime, JDK 21, and permission for Xcode to resolve and run the pinned Point-Free package macros.

1. Open `iosApp/iosApp.xcodeproj` in Xcode.
2. Select the `AppOne` or `AppTwo` scheme.
3. Select an iPhone simulator.
4. Press `Cmd+R`.

Both targets build `SharedLogic.framework` with:

```bash
./gradlew :shared:application:embedAndSignAppleFrameworkForXcode
```

If Kotlin/Swift symbols are stale, use **Product > Clean Build Folder** and rebuild. Confirm that the Gradle framework build phase completes successfully.

Command-line simulator builds:

```bash
xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme AppOne \
  -destination 'generic/platform=iOS Simulator' \
  -skipMacroValidation \
  CODE_SIGNING_ALLOWED=NO build

xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme AppTwo \
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
4. Add an Android flavor.
5. Add an iOS target and entry point.
6. Add catalog and platform wiring tests.

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
2. `AppCatalog` resolves product and profile configuration.
3. `AppContext` is the typed authenticated source of truth.
4. Capabilities control access; role names do not act as permission checks.
5. `FeatureRegistry` controls feature visibility.
6. Policies control behavior inside a feature.
7. Native UI renders shared decisions instead of rebuilding them.
8. Metro constructs shared dependencies; TCA Dependencies injects native clients.
9. Unknown apps and profiles fail explicitly.
10. Stable IDs remain separate from display text.
