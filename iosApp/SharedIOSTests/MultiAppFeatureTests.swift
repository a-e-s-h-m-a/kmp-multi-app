import ComposableArchitecture
@testable import SharedIOSCore
import XCTest

@MainActor
final class MultiAppFeatureTests: XCTestCase {
    func testSuccessfulLoginShowsSharedFeatures() async {
        let snapshot = NativeSessionSnapshot.customerFixture
        let store = TestStore(initialState: initialState()) {
            MultiAppFeature()
        } withDependencies: {
            $0.multiAppClient = testClient(login: { _ in snapshot })
        }

        await store.send(.loginTapped) {
            $0.isLoading = true
        }
        await store.receive(.loginSucceeded(snapshot)) {
            $0.isLoading = false
            $0.loggedInUserSummary = snapshot.userSummary
            $0.availableFeatures = snapshot.availableFeatures
            $0.deliveryExperienceName = snapshot.deliveryExperienceName
            $0.deliveryOrders = snapshot.deliveryOrders
            $0.selectedScreen = .features
        }
    }

    func testFailedLoginShowsError() async {
        let store = TestStore(initialState: initialState()) {
            MultiAppFeature()
        } withDependencies: {
            $0.multiAppClient = testClient(login: { _ in throw LoginFailure() })
        }

        await store.send(.loginTapped) {
            $0.isLoading = true
        }
        await store.receive(.loginFailed("Login failed")) {
            $0.isLoading = false
            $0.loginError = "Login failed"
        }
    }

    func testLogoutClearsNativeAndSharedSession() async {
        let recorder = LogoutRecorder()
        var state = initialState()
        state.loggedInUserSummary = "Customer"
        state.availableFeatures = NativeSessionSnapshot.customerFixture.availableFeatures
        state.deliveryExperienceName = "Customer Delivery"
        state.deliveryOrders = NativeSessionSnapshot.customerFixture.deliveryOrders
        state.selectedScreen = .features

        let store = TestStore(initialState: state) {
            MultiAppFeature()
        } withDependencies: {
            $0.multiAppClient = testClient(logout: { await recorder.record() })
        }

        await store.send(.logoutTapped) {
            $0.loggedInUserSummary = nil
            $0.availableFeatures = []
            $0.deliveryExperienceName = "Delivery Disabled"
            $0.deliveryOrders = []
            $0.selectedScreen = .login
        }
        await store.finish()

        let logoutCount = await recorder.count
        XCTAssertEqual(logoutCount, 1)
    }

    func testLogoutCancelsInFlightLogin() async {
        let gate = LoginGate()
        let store = TestStore(initialState: initialState()) {
            MultiAppFeature()
        } withDependencies: {
            $0.multiAppClient = testClient(login: { _ in try await gate.wait() })
        }

        await store.send(.loginTapped) {
            $0.isLoading = true
        }
        await gate.waitUntilStarted()
        await store.send(.logoutTapped) {
            $0.isLoading = false
        }
        await store.finish()
    }

    func testFeatureNavigation() async {
        var state = initialState()
        state.availableFeatures = NativeSessionSnapshot.customerFixture.availableFeatures
        state.selectedScreen = .features
        let store = TestStore(initialState: state) {
            MultiAppFeature()
        }

        await store.send(.featureTapped("delivery")) {
            $0.selectedScreen = .feature("delivery")
        }
        await store.send(.backTapped) {
            $0.selectedScreen = .features
        }
    }

    private func initialState() -> MultiAppFeature.State {
        MultiAppFeature.State(
            appName: "AppOne",
            supportedUsernames: ["customer"],
            selectedUsername: "customer"
        )
    }

    private func testClient(
        login: @escaping @Sendable (String) async throws -> NativeSessionSnapshot = { _ in
            .customerFixture
        },
        logout: @escaping @Sendable () async -> Void = {}
    ) -> MultiAppClient {
        MultiAppClient(
            appName: "AppOne",
            defaultUsername: "customer",
            supportedUsernames: ["customer"],
            login: login,
            logout: logout
        )
    }
}

private struct LoginFailure: LocalizedError {
    var errorDescription: String? { "Login failed" }
}

private actor LogoutRecorder {
    private(set) var count = 0

    func record() {
        count += 1
    }
}

private actor LoginGate {
    private var started = false

    func wait() async throws -> NativeSessionSnapshot {
        started = true
        try await Task.sleep(nanoseconds: 60_000_000_000)
        return .customerFixture
    }

    func waitUntilStarted() async {
        while !started {
            await Task.yield()
        }
    }
}

private extension NativeSessionSnapshot {
    static let customerFixture = Self(
        userSummary: "Customer",
        availableFeatures: [
            NativeFeature(id: "home", title: "Home"),
            NativeFeature(id: "delivery", title: "Delivery"),
        ],
        deliveryExperienceName: "Customer Delivery",
        deliveryOrders: [
            NativeDeliveryOrder(
                id: "order-1",
                title: "Order 1",
                status: "Ready",
                actions: ["Track"]
            ),
        ]
    )
}
