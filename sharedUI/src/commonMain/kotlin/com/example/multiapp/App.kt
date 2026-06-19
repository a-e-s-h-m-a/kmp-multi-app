package com.example.multiapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.multiapp.domain.AndroidAppCompositionRoot
import com.example.multiapp.domain.AppContext
import com.example.multiapp.domain.AppId
import com.example.multiapp.domain.AppSession
import com.example.multiapp.domain.DeliveryAction
import com.example.multiapp.domain.DeliveryOrder
import com.example.multiapp.domain.FeatureDescriptor
import com.example.multiapp.domain.FeatureId
import kotlinx.coroutines.launch

private val sampleUsers = listOf("customer", "driver", "admin", "merchant", "readonly", "nod")

private sealed interface Screen {
    data object Login : Screen
    data object Features : Screen
    data class Feature(val id: FeatureId) : Screen
}

@Composable
@Preview
@OptIn(ExperimentalLayoutApi::class)
fun App(appId: AppId = AppId.AppOne) {
    val compositionRoot = remember(appId) { AndroidAppCompositionRoot(appId) }
    val session = compositionRoot.session
    var selectedUser by remember { mutableStateOf(defaultUsername(appId)) }
    var context by remember { mutableStateOf<AppContext?>(null) }
    var features by remember { mutableStateOf<List<FeatureDescriptor>>(emptyList()) }
    var screen by remember { mutableStateOf<Screen>(Screen.Login) }
    val coroutineScope = rememberCoroutineScope()

    MaterialTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            Box(
                modifier = Modifier
                    .safeContentPadding()
                    .padding(20.dp),
            ) {
                when (val currentScreen = screen) {
                    Screen.Login -> LoginScreen(
                        appId = appId,
                        selectedUser = selectedUser,
                        onUserSelected = { selectedUser = it },
                        onLogin = {
                            coroutineScope.launch {
                                context = session.login(appId, selectedUser)
                                features = session.availableFeatures()
                                screen = Screen.Features
                            }
                        },
                    )
                    Screen.Features -> FeatureListScreen(
                        context = requireNotNull(context),
                        features = features,
                        onFeatureTapped = { feature ->
                            session.trackFeatureOpened(feature.id)
                            screen = Screen.Feature(feature.id)
                        },
                        onLogout = {
                            context = null
                            features = emptyList()
                            screen = Screen.Login
                        },
                    )
                    is Screen.Feature -> FeatureScreen(
                        featureId = currentScreen.id,
                        session = session,
                        context = requireNotNull(context),
                        onBack = { screen = Screen.Features },
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun LoginScreen(
    appId: AppId,
    selectedUser: String,
    onUserSelected: (String) -> Unit,
    onLogin: () -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Text(
                text = appId.name,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Native Android shell with shared KMP policies",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                sampleUsers.forEach { username ->
                    FilterChip(
                        selected = selectedUser == username,
                        onClick = { onUserSelected(username) },
                        label = { Text(username) },
                    )
                }
            }
        }
        item {
            Button(onClick = onLogin) {
                Text("Login")
            }
        }
    }
}

@Composable
private fun FeatureListScreen(
    context: AppContext,
    features: List<FeatureDescriptor>,
    onFeatureTapped: (FeatureDescriptor) -> Unit,
    onLogout: () -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = context.appId.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${context.userType.name} / ${context.userId}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(onClick = onLogout) {
                    Text("Logout")
                }
            }
        }
        items(features) { feature ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onFeatureTapped(feature) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = feature.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Available through FeatureRegistry",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureScreen(
    featureId: FeatureId,
    session: AppSession,
    context: AppContext,
    onBack: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedButton(onClick = onBack) {
            Text("Back")
        }

        when (featureId) {
            FeatureId.Home -> SimpleFeatureView(
                title = "Home",
                body = "Welcome ${context.userId}. This native screen receives typed session state.",
            )
            FeatureId.Profile -> SimpleFeatureView(
                title = "Profile",
                body = "User type: ${context.userType.name}",
            )
            FeatureId.Reports -> ReportsView(context)
            FeatureId.Payments -> PaymentsView(context)
            FeatureId.Delivery -> DeliveryView(
                policyName = session.deliveryPolicy().experienceName,
                orders = session.sampleDeliveryOrders(),
                actionsForOrder = { order -> session.deliveryPolicy().availableActions(order) },
            )
        }
    }
}

@Composable
private fun SimpleFeatureView(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(text = body, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun DeliveryView(
    policyName: String,
    orders: List<DeliveryOrder>,
    actionsForOrder: (DeliveryOrder) -> List<DeliveryAction>,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Text(
                text = policyName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        items(orders) { order ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = order.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${order.id} / ${order.status.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        actionsForOrder(order).forEach { action ->
                            FilterChip(
                                selected = false,
                                onClick = {},
                                label = { Text(action.name) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportsView(context: AppContext) {
    val reports = requireNotNull(context.capabilities.reports)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Reports", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        CapabilityLine("Delivery volume", reports.canViewReports)
        CapabilityLine("Global reports", reports.canViewGlobalReports)
        CapabilityLine("Merchant reports", reports.canViewMerchantReports)
    }
}

@Composable
private fun PaymentsView(context: AppContext) {
    val payments = requireNotNull(context.capabilities.payments)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Payments", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        CapabilityLine("Make payment", payments.canMakePayment)
        CapabilityLine("Payment history", payments.canViewPaymentHistory)
    }
}

@Composable
private fun CapabilityLine(label: String, enabled: Boolean) {
    Text(
        text = "$label: ${if (enabled) "enabled" else "disabled"}",
        style = MaterialTheme.typography.bodyLarge,
    )
}

private fun defaultUsername(appId: AppId): String =
    when (appId) {
        AppId.AppOne -> "customer"
        AppId.AppTwo -> "admin"
    }
