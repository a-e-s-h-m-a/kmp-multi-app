# Commerce Permission And Feature Resolution

This document explains how the sample resolves configured experiences, business units, roles, permissions, feature visibility, tweaks, and simulated business actions.

## Terms

| Term | Meaning |
|---|---|
| Product | Installed shell/binary, such as AppOne, AppTwo, or SuperApp. |
| `ProductId` | Stable product key: `AppOneStandalone`, `AppTwoStandalone`, `SuperApp`. |
| App shell | Runtime host app id: `AppOne` or `AppTwo`. |
| Experience | Configured business experience such as `Newport&Buckhead` or `Shop`. |
| BU | Business unit returned by login, such as `SSMG`, `USBL`, or `CABL`. |
| Role | Coarse user grant such as `CUSTOMER_ADMIN` or `DELIVERY_USER`. |
| Permission/capability | Fine-grained commerce permission such as `orders.edit`. |

The main correction in this branch is that an experience is not the same as `AppOne` or `AppTwo`. Experiences are configured records that may launch through a host app shell.

## Product Layer

The product layer answers: what app did the user install?

| Product ID | Display | Supported configured experiences |
|---|---|---|
| `AppOneStandalone` | AppOne | `Shop` |
| `AppTwoStandalone` | AppTwo | `Newport&Buckhead` |
| `SuperApp` | Super App | `Newport&Buckhead`, `Shop` |

Single-app products support one configured experience. SuperApp supports both.

## Experience Config Layer

The experience layer answers: what configured experiences exist, which BUs can use them, and what capability ceiling does each experience support?

| Experience ID | Display | Host app shell | Supported BUs | Supported features | Theme |
|---|---|---|---|---|---|
| `newport-buckhead` | Newport&Buckhead | `AppTwo` | `SSMG`, `CABL` | Orders, Lists, Delivery | SSMG Boutique Theme |
| `shop` | Shop | `AppOne` | `USBL`, `CABL` | Orders, Catalog, Product Details, Delivery | Broadline Theme |

Experience-level capabilities mean:

> This experience knows how to support these capabilities.

They do not mean every user gets those capabilities.

Experience-level supported features are coarser than permissions. For example, `FeatureId.Orders` means the experience supports the Orders feature surface. Permissions such as `orders.view` and `orders.edit` decide what the user can see or do inside that feature.

## Business Unit Config Layer

The BU layer answers: which experiences can this BU access, and what capability ceiling applies to this BU?

| BU | Allowed configured experiences |
|---|---|
| `SSMG` | `Newport&Buckhead` |
| `USBL` | `Shop` |
| `CABL` | `Newport&Buckhead`, `Shop` |

`CABL` is the demo multi-experience BU.

## Login Grant Layer

The current simulation uses `HardcodedLoginConfig.loginGrants(productId, username)`.

| Product | Simulated login behavior |
|---|---|
| `AppOneStandalone` | Always returns one `USBL` grant. |
| `AppTwoStandalone` | Always returns one `SSMG` grant. |
| `SuperApp` + `admin` | Always returns `CABL` to open the switcher. |
| Other `SuperApp` users | Return one random-but-stable grant for `SSMG`, `USBL`, or `CABL`. |

Each grant includes:

- BU
- user type
- roles
- explicit permissions
- domain capabilities such as delivery mode and allowed delivery operations

## Experience Eligibility

The resolver keeps an experience only if all three statements are true:

```text
experience is supported by ProductDefinition
AND experience is listed in BusinessUnitDefinition.allowedExperiences
AND BU is listed in ExperienceDefinition.supportedBusinessUnits
```

Examples:

```text
AppOneStandalone + USBL
  product supports: Shop
  USBL allows: Shop
  Shop supports: USBL
  result: Shop
```

```text
AppTwoStandalone + SSMG
  product supports: Newport&Buckhead
  SSMG allows: Newport&Buckhead
  Newport&Buckhead supports: SSMG
  result: Newport&Buckhead
```

```text
SuperApp + CABL
  product supports: Newport&Buckhead, Shop
  CABL allows: Newport&Buckhead, Shop
  both experiences support CABL
  result: switcher with Newport&Buckhead and Shop
```

## Permission Resolution

Permission templates answer: what capabilities does a role usually grant?

Current examples:

```text
CUSTOMER
  orders.view
  lists.view
  catalog.view
  pdp.view
  delivery.view
  delivery.status

CUSTOMER_ADMIN
  orders.view
  orders.edit
  orders.notifications
  lists.view
  lists.edit
  lists.purchaseHistory
  catalog.view
  catalog.recommendations
  pdp.view
  pdp.internalDetails
  delivery.view
  delivery.edit
  delivery.progress
  delivery.status
  delivery.map
  delivery.invoices
```

The final resolved commerce capability set is:

```text
resolved capabilities =
  (role template capabilities + explicit login permissions)
  intersect BU allowed capabilities
  intersect experience supported capabilities
```

This clipping is important. A role may grant a permission, but the final context only gets it if both the BU and the experience allow it.

The UI should check permissions, not role names:

```kotlin
context.commerceCapabilities.has(PermissionId.OrdersEdit)
```

not:

```kotlin
RoleId.CustomerAdmin in context.roles
```

## AppContext

Once an experience is eligible and capabilities are resolved, `ProductRuntime` creates an `AppContext`.

The context contains:

- host `appId`
- `businessUnitId`
- user id
- user type
- roles
- explicit permissions
- final resolved `commerceCapabilities`
- domain capabilities such as delivery behavior

This `AppContext` is the source of truth for feature rendering and policies.

## Feature Resolution

`FeatureRegistry` defines the feature surface as data:

- feature id
- display title
- required permission
- optional tweak permissions
- availability predicate

A feature is visible only when its availability predicate passes against `AppContext`.

Optional tweaks are rendered when the matching tweak permission exists in `context.commerceCapabilities`.

The first filter is the resolved coarse feature set:

```kotlin
features.filter { it.id in context.supportedFeatures && it.isAvailable(context) }
```

That means a user can have `catalog.view`, but Catalog still stays hidden when the selected experience does not support the Catalog feature.

## Product Feature Bundling

Product build metadata is centralized in:

```text
config/product-feature-bundles.json
```

Android flavors read this config in `androidApp/build.gradle.kts`. iOS constants and native feature registries are generated from the same config into:

```text
iosApp/SharedIOS/Core/ProductFeatureBundles.generated.swift
```

Regenerate the Swift constants and registries with:

```bash
./gradlew generateIOSProductFeatureBundles
```

`ProductRuntime.bundledFeatures` calculates the union of coarse features for every configured experience a product can launch.

| Product | Feature union |
|---|---|
| `AppOneStandalone` | Orders, Catalog, Product Details, Delivery |
| `AppTwoStandalone` | Orders, Lists, Delivery |
| `SuperApp` | Orders, Lists, Catalog, Product Details, Delivery |

Android and iOS now use this as true physical bundling:

- `shared:application` no longer imports Orders, Lists, Catalog, or Product Details.
- Each Android flavor has a `ProductFeatureBundle` source file that imports only the feature modules compiled into that flavor.
- `androidApp/build.gradle.kts` attaches feature module dependencies to `appOneImplementation`, `appTwoImplementation`, or `superAppImplementation`.
- The iOS KMP framework build accepts `-PiosProductBundle=appOne|appTwo|superApp`.
- `shared/application/build.gradle.kts` uses that property to attach only the selected iOS feature module dependencies and generate the matching `platformFeatureDefinitions()` implementation.
- Each Xcode target passes its product bundle into `:shared:application:embedAndSignAppleFrameworkForXcode`, so `SharedLogic.framework` is compiled differently for AppOne, AppTwo, and SuperApp.
- Runtime still filters by the selected experience and the resolved permission set.

The login screens display the build feature union from platform build metadata so the package contents are visible while testing.

The coarse feature ids also map to concrete KMP modules in `config/product-feature-bundles.json`:

| Feature id | Module |
|---|---|
| `orders` | `:shared:features:orders` |
| `lists` | `:shared:features:lists` |
| `catalog` | `:shared:features:catalog` |
| `product-details` | `:shared:features:productdetails` |
| `delivery` | `:shared:features:delivery` |

## Action Resolution

Non-delivery feature detail screens use structured action tables from their feature modules. Each action declares:

- label
- required permission
- simulated result text

The UI filters allowed actions from the resolved capability set.

Delivery actions are policy-driven because order status changes affect the next available actions. The policy/repository code now lives behind `DeliveryFeature.runtimeContributor`, so `shared:application` does not import delivery domain classes.

`DeliveryPolicyResolver` reads `context.capabilities.delivery` and returns a policy:

- customer
- driver
- admin
- merchant
- read-only
- disabled

The selected policy calculates actions per order state and exposes them as generic `FeatureRuntimeSnapshot` data for Android, iOS, and snapshot mapping.

## Theme Resolution

Theme is bound to the configured experience:

| Experience | UI theme |
|---|---|
| `Newport&Buckhead` | Boutique |
| `Shop` | Broadline |

The UI displays the resolved theme but does not let users switch it independently.

## Navigation

Android and iOS both support:

- login to switcher when multiple experiences resolve
- switcher back to login
- experience feature list back to switcher or login
- feature detail back to feature list
- logout back to login

## Backend Migration

The backend integration should replace the hardcoded grant source:

```text
HardcodedLoginConfig.loginGrants(productId, username)
```

The real response should return:

- BU
- roles
- explicit permissions
- domain capabilities

The product/BU/experience eligibility logic and capability intersection can stay in shared code.
