package com.aeshma.multiapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.aeshma.multiapp.application.AppSession
import com.aeshma.multiapp.application.AppRuntime
import com.aeshma.multiapp.application.FeatureDescriptor
import com.aeshma.multiapp.application.ProductRuntime
import com.aeshma.multiapp.application.createAppRuntime
import com.aeshma.multiapp.application.createProductRuntime
import com.aeshma.multiapp.core.model.AppContext
import com.aeshma.multiapp.core.model.AppId
import com.aeshma.multiapp.core.model.FeatureId
import com.aeshma.multiapp.core.model.ProductId
import com.aeshma.multiapp.feature.delivery.DeliveryAction
import com.aeshma.multiapp.feature.delivery.DeliveryOrder
import kotlinx.coroutines.launch

private sealed interface Screen {
    data object Login : Screen
    data object Features : Screen
    data class Feature(val id: FeatureId) : Screen
}

@Composable
@Preview
@OptIn(ExperimentalLayoutApi::class)
fun App(appId: AppId = AppId.AppOne) {
    val runtime = remember(appId) { createAppRuntime(appId) }
    AppThemeContainer {
        ExperienceApp(runtime = runtime, onExitExperience = null)
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun ProductApp(productId: ProductId = ProductId.AppOneStandalone) {
    val productRuntime = remember(productId) { createProductRuntime(productId) }
    var selectedExperience by remember(productRuntime) {
        mutableStateOf(productRuntime.defaultExperience)
    }

    AppThemeContainer {
        val experience = selectedExperience
        if (experience == null) {
            ExperiencePickerScreen(
                productRuntime = productRuntime,
                onExperienceSelected = { selectedExperience = it },
            )
        } else {
            val runtime = remember(productRuntime, experience) {
                productRuntime.appRuntimeFor(experience)
            }
            ExperienceApp(
                runtime = runtime,
                onExitExperience = if (productRuntime.productDefinition.showsExperiencePicker) {
                    {
                        runtime.session.logout()
                        selectedExperience = null
                    }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun AppThemeContainer(content: @Composable () -> Unit) {
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
                content()
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ExperiencePickerScreen(
    productRuntime: ProductRuntime,
    onExperienceSelected: (AppId) -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Text(
                productRuntime.productDefinition.displayName,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Choose which app experience to launch.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(productRuntime.supportedExperiences, key = { it.id.externalName }) { experience ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExperienceSelected(experience.id) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        experience.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Default user: ${experience.defaultUsername}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ExperienceApp(
    runtime: AppRuntime,
    onExitExperience: (() -> Unit)?,
) {
    val session = runtime.session
    var selectedUser by remember(runtime) { mutableStateOf(runtime.appDefinition.defaultUsername) }
    var context by remember(runtime) { mutableStateOf<AppContext?>(null) }
    var features by remember(runtime) { mutableStateOf<List<FeatureDescriptor>>(emptyList()) }
    var screen by remember(runtime) { mutableStateOf<Screen>(Screen.Login) }
    val coroutineScope = rememberCoroutineScope()

    when (val currentScreen = screen) {
        Screen.Login -> LoginScreen(
            appName = runtime.appDefinition.displayName,
            users = runtime.appDefinition.supportedUsernames,
            selectedUser = selectedUser,
            onUserSelected = { selectedUser = it },
            onLogin = {
                coroutineScope.launch {
                    context = session.login(runtime.appId, selectedUser)
                    features = session.availableFeatures()
                    screen = Screen.Features
                }
            },
            onExitExperience = onExitExperience,
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

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun LoginScreen(
    appName: String,
    users: List<String>,
    selectedUser: String,
    onUserSelected: (String) -> Unit,
    onLogin: () -> Unit,
    onExitExperience: (() -> Unit)?,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Text(appName, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(
                "Native Android shell with shared KMP policies",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                users.forEach { username ->
                    FilterChip(
                        selected = selectedUser == username,
                        onClick = { onUserSelected(username) },
                        label = { Text(username) },
                    )
                }
            }
        }
        item { Button(onClick = onLogin) { Text("Login") } }
        if (onExitExperience != null) {
            item { OutlinedButton(onClick = onExitExperience) { Text("Switch experience") } }
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
                        context.appId.externalName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "${context.userType.name} / ${context.userId}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(onClick = onLogout) { Text("Logout") }
            }
        }
        items(features, key = { it.id.value }) { feature ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onFeatureTapped(feature) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Text(
                    feature.title,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
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
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedButton(onClick = onBack) { Text("Back") }
        when (featureId) {
            FeatureId.Home -> SimpleFeatureView("Home", "Welcome ${context.userId}.")
            FeatureId.Profile -> SimpleFeatureView("Profile", "User type: ${context.userType.name}")
            FeatureId.Reports -> ReportsView(context)
            FeatureId.Payments -> PaymentsView(context)
            FeatureId.Delivery -> DeliveryView(
                policyName = session.deliveryPolicy().experienceName,
                orders = session.deliveryOrders(),
                actionsForOrder = session.deliveryPolicy()::availableActions,
            )
            else -> SimpleFeatureView("Unavailable", "Unknown feature: ${featureId.value}")
        }
    }
}

@Composable
private fun SimpleFeatureView(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(body, style = MaterialTheme.typography.bodyLarge)
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
            Text(policyName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        items(orders, key = DeliveryOrder::id) { order ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(order.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${order.id} / ${order.status.name}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        actionsForOrder(order).forEach { action ->
                            FilterChip(selected = false, onClick = {}, label = { Text(action.name) })
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
    Text("$label: ${if (enabled) "enabled" else "disabled"}")
}
