# Feature Mechanism And Tab Flow

This document explains the full demo flow from login to experience selection, feature resolution, bottom-tab layout, and permission-driven UI inside each feature.

## Big Picture

The app is simulating a product/experience/permission architecture:

1. The installed product decides which configured experiences can ever launch.
2. Login returns hardcoded BU, roles, permissions, and domain capabilities.
3. The resolver finds the eligible configured experience or experiences for that BU.
4. The selected experience contributes theme, host app shell, allowed sites, supported features, and supported capabilities.
5. The final `AppContext` is created by intersecting role permissions, explicit login permissions, BU capabilities, and experience capabilities.
6. `FeatureRegistry` uses the final context to decide which feature modules are visible.
7. Android and iOS render the resolved features as bottom tabs.
8. Each feature renders only the UI blocks/actions allowed by the resolved permissions.

## Core Terms

| Term | Meaning |
|---|---|
| Product | Installed app binary/shell, such as `AppOneStandalone`, `AppTwoStandalone`, or `SuperApp`. |
| App shell | Native host app id, currently `AppOne` or `AppTwo`. |
| Experience | Business experience config, such as `Shop` or `Newport&Buckhead`. This is not the same as AppOne/AppTwo. |
| BU | Business unit returned from login, such as `USBL`, `SSMG`, or `CABL`. |
| Role | Coarse role such as `CUSTOMER`, `CUSTOMER_ADMIN`, or `DELIVERY_USER`. |
| Permission | Fine-grained commerce capability such as `orders.edit`. |
| Feature | Coarse screen/module such as Orders, Lists, Catalog, Product Details, or Delivery. |
| Tweak | Optional feature behavior enabled by an extra permission. |

## Config Sources

| Area | File |
|---|---|
| Product/experience build bundle config | `config/product-feature-bundles.json` |
| Product definitions | `shared/core/config/.../ProductCatalog.kt` |
| Experience definitions | `shared/core/config/.../ExperienceCatalog.kt` |
| BU definitions | `shared/core/config/.../CapabilityPresets.kt` |
| Hardcoded login grants | `shared/core/config/.../HardcodedLoginConfig.kt` |
| Feature ids and feature definition model | `shared/core/model/.../FeatureId.kt` |
| Feature registry | `shared/application/.../FeatureRegistry.kt` |
| Android tab UI | `shared/ui/.../App.kt` |
| iOS tab UI | `iosApp/SharedIOS/MultiAppView.swift` |

## Login To Experience Flow

### Single-App Products

`AppOneStandalone` and `AppTwoStandalone` each support one configured experience.

| Product | Login BU | Resolved experience |
|---|---|---|
| `AppOneStandalone` | `USBL` | `Shop` |
| `AppTwoStandalone` | `SSMG` | `Newport&Buckhead` |

Because there is only one eligible experience, the app launches it directly after login.

### SuperApp

`SuperApp` supports both configured experiences:

| Experience | Supported BUs |
|---|---|
| `Shop` | `USBL`, `CABL` |
| `Newport&Buckhead` | `SSMG`, `CABL` |

For the demo, `SuperApp` + `admin` returns `CABL`, which is eligible for both experiences. That creates the multi-experience path:

```text
SuperApp login admin
  -> hardcoded grant returns CABL
  -> CABL can access Shop and Newport&Buckhead
  -> product supports both
  -> show experience switcher
```

When the user chooses an experience, the app launches the selected experience with its own theme and feature set.

## Experience Eligibility

An experience is eligible only when all of these are true:

```text
product supports the experience
AND BU config allows the experience
AND experience config supports the BU
```

This is why a user can have a permission but still not see a feature. The selected experience must support the coarse feature first.

## Capability Resolution

The final permission set is an intersection:

```text
final commerce capabilities =
  role template permissions
  + explicit login permissions
  intersect BU supported capabilities
  intersect experience supported capabilities
```

Example:

```text
role grants catalog.view
selected experience supports catalog
BU supports catalog
result: catalog.view is available
```

But:

```text
role grants catalog.view
selected experience does not support Catalog
result: Catalog feature stays hidden
```

The final result is stored in `AppContext.commerceCapabilities`.

## Feature Modules

Each dummy feature is a KMP module:

| Feature | Module | Feature id |
|---|---|---|
| Orders | `shared/features/orders` | `orders` |
| Lists | `shared/features/lists` | `lists` |
| Catalog | `shared/features/catalog` | `catalog` |
| Product Details | `shared/features/productdetails` | `product-details` |
| Delivery | `shared/features/delivery` | `delivery` |

Each module owns a `FeatureDefinitionSpec`.

```kotlin
data class FeatureDefinitionSpec(
    val id: FeatureId,
    val title: String,
    val requiredPermission: PermissionId,
    val tweakPermissions: List<PermissionId>,
    val permissionRows: List<FeaturePermissionRow>,
    val actions: List<FeatureActionDefinition>,
    val uiBlocks: List<FeatureUiBlock>,
)
```

This means the application layer does not hardcode Orders/List/Catalog behavior inline. It imports feature definitions from modules and applies one generic resolution path.

## Feature Registry

`FeatureRegistry` registers feature module definitions:

```kotlin
private val features = listOf(
    descriptor(OrdersFeature.definition),
    descriptor(ListsFeature.definition),
    descriptor(CatalogFeature.definition),
    descriptor(ProductDetailsFeature.definition),
    descriptor(DeliveryFeature.definition),
)
```

Then it filters them:

```kotlin
features.filter {
    it.id in context.supportedFeatures && it.isAvailable(context)
}
```

The two checks mean:

1. `context.supportedFeatures`: the selected experience supports the coarse feature.
2. `isAvailable(context)`: the final permissions include the feature's required permission.

For example, Orders appears only when:

```text
selected experience supports FeatureId.Orders
AND final permissions include orders.view
```

## Inside A Feature

Each feature definition has three important UI/action sections.

### Permission Rows

Permission rows show enabled/disabled state for related capabilities.

Example from Orders:

```text
View orders -> orders.view
Edit orders -> orders.edit
Order notifications -> orders.notifications
```

Android evaluates these directly from `AppContext`.

iOS receives the already-evaluated rows through `SharedFeaturePermissionRowSnapshot`:

```kotlin
SharedFeaturePermissionRowSnapshot(
    label = row.label,
    permission = row.permission.value,
    enabled = context.commerceCapabilities.has(row.permission),
)
```

### UI Blocks

UI blocks are permission-gated dummy panels.

Example from Product Details:

```text
Product summary
  required permission: pdp.view

Internal product details
  required permission: pdp.internalDetails
```

If the login only resolves `pdp.view`, the normal Product summary appears but Internal product details is hidden.

Android filters blocks in Compose:

```kotlin
val visibleBlocks = feature.uiBlocks.filter {
    context.hasPermission(it.requiredPermission)
}
```

iOS receives only visible blocks in `SharedFeatureUiBlockSnapshot`:

```kotlin
uiBlocks = feature.uiBlocks
    .filter { context.commerceCapabilities.has(it.requiredPermission) }
```

### Actions

Feature actions are also permission-gated.

Example from Lists:

```text
Open    -> lists.view
Rename  -> lists.edit
History -> lists.purchaseHistory
```

If the login does not resolve `lists.edit`, the Rename action is not shown.

When an allowed action is tapped, the UI updates local demo state with the action result string. This proves the action pipeline works without requiring a backend.

## Delivery Is Special

Delivery still uses the generic feature definition for permission rows and UI blocks, but its actions are stateful.

Delivery action availability comes from:

```text
DeliveryPolicyResolver
  -> reads context.capabilities.delivery
  -> selects customer/driver/admin/merchant/read-only policy
  -> calculates allowed actions for each order state
```

Example:

```text
Created order + driver policy
  -> Accept

Assigned order + driver policy
  -> MarkPickedUp, Track

PickedUp order + driver policy
  -> MarkDelivered, Track
```

When a delivery action is tapped, the order status changes and the allowed actions are recalculated.

## Bottom Tab Layout

After login, both Android and iOS now use an app-style bottom tab shell.

The layout rule is:

```text
first 4 resolved features -> direct bottom tabs
remaining features -> More tab
```

The default selected tab is the first resolved feature.

Examples:

```text
AppOne / Shop
  features: Orders, Catalog, Product Details, Delivery
  tabs: Orders, Catalog, Product Details, Delivery
```

```text
AppTwo / Newport&Buckhead
  features: Orders, Lists, Delivery
  tabs: Orders, Lists, Delivery
```

```text
SuperApp / CABL / Shop
  features: Orders, Catalog, Product Details, Delivery
  tabs: Orders, Catalog, Product Details, Delivery
```

```text
SuperApp / CABL / Newport&Buckhead
  features: Orders, Lists, Delivery
  tabs: Orders, Lists, Delivery
```

If a future experience resolves more than four features:

```text
tabs: Feature 1, Feature 2, Feature 3, Feature 4, More
More: Feature 5, Feature 6, ...
```

## Android Rendering

Android uses shared Compose UI in:

```text
shared/ui/src/commonMain/kotlin/com/aeshma/multiapp/ui/App.kt
```

Main pieces:

| Composable | Responsibility |
|---|---|
| `FeatureListScreen` | Post-login bottom tab shell. |
| `FeatureTabContent` | Chooses generic feature rendering or Delivery rendering. |
| `GenericFeatureView` | Renders permission rows, visible UI blocks, and allowed actions. |
| `DeliveryView` | Renders delivery permission rows, UI blocks, orders, and stateful delivery actions. |
| `MoreFeaturesList` | Lists overflow features when there are more than four. |

Android gets `FeatureDescriptor` objects directly from shared KMP:

```kotlin
features = session.availableFeatures()
```

So Android can evaluate permission rows, visible UI blocks, and actions directly against `AppContext`.

## iOS Rendering

iOS uses native SwiftUI in:

```text
iosApp/SharedIOS/MultiAppView.swift
```

Main pieces:

| SwiftUI view | Responsibility |
|---|---|
| `FeatureTabShellView` | Post-login bottom `TabView` shell. |
| `FeatureTabContentView` | Chooses generic feature rendering or Delivery rendering. |
| `GenericFeatureContentView` | Renders normal feature screens. |
| `GenericFeatureContentRows` | Renders visible UI blocks and allowed actions. |
| `DeliveryFeatureContentView` | Renders delivery feature content and stateful delivery actions. |
| `MoreFeaturesView` | Lists overflow features when there are more than four. |

iOS receives a `SharedSessionSnapshot` from KMP. `LiveMultiAppClient.swift` maps it into native Swift structs:

```text
SharedFeatureSnapshot
  -> NativeFeature
```

`NativeFeature` includes:

- id
- title
- required permission
- enabled tweaks
- permission rows
- allowed actions
- visible UI blocks

The important difference is that iOS does not recompute feature permissions in Swift. KMP exports the already-resolved/filtered data.

## Data Flow Diagram

```text
Login username
  -> HardcodedLoginConfig
  -> BU + roles + explicit permissions + domain capabilities
  -> ProductRuntime resolves eligible experiences
  -> single experience launches OR switcher is shown
  -> selected ExperienceDefinition
  -> AppContext with intersected capabilities
  -> FeatureRegistry.availableFeatures(context)
  -> Android FeatureDescriptor list
  -> iOS SharedSessionSnapshot / NativeFeature list
  -> bottom tab shell
  -> feature screen
  -> permission rows + visible UI blocks + allowed actions
```

## Product Bundling Simulation

`config/product-feature-bundles.json` declares which coarse features belong to each product build:

```json
"bundledFeatures": ["orders", "catalog", "product-details", "delivery"]
```

This is currently a simulation. The app still links all dummy feature modules so every path can be demonstrated locally. The config shows what a real product-specific build could include.

In a production version:

- Android product flavors could include only the modules listed for that flavor.
- iOS targets or framework variants could link only the modules listed for that target.
- Runtime would still apply the same experience/BU/permission intersection.

## How To Test

### Android

Run one of:

```bash
./gradlew :androidApp:assembleAppOneDebug
./gradlew :androidApp:assembleAppTwoDebug
./gradlew :androidApp:assembleSuperAppDebug
```

Then log in with:

| Target | Login | Expected |
|---|---|---|
| AppOne | `user` or `customer` | Shop features as bottom tabs. |
| AppTwo | `user` or `customer` | Newport&Buckhead features as bottom tabs. |
| SuperApp | `admin` | CABL multi-experience switcher, then tabs for selected experience. |

### iOS

Run AppOne, AppTwo, or SuperApp from Xcode.

Use the same logins:

| Target | Login | Expected |
|---|---|---|
| AppOne | `user` or `customer` | Shop features as bottom tabs. |
| AppTwo | `user` or `customer` | Newport&Buckhead features as bottom tabs. |
| SuperApp | `admin` | CABL multi-experience switcher, then tabs for selected experience. |

If generated iOS constants are stale, run:

```bash
./gradlew generateIOSProductFeatureBundles
```

## What To Look For

1. Theme changes with the selected experience and cannot be freely switched.
2. Feature tabs differ by product, BU, and selected experience.
3. Permission rows show enabled/disabled status.
4. UI blocks appear only when the login has the required permission.
5. Actions appear only when allowed.
6. Tapping generic actions updates local result text.
7. Tapping Delivery actions changes order status and changes future available actions.

