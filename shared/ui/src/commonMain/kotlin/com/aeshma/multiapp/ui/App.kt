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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aeshma.multiapp.application.AppSession
import com.aeshma.multiapp.application.AppRuntime
import com.aeshma.multiapp.application.FeatureDescriptor
import com.aeshma.multiapp.application.ProductRuntime
import com.aeshma.multiapp.application.ResolvedExperienceOption
import com.aeshma.multiapp.application.createAppRuntime
import com.aeshma.multiapp.application.createProductRuntime
import com.aeshma.multiapp.core.model.AppContext
import com.aeshma.multiapp.core.model.AppId
import com.aeshma.multiapp.core.model.FeatureDefinitionSpec
import com.aeshma.multiapp.core.model.FeatureId
import com.aeshma.multiapp.core.model.FeatureRuntimeContributor
import com.aeshma.multiapp.core.model.FeatureRuntimeItem
import com.aeshma.multiapp.core.model.FeatureRuntimeSnapshot
import com.aeshma.multiapp.core.model.PermissionId
import com.aeshma.multiapp.core.model.ProductId
import kotlinx.coroutines.launch

private sealed interface Screen {
    data object Login : Screen
    data object Features : Screen
    data class Feature(val id: FeatureId) : Screen
}

private sealed interface ProductScreen {
    data object Login : ProductScreen
    data object ExperienceSwitcher : ProductScreen
    data object Experience : ProductScreen
}

private enum class DemoTheme(
    val label: String,
    val primary: Color,
    val secondary: Color,
    val background: Color,
) {
    Boutique("Boutique", Color(0xFF72511E), Color(0xFF006B5A), Color(0xFFFFFBF2)),
    Broadline("Broadline", Color(0xFF145DA0), Color(0xFF7D4E00), Color(0xFFF7FAFF)),
    Operations("Operations", Color(0xFF386A20), Color(0xFF8C1D18), Color(0xFFF8FBF4)),
}

@Composable
@Preview
@OptIn(ExperimentalLayoutApi::class)
fun App(appId: AppId = AppId.AppOne) {
    val runtime = remember(appId) { createAppRuntime(appId) }
    AppThemeContainer(theme = DemoTheme.Boutique) {
        ExperienceApp(
            runtime = runtime,
            experienceTitle = runtime.appDefinition.displayName,
            experienceSubtitle = "Native Android shell with shared KMP policies",
            onExitExperience = null,
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun ProductApp(
    productId: ProductId = ProductId.AppOneStandalone,
    buildFeatureBundle: Set<FeatureId>? = null,
    featureDefinitions: List<FeatureDefinitionSpec> = emptyList(),
    featureRuntimeContributors: List<FeatureRuntimeContributor> = emptyList(),
) {
    val productRuntime = remember(productId, featureDefinitions, featureRuntimeContributors) {
        createProductRuntime(
            productId,
            featureDefinitions = featureDefinitions,
            featureRuntimeContributors = featureRuntimeContributors,
        )
    }
    val packagedFeatures = remember(productRuntime, buildFeatureBundle) {
        buildFeatureBundle ?: productRuntime.bundledFeatures
    }
    var selectedUser by remember(productRuntime) { mutableStateOf(productRuntime.defaultUsername) }
    var productScreen by remember(productRuntime) { mutableStateOf<ProductScreen>(ProductScreen.Login) }
    var resolvedOptions by remember(productRuntime) { mutableStateOf<List<ResolvedExperienceOption>>(emptyList()) }
    var selectedOption by remember(productRuntime) { mutableStateOf<ResolvedExperienceOption?>(null) }
    var selectedTheme by remember(productRuntime) { mutableStateOf(DemoTheme.Operations) }

    fun launch(option: ResolvedExperienceOption) {
        val runtime = productRuntime.appRuntimeFor(option.experience.id)
        runtime.session.start(option.context, selectedUser)
        selectedOption = option
        selectedTheme = option.defaultTheme()
        productScreen = ProductScreen.Experience
    }

    AppThemeContainer(theme = selectedTheme) {
        when (productScreen) {
            ProductScreen.Login -> LoginScreen(
                appName = productRuntime.productDefinition.displayName,
                subtitle = "Build bundle: ${packagedFeatures.joinToString { it.value }}",
                users = productRuntime.supportedUsernames,
                selectedUser = selectedUser,
                onUserSelected = { selectedUser = it },
                onLogin = {
                    resolvedOptions = productRuntime.resolvedExperienceOptions(selectedUser)
                    if (resolvedOptions.size == 1) {
                        launch(resolvedOptions.single())
                    } else {
                        productScreen = ProductScreen.ExperienceSwitcher
                    }
                },
                onExitExperience = null,
            )
            ProductScreen.ExperienceSwitcher -> ExperiencePickerScreen(
                productRuntime = productRuntime,
                options = resolvedOptions,
                onExperienceSelected = ::launch,
                onLogout = {
                    resolvedOptions = emptyList()
                    selectedOption = null
                    productScreen = ProductScreen.Login
                },
            )
            ProductScreen.Experience -> {
                val option = requireNotNull(selectedOption)
                val runtime = remember(productRuntime, option) {
                    productRuntime.appRuntimeFor(option.experience.id)
                }
                ExperienceApp(
                    runtime = runtime,
                    experienceTitle = option.experience.displayName,
                    experienceSubtitle = "${option.grant.label} / ${option.context.businessUnitId.value} / ${option.experience.allowedSites.joinToString()}",
                    onExitExperience = if (resolvedOptions.size > 1) {
                        {
                            runtime.session.logout()
                            selectedOption = null
                            productScreen = ProductScreen.ExperienceSwitcher
                        }
                    } else {
                        null
                    },
                    initialContext = option.context,
                    initialUsername = selectedUser,
                    selectedTheme = selectedTheme,
                    onBackFromFeatures = {
                        runtime.session.logout()
                        selectedOption = null
                        productScreen = if (resolvedOptions.size > 1) {
                            ProductScreen.ExperienceSwitcher
                        } else {
                            resolvedOptions = emptyList()
                            ProductScreen.Login
                        }
                    },
                    onLogout = {
                        runtime.session.logout()
                        resolvedOptions = emptyList()
                        selectedOption = null
                        productScreen = ProductScreen.Login
                    },
                )
            }
        }
    }
}

@Composable
private fun AppThemeContainer(
    theme: DemoTheme,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = theme.primary,
            secondary = theme.secondary,
            background = theme.background,
            surface = Color.White,
            surfaceVariant = theme.background,
        ),
    ) {
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
    options: List<ResolvedExperienceOption>,
    onExperienceSelected: (ResolvedExperienceOption) -> Unit,
    onLogout: () -> Unit,
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
                "Choose from the experiences resolved from the hardcoded login response.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            OutlinedButton(onClick = onLogout) { Text("Back to login") }
        }
        if (options.isEmpty()) {
            item {
                Text(
                    "No experiences are available for the returned BU, roles, and permissions.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(options, key = { "${it.grant.id}-${it.experience.id.value}" }) { option ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExperienceSelected(option) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        option.experience.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "${option.grant.label} / ${option.grant.businessUnitId.value}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Theme: ${option.defaultTheme().label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Roles: ${option.grant.roles.joinToString { it.value }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Resolved permissions: ${option.context.commerceCapabilities.permissions.joinToString { it.value }}",
                        style = MaterialTheme.typography.bodySmall,
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
    experienceTitle: String,
    experienceSubtitle: String,
    onExitExperience: (() -> Unit)?,
    initialContext: AppContext? = null,
    initialUsername: String? = null,
    selectedTheme: DemoTheme = DemoTheme.Boutique,
    onBackFromFeatures: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
) {
    val session = runtime.session
    var selectedUser by remember(runtime, initialUsername) {
        mutableStateOf(initialUsername ?: runtime.appDefinition.defaultUsername)
    }
    var context by remember(runtime, initialContext) { mutableStateOf(initialContext) }
    var features by remember(runtime, initialContext) {
        mutableStateOf(if (initialContext == null) emptyList() else session.availableFeatures())
    }
    var screen by remember(runtime, initialContext) {
        mutableStateOf<Screen>(if (initialContext == null) Screen.Login else Screen.Features)
    }
    val coroutineScope = rememberCoroutineScope()

    when (val currentScreen = screen) {
        Screen.Login -> LoginScreen(
            appName = experienceTitle,
            subtitle = experienceSubtitle,
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
            session = session,
            context = requireNotNull(context),
            features = features,
            selectedTheme = selectedTheme,
            onBack = onBackFromFeatures,
            onFeatureTapped = { feature ->
                session.trackFeatureOpened(feature.id)
                screen = Screen.Feature(feature.id)
            },
            onLogout = {
                if (onLogout != null) {
                    onLogout()
                } else {
                    session.logout()
                    context = null
                    features = emptyList()
                    screen = Screen.Login
                }
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
    subtitle: String,
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
                subtitle,
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
    session: AppSession,
    context: AppContext,
    features: List<FeatureDescriptor>,
    selectedTheme: DemoTheme,
    onBack: (() -> Unit)?,
    onFeatureTapped: (FeatureDescriptor) -> Unit,
    onLogout: () -> Unit,
) {
    var selectedTab by remember(features.map { it.id.value }) { mutableStateOf(0) }
    val tabFeatures = features.take(4)
    val overflowFeatures = features.drop(4)
    val hasMoreTab = overflowFeatures.isNotEmpty()
    val tabCount = tabFeatures.size + if (hasMoreTab) 1 else 0
    val selectedTabIndex = if (tabCount == 0) 0 else selectedTab.coerceIn(0, tabCount - 1)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
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
                    "${context.businessUnitId.value} / ${context.roles.joinToString { it.value }} / ${context.userId}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onBack != null) {
                    OutlinedButton(onClick = onBack) { Text("Back") }
                }
                OutlinedButton(onClick = onLogout) { Text("Logout") }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "Theme: ${selectedTheme.label}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Resolved features: ${features.joinToString { it.title }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            if (features.isEmpty()) {
                SimpleFeatureView("No features", "The resolved experience has no features for this login.")
            } else if (hasMoreTab && selectedTabIndex == tabFeatures.size) {
                MoreFeaturesList(
                    features = overflowFeatures,
                    context = context,
                    modifier = Modifier.fillMaxSize(),
                    onFeatureTapped = onFeatureTapped,
                )
            } else {
                tabFeatures.getOrNull(selectedTabIndex)?.let { feature ->
                    FeatureTabContent(
                        feature = feature,
                        session = session,
                        context = context,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        if (features.isNotEmpty()) {
            NavigationBar {
                tabFeatures.forEachIndexed { index, feature ->
                    NavigationBarItem(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTab = index },
                        icon = { Text(feature.navInitial()) },
                        label = { Text(feature.title) },
                    )
                }
                if (hasMoreTab) {
                    NavigationBarItem(
                        selected = selectedTabIndex == tabFeatures.size,
                        onClick = { selectedTab = tabFeatures.size },
                        icon = { Text("...") },
                        label = { Text("More") },
                    )
                }
            }
        }
    }
}

private fun FeatureDescriptor.navInitial(): String =
    title
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { id.value.take(1).uppercase() }

@Composable
private fun FeatureTabContent(
    feature: FeatureDescriptor,
    session: AppSession,
    context: AppContext,
    modifier: Modifier = Modifier,
) {
    val runtimeSnapshot = session.featureRuntimeSnapshot(feature.id)
    if (runtimeSnapshot != null) {
        RuntimeFeatureView(
            runtimeSnapshot = runtimeSnapshot,
            feature = feature,
            context = context,
            modifier = modifier,
        )
    } else {
        GenericFeatureView(feature = feature, context = context, modifier = modifier)
    }
}

@Composable
private fun MoreFeaturesList(
    features: List<FeatureDescriptor>,
    context: AppContext,
    modifier: Modifier = Modifier,
    onFeatureTapped: (FeatureDescriptor) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Text("More features", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        items(features, key = { it.id.value }) { feature ->
            FeatureSummaryCard(feature = feature, context = context, onFeatureTapped = onFeatureTapped)
        }
    }
}

@Composable
private fun FeatureSummaryCard(
    feature: FeatureDescriptor,
    context: AppContext,
    onFeatureTapped: (FeatureDescriptor) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onFeatureTapped(feature) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                feature.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Requires: ${feature.requiredPermission.value}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val enabledTweaks = feature.enabledTweaks(context)
            if (enabledTweaks.isNotEmpty()) {
                Text(
                    "Enabled tweaks: ${enabledTweaks.joinToString { it.value }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        val feature = session.availableFeatures().firstOrNull { it.id == featureId }
        val runtimeSnapshot = session.featureRuntimeSnapshot(featureId)
        if (runtimeSnapshot != null) {
            RuntimeFeatureView(
                runtimeSnapshot = runtimeSnapshot,
                feature = feature,
                context = context,
            )
        } else {
            if (feature == null) {
                SimpleFeatureView("Unavailable", "Unknown feature: ${featureId.value}")
            } else {
                GenericFeatureView(feature = feature, context = context)
            }
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
private fun GenericFeatureView(
    feature: FeatureDescriptor,
    context: AppContext,
    modifier: Modifier = Modifier,
) {
    var lastActionResult by remember(feature.id, context.userId) {
        mutableStateOf("No action has been triggered yet.")
    }
    val allowedActions = feature.actions.filter { context.hasPermission(it.requiredPermission) }
    val visibleBlocks = feature.uiBlocks.filter { context.hasPermission(it.requiredPermission) }
    val hiddenBlockCount = feature.uiBlocks.size - visibleBlocks.size

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Text(feature.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        items(feature.permissionRows, key = { it.permission.value }) { row ->
            CommercePermissionLine(row.label, context.hasPermission(row.permission))
        }
        item {
            if (hiddenBlockCount > 0) {
                Text(
                    "$hiddenBlockCount capability-driven panel(s) hidden for this login.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(visibleBlocks, key = { it.title }) { block ->
            CapabilityPanel(title = block.title, body = block.body)
        }
        item {
            Text(
                lastActionResult,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                allowedActions.forEach { action ->
                    FilterChip(
                        selected = false,
                        onClick = { lastActionResult = action.result },
                        label = { Text(action.label) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CapabilityPanel(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun RuntimeFeatureView(
    runtimeSnapshot: FeatureRuntimeSnapshot,
    feature: FeatureDescriptor?,
    context: AppContext,
    modifier: Modifier = Modifier,
) {
    var itemStatuses by remember(runtimeSnapshot) {
        mutableStateOf(runtimeSnapshot.items.associate { it.id to it.status })
    }
    var lastActionResult by remember(runtimeSnapshot) { mutableStateOf("No action has been triggered yet.") }
    val visibleBlocks = feature?.uiBlocks.orEmpty().filter { context.hasPermission(it.requiredPermission) }
    val hiddenBlockCount = feature?.uiBlocks.orEmpty().size - visibleBlocks.size

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Text(runtimeSnapshot.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            feature?.permissionRows.orEmpty().forEach { row ->
                CommercePermissionLine(row.label, context.hasPermission(row.permission))
            }
            if (hiddenBlockCount > 0) {
                Text(
                    "$hiddenBlockCount capability-driven panel(s) hidden for this login.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                lastActionResult,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        items(visibleBlocks, key = { it.title }) { block ->
            CapabilityPanel(title = block.title, body = block.body)
        }
        items(runtimeSnapshot.items, key = FeatureRuntimeItem::id) { item ->
            val displayedStatus = itemStatuses[item.id] ?: item.status
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${item.id} / $displayedStatus",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (item.subtitle.isNotBlank()) {
                        Text(item.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item.actions.forEach { action ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    if (action.nextStatus.isNotBlank()) {
                                        itemStatuses = itemStatuses + (item.id to action.nextStatus)
                                    }
                                    lastActionResult = action.result
                                },
                                label = { Text(action.label) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommercePermissionLine(label: String, enabled: Boolean) {
    Text("$label: ${if (enabled) "enabled" else "disabled"}")
}

private fun AppContext.hasPermission(permission: PermissionId): Boolean =
    commerceCapabilities.has(permission)

private fun ResolvedExperienceOption.defaultTheme(): DemoTheme =
    when (experience.id) {
        com.aeshma.multiapp.core.model.ExperienceId.NewportBuckhead -> DemoTheme.Boutique
        com.aeshma.multiapp.core.model.ExperienceId.Shop -> DemoTheme.Broadline
        else -> DemoTheme.Operations
    }
