# Commerce Permission And Feature Resolution

This document explains how the sample resolves product, experience, business-unit, role, permission, feature, tweak, and business-action decisions.

The core idea is to keep these questions separate:

1. What product binary is installed?
2. Which app experience is active?
3. Who logged in, from which business unit, with which roles and permissions?
4. Which features, tweaks, and business actions should be available?

That separation lets standalone apps and the Super App share the same downstream runtime once an experience is selected.

## 1. Product Layer

The product layer answers:

> What did the user install?

Examples:

- AppOne standalone
- AppTwo standalone
- Super App

This is represented by `ProductId`.

The Android flavor or iOS target injects the product identity:

- `AppOneStandalone`
- `AppTwoStandalone`
- `SuperApp`

`ProductCatalog` maps each product to the app experiences it can host:

```text
AppOneStandalone -> AppOne
AppTwoStandalone -> AppTwo
SuperApp         -> AppOne, AppTwo
```

The Super App is not itself an app experience. It is a gateway product that can route into multiple app experiences.

```text
ProductId = installed shell / binary
AppId     = selected experience inside that shell
```

So a Super App product can launch the AppOne or AppTwo experience, but the Super App itself is not AppOne or AppTwo.

## 2. Experience Config Layer

The experience layer answers:

> What experiences exist, and what are their default commerce capabilities?

The hard-coded config currently defines two experiences:

```text
Newport&Buckhead
  appId = AppOne
  theme = SSMG Boutique Theme
  allowedSites = BHNP
  supportedBusinessUnits = SSMG
  commerceCapabilities =
    orders.view
    orders.edit
    lists.view
    catalog.view
    pdp.view
    delivery.view
    delivery.status

Shop
  appId = AppTwo
  theme = Broadline Theme
  allowedSites = USBL
  supportedBusinessUnits = USBL
  commerceCapabilities =
    orders.view
    orders.notifications
    lists.view
    lists.purchaseHistory
    catalog.view
    catalog.recommendations
    pdp.view
    delivery.view
    delivery.progress
    delivery.map
    delivery.invoices
```

Experience-level capabilities mean:

> This experience knows how to support these capabilities.

They do not mean:

> Every user in this experience can do every one of these things.

The final user capability set is resolved later by merging business-unit capabilities, experience capabilities, role permission templates, and explicit user permissions.

## 3. Business Unit Config Layer

The business-unit layer answers:

> Which experiences can this business unit access?

Current hard-coded BU config:

```text
SSMG
  allowedExperiences = AppOne
  commerceCapabilities =
    orders.view
    catalog.view
    delivery.view

USBL
  allowedExperiences = AppTwo
  commerceCapabilities =
    orders.view
    lists.view
    catalog.view
    delivery.view
```

The BU participates in two ways:

1. It filters available experiences.
2. It contributes baseline commerce capabilities.

The experience availability rule is an intersection:

```text
available experiences =
  product.supportedExperiences
    ∩ businessUnit.allowedExperiences
    ∩ experience.supportedBusinessUnits
```

For the Super App with `SSMG`:

```text
Product supports: AppOne, AppTwo
Selected BU: SSMG
BU allows: AppOne
Experience AppOne supports BU: SSMG

Result: AppOne / Newport&Buckhead
```

For the Super App with `USBL`:

```text
Product supports: AppOne, AppTwo
Selected BU: USBL
BU allows: AppTwo
Experience AppTwo supports BU: USBL

Result: AppTwo / Shop
```

For standalone AppOne:

```text
Product supports: AppOne
BU allows: AppOne

Result: AppOne / Newport&Buckhead
```

The same filtering logic therefore works for both standalone apps and the Super App.

## 4. Permission Template Layer

The permission template layer answers:

> What capabilities does this role usually get?

Current hard-coded templates:

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

The UI should not check raw role names for rendering decisions.

Prefer this:

```kotlin
PermissionId.OrdersEdit in context.commerceCapabilities
```

Instead of this:

```kotlin
RoleId.CustomerAdmin in context.roles
```

Roles are coarse. Permission-string capabilities are what feature rendering should use.

## 5. Login And AppContext Resolution

Login resolves through `AppCatalog`.

The main path is:

```kotlin
fun contextFor(appId: AppId, username: String): AppContext
```

During login, the catalog resolves:

1. app definition
2. username/profile
3. business unit
4. roles
5. explicit permissions
6. final commerce capabilities

Conceptually:

```kotlin
val businessUnitId = businessUnitFor(definition.id)
val roles = rolesFor(profile.userType, normalizedUsername)
val explicitPermissions = explicitPermissionsFor(profile.userType, normalizedUsername)

val commerceCapabilities = commerceCapabilitiesFor(
    appId = definition.id,
    businessUnitId = businessUnitId,
    roles = roles,
    explicitPermissions = explicitPermissions,
)
```

Then the catalog returns an enriched `AppContext`:

```kotlin
AppContext(
    appId = definition.id,
    businessUnitId = businessUnitId,
    userId = "${definition.configKey}-$normalizedUsername",
    userType = profile.userType,
    roles = roles,
    explicitPermissions = explicitPermissions,
    commerceCapabilities = commerceCapabilities,
    capabilities = profile.capabilities,
)
```

After login, `AppContext` is the authenticated source of truth for shared logic and UI rendering.

The UI does not need to know whether `delivery.map` came from:

- experience config
- business-unit config
- role template
- explicit user permission

It only asks:

```kotlin
PermissionId.DeliveryMap in context.commerceCapabilities
```

## 6. Final Commerce Capability Merge

The final permission set is the union of:

```text
business-unit capabilities
+ experience capabilities
+ role template capabilities
+ explicit user permissions
```

In code, this is represented as:

```kotlin
businessUnit.commerceCapabilities
    .plus(experience.commerceCapabilities)
    .plus(permissionTemplateCatalog.capabilitiesFor(roles))
    .plus(CommerceCapabilities(explicitPermissions))
```

### Example: AppOne Customer

Business unit `SSMG` contributes:

```text
orders.view
catalog.view
delivery.view
```

Experience `Newport&Buckhead` contributes:

```text
orders.view
orders.edit
lists.view
catalog.view
pdp.view
delivery.view
delivery.status
```

Role `CUSTOMER` contributes:

```text
orders.view
lists.view
catalog.view
pdp.view
delivery.view
delivery.status
```

Explicit permissions:

```text
none
```

Final resolved set:

```text
orders.view
orders.edit
lists.view
catalog.view
pdp.view
delivery.view
delivery.status
```

Feature result:

```text
Orders
Lists
Catalog
Product Details
Delivery
```

Enabled tweaks:

```text
Orders: orders.edit
Delivery: delivery.status
```

### Example: AppTwo Admin/Merchant-Like User

Business unit `USBL` contributes:

```text
orders.view
lists.view
catalog.view
delivery.view
```

Experience `Shop` contributes:

```text
orders.view
orders.notifications
lists.view
lists.purchaseHistory
catalog.view
catalog.recommendations
pdp.view
delivery.view
delivery.progress
delivery.map
delivery.invoices
```

Role `CUSTOMER_ADMIN` contributes:

```text
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

Final resolved set includes:

```text
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

The user sees the same major feature shells, but with more enabled tweaks inside those features.

## 7. Feature Registry Layer

The feature registry answers:

> Which top-level feature cards should be visible?

Each feature has:

- a stable `FeatureId`
- a title
- one required permission
- optional tweak permissions

Example:

```kotlin
FeatureDescriptor(
    id = FeatureId.Orders,
    title = "Orders",
    requiredPermission = PermissionId.OrdersView,
    tweakPermissions = listOf(
        PermissionId.OrdersEdit,
        PermissionId.OrdersNotifications,
    ),
) { PermissionId.OrdersView in it.commerceCapabilities }
```

The rule is:

```text
requiredPermission = can enter the feature
tweakPermissions   = what the feature can do or render differently
```

Current commerce feature map:

| Feature | Required permission | Tweaks |
|---|---|---|
| Orders | `orders.view` | `orders.edit`, `orders.notifications` |
| Lists | `lists.view` | `lists.edit`, `lists.purchaseHistory` |
| Catalog | `catalog.view` | `catalog.recommendations` |
| Product Details | `pdp.view` | `pdp.internalDetails` |
| Delivery | `delivery.view` | `delivery.edit`, `delivery.progress`, `delivery.status`, `delivery.map`, `delivery.invoices` |

`FeatureRegistry.availableFeatures(context)` filters features by their required permission.

Each available feature computes enabled tweaks dynamically:

```kotlin
fun enabledTweaks(context: AppContext): List<PermissionId> =
    tweakPermissions.filter(context.commerceCapabilities::has)
```

## 8. UI Rendering Layer

Android and iOS consume already-resolved information.

The UI does not calculate:

- business-unit rules
- role templates
- product/experience filtering
- raw auth response mapping

The UI renders:

- current `AppContext`
- available features
- enabled tweaks
- allowed business actions

### Android Compose

The Compose feature list displays:

```text
AppId
Business unit / roles / userId
Feature card
  required permission
  enabled tweaks
```

Feature cards render:

```kotlin
Text("Requires: ${feature.requiredPermission.value}")

val enabledTweaks = feature.enabledTweaks(context)

if (enabledTweaks.isNotEmpty()) {
    Text("Enabled tweaks: ${enabledTweaks.joinToString { it.value }}")
}
```

If a user has:

```text
orders.view
orders.edit
```

The card shows:

```text
Orders
Requires: orders.view
Enabled tweaks: orders.edit
```

If a user only has:

```text
orders.view
```

The user still sees Orders, but edit and notification tweaks are not enabled.

### iOS SwiftUI

Kotlin maps shared feature descriptors into snapshots, then Swift maps those snapshots into native presentation models:

```swift
NativeFeature(
    id: String,
    title: String,
    requiredPermission: String,
    enabledTweaks: [String]
)
```

SwiftUI renders the same feature title, required permission, and enabled tweaks. The decision logic remains shared in Kotlin.

## 9. How Features Should Behave Dynamically Based On Tweaks

A feature should treat permissions in two levels.

### Level 1: Feature Entry

The user can open the feature only if they have the required permission.

Example:

```text
orders.view
```

Without it, Orders should not be shown.

### Level 2: Inside-Feature Behavior

Once inside, each optional interaction or section checks its own permission.

For Orders:

```text
orders.view
  show orders list

orders.edit
  show edit order button

orders.notifications
  show notification preferences, notification badges, or subscribe controls
```

For Lists:

```text
lists.view
  show saved lists

lists.edit
  allow create/edit/delete lists

lists.purchaseHistory
  show reorder from purchase history
```

For Catalog:

```text
catalog.view
  show catalog

catalog.recommendations
  show recommended products carousel
```

For Product Details:

```text
pdp.view
  show product details page

pdp.internalDetails
  show internal SKU, vendor, margin, or operations-only information
```

For Delivery:

```text
delivery.view
  show delivery screen

delivery.edit
  allow editing delivery details

delivery.progress
  show progress timeline

delivery.status
  show status chips

delivery.map
  show map

delivery.invoices
  show invoice/documents section
```

Feature code should follow this shape:

```kotlin
if (context.hasPermission(PermissionId.DeliveryMap)) {
    DeliveryMap()
}

if (context.hasPermission(PermissionId.DeliveryInvoices)) {
    InvoiceSection()
}

if (context.hasPermission(PermissionId.DeliveryEdit)) {
    EditDeliveryButton()
}
```

This lets one feature shell reshape itself per user, business unit, role, permission set, and experience.

## 10. Business Actions Versus Feature Tweaks

Feature tweaks decide which UI sections or controls may be shown.

Business actions decide what the user may actually do for a specific domain object and state.

For example, a user may have:

```text
delivery.edit
```

But whether they can edit a specific delivery still depends on order state:

```text
Created order     -> edit address may be allowed
Out for delivery  -> edit address may be blocked
Delivered         -> edit address may be blocked
```

Permissions are necessary, but not always sufficient.

The delivery sample models business behavior through policies:

```text
DeliveryPolicyResolver
DeliveryPolicy
CustomerDeliveryPolicy
DriverDeliveryPolicy
AdminDeliveryPolicy
MerchantDeliveryPolicy
ReadOnlyDeliveryPolicy
```

These answer:

```kotlin
availableActions(order)
canOpenDeliveryDetails(order)
```

The full decision chain is:

```text
Can user see Delivery feature?
  requires delivery.view

Can user see Edit Delivery UI?
  requires delivery.edit

Can user edit this specific order right now?
  requires delivery.edit
  and delivery policy says action is allowed for this order state
```

This creates layered enforcement:

```text
Feature Registry:
  can enter feature?

Feature UI tweaks:
  can see or use an optional UI capability?

Business policy:
  can perform this action for this specific domain object?
```

## 11. How Allowed Business Actions Reflect In UI

For Delivery, the UI should compute:

```kotlin
val policy = session.deliveryPolicy()
val actions = policy.availableActions(order)
```

Then render only the actions returned by shared business logic.

### Customer Example

```text
Order state: Created
User: Customer

Capabilities:
  delivery.view
  delivery.status

Policy:
  CustomerDeliveryPolicy

Allowed actions:
  Cancel
  EditAddress
  Track
```

UI result:

```text
Delivery screen visible
Status section visible
Map hidden if delivery.map is missing
Invoice section hidden if delivery.invoices is missing

Actions:
  Cancel
  Edit Address
  Track
```

### Driver Example

```text
Order state: Created
User: Driver

Capabilities:
  delivery.view
  delivery.progress
  delivery.status
  delivery.map

Policy:
  DriverDeliveryPolicy

Allowed actions:
  Accept
```

UI result:

```text
Delivery screen visible
Progress timeline visible
Status visible
Map visible
Invoices hidden

Actions:
  Accept
```

### Customer Admin Example

```text
Order state: Delivered
User: Customer Admin

Capabilities:
  delivery.view
  delivery.edit
  delivery.progress
  delivery.status
  delivery.map
  delivery.invoices

Policy:
  AdminDeliveryPolicy
```

UI result:

```text
Delivery screen visible
Progress visible
Status visible
Map visible
Invoices visible

Edit controls may be generally available, but the order-state policy may still avoid returning EditAddress for a delivered order.
```

## 12. Recommended Pattern For Every Feature

Each feature should follow this pattern:

```text
FeatureDescriptor
  requiredPermission
  tweakPermissions

FeatureScreen
  receives AppContext
  checks tweak permissions for optional sections

FeaturePolicy / UseCase
  checks domain and business state
  returns allowed actions/results

UI
  renders returned sections and actions
```

### Orders

```text
Orders feature visible:
  orders.view

Inside Orders:
  orders.edit -> show edit buttons
  orders.notifications -> show notification controls

Business action:
  edit order allowed only if order status allows edit
```

### Lists

```text
Lists feature visible:
  lists.view

Inside Lists:
  lists.edit -> create/edit/delete
  lists.purchaseHistory -> purchase history shortcuts

Business action:
  reorder allowed only if list has purchasable items, account is active, etc.
```

### Catalog

```text
Catalog feature visible:
  catalog.view

Inside Catalog:
  catalog.recommendations -> recommendations rail

Business action:
  add to cart allowed only if item is purchasable for site/customer/BU
```

### Product Details

```text
Product Details feature visible:
  pdp.view

Inside Product Details:
  pdp.internalDetails -> internal details panel

Business action:
  request substitution, add-to-list, or add-to-cart depends on item/customer/site rules
```

### Delivery

```text
Delivery feature visible:
  delivery.view

Inside Delivery:
  delivery.progress
  delivery.status
  delivery.map
  delivery.invoices
  delivery.edit

Business action:
  actions returned by DeliveryPolicy for current order state
```

## 13. Why This Works For Super App And Standalone Apps

Standalone and Super App use the same downstream flow after an experience is selected.

```text
Standalone:
  ProductId
    -> default AppId
    -> login
    -> AppContext
    -> features
    -> UI

Super App:
  ProductId
    -> BU filter
    -> selected AppId
    -> login
    -> AppContext
    -> features
    -> UI
```

Once an `AppId` is selected, both paths are identical.

That means:

- no duplicate standalone logic
- no special Super App-only feature code
- one shared `FeatureRegistry`
- one shared config resolution path
- one shared iOS snapshot path
- one shared Android Compose rendering path

The Super App adds only the gateway step.

Everything else is the normal app runtime.

## 14. How This Should Evolve Later

Right now the config is hardcoded:

```kotlin
defaultExperienceDefinitions()
defaultBusinessUnitDefinitions()
defaultPermissionTemplates()
```

Later, these can move to remote config or backend payloads:

```text
Experience_Config from backend
Business_Unit_Config from backend
Permission_Template from backend
User roles/permissions from auth response
```

The rest of the app should stay mostly the same:

```text
Resolve config
  -> produce AppContext
  -> FeatureRegistry reads commerceCapabilities
  -> UI renders features and tweaks
  -> domain policies return allowed business actions
```

The input source changes, but the app behavior model remains stable.
