# Kotlin Multiplatform Multi-App Architecture Sample

This project demonstrates a realistic Kotlin Multiplatform architecture for supporting multiple separate apps from one codebase while keeping the UI fully native.

It is not a single super app. It models two separate app identities:

- AppOne: customer/driver delivery app
- AppTwo: admin/merchant operations app

Shared KMP logic owns identity, session state, fake login/config, network and analytics abstractions, typed capabilities, feature availability, delivery policy resolution, and common tests. Native platforms own screens, navigation, platform composition roots, target/flavor branding, and platform-specific dependency wiring.

## Architecture Flow

```text
App Target / Product Flavor
    -> AppId
    -> Login
    -> Remote Config / Permission JSON
    -> Typed AppContext
    -> FeatureRegistry
    -> Policy / Strategy Resolver
    -> Native UI Navigation
    -> Feature Screens
```

The main rule is that UI should not know why something is available. UI renders available features and available actions.

## Project Structure

```text
sharedLogic/
  src/commonMain/kotlin/com/example/multiapp/domain/
  src/commonTest/kotlin/com/example/multiapp/domain/

sharedUI/
  Native Android Compose sample UI that consumes shared logic.

androidApp/
  One Android app module with appOne and appTwo product flavors.

iosApp/
  Existing simple Xcode target plus source samples for AppOne/AppTwo SwiftUI target wiring.
```

## AppOne

AppOne is a customer/driver delivery app.

Bundled features:

- Home
- Login
- Profile
- Delivery
- Payments

It does not bundle Reports, Admin Dashboard, or Fleet Management.

Supported sample users:

- `customer`: Home, Delivery, Payments, Profile. Uses `CustomerDeliveryPolicy`.
- `driver`: Home, Delivery, Profile. Uses `DriverDeliveryPolicy`.
- `readonly`: Home, Delivery, Profile. Uses `ReadOnlyDeliveryPolicy`.

Admin and merchant usernames in AppOne intentionally resolve to limited/no admin features for the first implementation.

## AppTwo

AppTwo is an admin/merchant operations app.

Bundled features:

- Home
- Login
- Profile
- Delivery
- Reports

It does not bundle Payments or Customer Checkout.

Supported sample users:

- `admin`: Home, Delivery, Reports, Profile. Uses `AdminDeliveryPolicy`.
- `merchant`: Home, Delivery, Reports, Profile. Uses `MerchantDeliveryPolicy`.
- `readonly`: Home, Delivery, Profile. Uses `ReadOnlyDeliveryPolicy`.

Customer and driver usernames in AppTwo intentionally resolve to limited/no customer delivery or payment features for the first implementation.

## Feature Registry

`FeatureRegistry` receives typed `AppContext` and returns only available `FeatureDescriptor` values.

Feature availability rules live in shared KMP:

- Home and Profile are available after login.
- Delivery requires `DeliveryCapability`.
- Reports requires `ReportsCapability.canViewReports`.
- Payments requires `PaymentsCapability.canMakePayment`.

Native UI renders the returned descriptors. It does not check `AppId` directly to decide what features exist.

## Delivery Policy Resolver

`DeliveryPolicyResolver` centralizes app/user/config variation:

- AppOne + Customer mode -> `CustomerDeliveryPolicy`
- AppOne + Driver mode -> `DriverDeliveryPolicy`
- AppTwo + Admin mode -> `AdminDeliveryPolicy`
- AppTwo + Merchant mode -> `MerchantDeliveryPolicy`
- ReadOnly mode -> `ReadOnlyDeliveryPolicy`
- Missing or invalid delivery capability -> `DisabledDeliveryPolicy`

Resolvers and composition roots are allowed to contain centralized branching. Screens should only render state and actions returned by policies.

## Rules

- UI should not check AppId directly.
- UI should not parse raw permission/config JSON.
- Feature availability comes from FeatureRegistry.
- Feature behavior comes from Policy/Strategy.
- App-specific wiring belongs in composition root.
- Server config should describe capabilities, not arbitrary UI.
- Resolvers may contain centralized branching.
- Screens should render state/actions only.

## Example Users

- AppOne + `customer`: customer delivery, payments enabled.
- AppOne + `driver`: driver workflow, payments and reports disabled.
- AppOne + `readonly`: delivery view-only.
- AppTwo + `admin`: admin delivery plus global and merchant reports.
- AppTwo + `merchant`: merchant delivery plus merchant reports.
- AppTwo + `readonly`: delivery view-only, reports disabled.
- Any app + `nod`: only Home and Profile, delivery policy is disabled.

## Android

Android uses one `androidApp` module with two product flavors:

- `appOne`: application id `com.example.appone`, injects `AppId.AppOne`
- `appTwo`: application id `com.example.apptwo`, injects `AppId.AppTwo`

The Compose UI in `sharedUI` has login, dynamic feature list, and native feature screens. Delivery actions come from `DeliveryPolicy`; feature navigation comes from `FeatureRegistry`.

Useful commands:

```bash
./gradlew :androidApp:assembleAppOneDebug
./gradlew :androidApp:assembleAppTwoDebug
./gradlew :sharedLogic:testAndroidHostTest
```

## iOS

The Xcode project contains three schemes:

- `iosApp`: the original starter target
- `AppOne`: app target with bundle id `com.example.appone`
- `AppTwo`: app target with bundle id `com.example.apptwo`

The multi-app SwiftUI source is split across:

- `iosApp/SharedIOS/MultiAppSwiftUI.swift`
- `iosApp/AppOne/AppOneApp.swift`
- `iosApp/AppTwo/AppTwoApp.swift`

Open `iosApp/iosApp.xcodeproj`, pick `AppOne` or `AppTwo` from the Xcode scheme dropdown, choose a simulator, and run.

Each target injects a different app identity. The SwiftUI sample is TCA-friendly: it uses state, action, environment/dependencies, and a reducer-like store. Full TCA and SKIE can be added later for better Swift/KMP ergonomics.

## How To Add A New Feature

1. Add a new `FeatureId`.
2. Add a capability model if the feature needs permissions.
3. Create a `FeatureDescriptor`.
4. Register it in `FeatureRegistry.default()`.
5. Create native Android and iOS screens.
6. Add navigation mapping for the new `FeatureId`.
7. Add common tests for availability and behavior.

## How To Add A New App

1. Add a new `AppId`.
2. Add an Android flavor or app module.
3. Add an iOS target.
4. Provide app-specific composition root injection.
5. Define bundled features through typed capabilities.
6. Add fake config/login rules.
7. Add resolver and feature tests.

## How To Add A New Delivery Experience

1. Add a `DeliveryMode` if needed.
2. Create a new `DeliveryPolicy`.
3. Update `DeliveryPolicyResolver`.
4. Add tests for resolver selection and available actions.
5. Keep UI unchanged unless a brand-new action needs different rendering.

## Next Steps

- Replace fake auth with a real API.
- Replace fake config with real remote config.
- Persist session.
- Add real networking with Ktor.
- Add real DI such as Koin or manual DI modules.
- Add SKIE for better Swift interop if needed.
- Split features into separate Gradle modules.
- Add full TCA on iOS.
- Add deep links per feature.
- Add analytics backend.
- Add feature flags and experiments.
