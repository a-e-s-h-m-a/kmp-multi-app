# Hardcoded Experience Resolution Flow

This document describes the current simulation for resolving configured experiences after login. In this model, `AppOne`, `AppTwo`, and `SuperApp` are product/app shells. An experience is a separate configured runtime profile such as `Newport&Buckhead` or `Shop`.

## Core Idea

The app does not pick a feature set only from the app shell. After login, the simulated auth response returns a business unit plus roles, explicit permissions, and domain capabilities. The product runtime then resolves the configured experience for that BU.

Most BUs map to one experience. `CABL` is the demo exception that maps to multiple experiences so the post-login experience switcher can be exercised.

## Current Product Mapping

| Product | Product id | Supported configured experiences | Login simulation |
|---|---|---|---|
| AppOne | `AppOneStandalone` | `Shop` | Returns a `USBL` grant. |
| AppTwo | `AppTwoStandalone` | `Newport&Buckhead` | Returns an `SSMG` grant. |
| Super App | `SuperApp` | `Newport&Buckhead`, `Shop` | `admin` returns `CABL`; other users return one randomized grant for `SSMG`, `USBL`, or `CABL`. |

Single-app products still use the same resolver, but because they support only one configured experience, they can only launch one experience after login.

## Current Experience Config

| Experience id | Display name | Host app shell | Supported BUs | Supported features | Sites | Theme |
|---|---|---|---|---|---|---|
| `newport-buckhead` | Newport&Buckhead | `AppTwo` | `SSMG`, `CABL` | Orders, Lists, Delivery | `BHNP` | SSMG Boutique Theme |
| `shop` | Shop | `AppOne` | `USBL`, `CABL` | Orders, Catalog, Product Details, Delivery | `USBL`, `CABL` | Broadline Theme |

The host app shell is only the app runtime used to launch the shared session. It is not the experience identity.

## Current Business Unit Config

| BU | Allowed configured experiences | Notes |
|---|---|---|
| `SSMG` | `Newport&Buckhead` | Normal one-BU-to-one-experience path. |
| `USBL` | `Shop` | Normal one-BU-to-one-experience path. |
| `CABL` | `Newport&Buckhead`, `Shop` | Demo multi-experience path that triggers the switcher in SuperApp. |

## Current Hardcoded Login Grants

| Grant | Label | BU | User type | Roles |
|---|---|---|---|---|
| `boutique-admin` | Boutique Admin | `SSMG` | Admin | `CUSTOMER_ADMIN`, `INTERNAL_USER` |
| `boutique-customer` | Boutique Buyer | `SSMG` | Customer | `CUSTOMER`, `DEMO_CUSTOMER` |
| `broadline-operator` | Broadline Operator | `USBL` | Merchant | `CUSTOMER_ADMIN`, `CSR` |
| `broadline-driver` | Broadline Driver | `USBL` | Driver | `DELIVERY_USER` |
| `canada-admin` | Canada Admin | `CABL` | Admin | `CUSTOMER_ADMIN`, `CSR_OPCO` |

`HardcodedLoginConfig.loginGrants(productId, username)` is the temporary stand-in for the future network response.

Current behavior:

- `AppOneStandalone` returns one `USBL` grant.
- `AppTwoStandalone` returns one `SSMG` grant.
- `SuperApp` returns `CABL` for `admin`, which reliably opens the experience switcher.
- Other `SuperApp` users return one random-but-stable grant for the username from `SSMG`, `USBL`, or `CABL`.

## Resolution Algorithm

Resolution happens in `ProductRuntime.resolvedExperienceOptions(username)`.

For each login grant:

1. Read the grant BU, roles, explicit permissions, and domain capabilities.
2. Read the product-supported configured experiences.
3. Read the BU config for the returned BU.
4. Keep only experiences where:

```text
experience is supported by the product
AND experience is allowed by the BU
AND BU is supported by the experience
```

5. Resolve commerce capabilities with intersection:

```text
resolved capabilities =
  (role template capabilities + explicit login permissions)
  intersect BU capability ceiling
  intersect experience capability ceiling
```

6. Build a `ResolvedExperienceOption` containing:
   - original login grant
   - configured experience definition
   - fully resolved `AppContext`
   - selected experience coarse feature set

7. The UI receives the resolved options:
   - zero options: show no eligible experience message
   - one option: launch directly
   - multiple options: show experience switcher

## Theme Resolution

Theme is tied to the configured experience and cannot be freely changed.

| Experience | UI theme |
|---|---|
| Newport&Buckhead | Boutique |
| Shop | Broadline |
| fallback | Operations |

Android maps this from `ResolvedExperienceOption.experience.id`. iOS maps it from the resolved experience metadata.

## Feature And Action Resolution

Feature visibility is table-driven through `FeatureRegistry`.

Each feature declares:

- stable feature id
- display title
- required permission
- optional tweak permissions
- availability predicate

After the final `AppContext` is created, `session.availableFeatures()` filters features from the resolved permissions.

Feature visibility first checks the selected experience’s coarse supported feature set, then checks permissions. This lets an experience support `Orders` as a feature while permissions such as `orders.view` and `orders.edit` decide the visible pieces/actions inside that feature.

`ProductRuntime.bundledFeatures` calculates the union of supported features for every experience a product can launch:

| Product | Feature union |
|---|---|
| `AppOneStandalone` | Orders, Catalog, Product Details, Delivery |
| `AppTwoStandalone` | Orders, Lists, Delivery |
| `SuperApp` | Orders, Lists, Catalog, Product Details, Delivery |

That union is the set a real product-specific build could physically include.

Each coarse feature id maps to a KMP feature module in `config/product-feature-bundles.json`. In the simulation the app links all dummy feature modules, but the definitions are owned by the feature modules:

| Feature id | Module |
|---|---|
| `orders` | `:shared:features:orders` |
| `lists` | `:shared:features:lists` |
| `catalog` | `:shared:features:catalog` |
| `product-details` | `:shared:features:productdetails` |
| `delivery` | `:shared:features:delivery` |

Non-delivery feature detail UI is structured from each module's `FeatureDefinitionSpec`. Each action declares:

- label
- required permission
- simulated result text

The UI filters those actions by `context.commerceCapabilities`.

Delivery remains stateful because order status changes affect future actions. Allowed delivery actions come from `DeliveryPolicyResolver`, which reads the resolved domain `DeliveryCapability` from the context.

## Android Flow

1. `ProductApp(productId)` creates a `ProductRuntime`.
2. User logs in.
3. Product runtime asks `HardcodedLoginConfig` for the simulated login grant.
4. Product runtime resolves configured experiences for the returned BU.
5. If there is one option, Android launches it immediately.
6. If there are multiple options, Android shows the switcher.
7. Launch calls `runtime.session.start(option.context, selectedUser)`.
8. Feature list and feature actions are rendered from the resolved context.
9. The experience determines the theme.

## iOS Flow

All iOS targets now use `MultiAppStoreFactory.makeProduct(...)`.

Entry points:

- AppOne: `makeProduct(productIdName: "AppOneStandalone")`
- AppTwo: `makeProduct(productIdName: "AppTwoStandalone")`
- SuperApp: `makeProduct(productIdName: "SuperApp")`

Flow:

1. Swift calls `client.resolveExperienceOptions(username)`.
2. `IOSProductCompositionRoot` calls shared `ProductRuntime.resolvedExperienceOptions(username)`.
3. If one option exists, TCA launches it immediately.
4. If multiple options exist, TCA routes to `.experienceSwitcher`.
5. Selecting an option calls `client.launchExperience(option.id, username)`.
6. The KMP bridge starts the shared session from the resolved context.
7. SwiftUI renders the returned feature snapshot.
8. Delivery action taps mutate local Swift state to simulate command results.

## Future Network Seam

The future network integration should replace this call:

```text
HardcodedLoginConfig.loginGrants(productId, username)
```

with a real auth/session response that returns:

- BU
- roles
- explicit permissions
- domain capabilities

The rest of the resolver can remain the same: product support, BU eligibility, experience eligibility, permission intersection, `AppContext` creation, and launch.

## Verification

Current verification command:

```bash
./gradlew shared:application:testAndroidHostTest shared:core:config:testAndroidHostTest shared:ui:compileAndroidMain
```

This passes in the current environment.

iOS Xcode build verification still cannot run here because `xcrun xcodebuild -version` reports that `xcodebuild` is unavailable.
