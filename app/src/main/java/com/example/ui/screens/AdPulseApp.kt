package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.auth.AuthScreen
import com.example.auth.signOut
import com.example.data.APP_OWNER_EMAIL
import com.example.data.AdPulseRepository
import com.example.data.UiState
import com.example.ui.components.AdPreferencesBottomSheet
import com.example.viewmodel.AdPulseViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_EARN = "earn"
const val ROUTE_ANALYTICS = "analytics"
const val ROUTE_WALLET = "wallet"
const val ROUTE_OWNER_INCOME = "income"
const val ROUTE_SETTINGS = "settings"

private enum class AdPulseDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    EARN(ROUTE_EARN, "Earn", Icons.Rounded.PlayCircle, Icons.Outlined.PlayCircle),
    ANALYTICS(ROUTE_ANALYTICS, "Analytics", Icons.Rounded.Insights, Icons.Outlined.Insights),
    WALLET(ROUTE_WALLET, "Wallet", Icons.Rounded.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    OWNER_INCOME(ROUTE_OWNER_INCOME, "Income", Icons.Rounded.Payments, Icons.Outlined.Payments),
}

@Composable
fun AdPulseAppRoot(auth: FirebaseAuth = Firebase.auth) {
    var currentUser by remember { mutableStateOf(auth.currentUser) }
    var showSplash by rememberSaveable { mutableStateOf(true) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val user = currentUser
        if (user == null) {
            AuthScreen(onAuthSuccess = { currentUser = auth.currentUser })
        } else {
            val isVerifiedOwner = user.email == APP_OWNER_EMAIL && user.isEmailVerified
            key(user.uid, isVerifiedOwner) {
                AuthenticatedAdPulseShell(
                    currentUserId = user.uid,
                    isVerifiedOwner = isVerifiedOwner,
                )
            }
        }

        AnimatedVisibility(
            visible = showSplash,
            exit = fadeOut(animationSpec = tween(durationMillis = 450)) +
                scaleOut(targetScale = 1.04f, animationSpec = tween(durationMillis = 450)),
        ) {
            SideHustleSplashScreen(
                onFinished = { showSplash = false },
            )
        }
    }
}

@Composable
private fun AuthenticatedAdPulseShell(
    currentUserId: String,
    isVerifiedOwner: Boolean,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: AdPulseViewModel = viewModel(
        key = "${currentUserId}_$isVerifiedOwner",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                AdPulseViewModel(
                    repository = AdPulseRepository(db),
                    currentUserId = currentUserId,
                    isVerifiedAppOwner = isVerifiedOwner,
                )
            }
        },
    )

    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val ownerConsoleState by viewModel.ownerConsoleState.collectAsStateWithLifecycle()
    val selectedFilterCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val analyticsRange by viewModel.analyticsRange.collectAsStateWithLifecycle()
    val selectedDateKey by viewModel.selectedDateKey.collectAsStateWithLifecycle()
    val activeWatchSession by viewModel.activeWatchSession.collectAsStateWithLifecycle()
    val rewardOutcome by viewModel.rewardOutcome.collectAsStateWithLifecycle()
    val isPayoutSubmitting by viewModel.isPayoutSubmitting.collectAsStateWithLifecycle()
    val isSavingPaymentSettings by viewModel.isSavingPaymentSettings.collectAsStateWithLifecycle()
    val isApplyingReferralCode by viewModel.isApplyingReferralCode.collectAsStateWithLifecycle()
    val isOwnerConfigSaving by viewModel.isOwnerConfigSaving.collectAsStateWithLifecycle()
    val isOwnerWithdrawing by viewModel.isOwnerWithdrawing.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val visibleDestinations = remember(isVerifiedOwner) {
        if (isVerifiedOwner) {
            AdPulseDestination.entries
        } else {
            AdPulseDestination.entries.filter { it != AdPulseDestination.OWNER_INCOME }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var showPreferencesSheet by remember { mutableStateOf(false) }

    LaunchedEffect(snackbarMessage) {
        val message = snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearSnackbar()
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_EARN

    val navigateTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail && activeWatchSession == null && currentRoute != ROUTE_SETTINGS) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        visibleDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label,
                                    )
                                },
                                label = { Text(dest.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                                modifier = Modifier.testTag("nav_${dest.route}"),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useRail && activeWatchSession == null && currentRoute != ROUTE_SETTINGS) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        visibleDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label,
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag("rail_${dest.route}"),
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    when (val state = dashboardState) {
                        is UiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        is UiState.Error -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "Unable to load Side Hustle data",
                                    style = MaterialTheme.typography.titleLarge,
                                )
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                                )
                                Button(
                                    onClick = {
                                        signOut(credentialManager, scope)
                                    }
                                ) {
                                    Text("Switch Account")
                                }
                            }
                        }

                        is UiState.Success -> {
                            val data = state.data
                            val analytics = remember(data, analyticsRange) {
                                viewModel.computeAnalytics(data, analyticsRange)
                            }
                            val prioritizedCampaigns = remember(
                                data.profile.preferredCategories,
                                selectedFilterCategory,
                                data.todayViewsByCampaign,
                            ) {
                                viewModel.prioritizedCampaigns(
                                    preferredCategories = data.profile.preferredCategories,
                                    selectedFilterCategory = selectedFilterCategory,
                                    todayViewsByCampaign = data.todayViewsByCampaign,
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .widthIn(max = 760.dp)
                                    .fillMaxSize(),
                            ) {
                                NavHost(
                                    navController = navController,
                                    startDestination = ROUTE_EARN,
                                ) {
                                    composable(ROUTE_EARN) {
                                        EarnScreen(
                                            data = data,
                                            analytics = analytics,
                                            prioritizedCampaigns = prioritizedCampaigns,
                                            selectedFilterCategory = selectedFilterCategory,
                                            onSelectFilterCategory = viewModel::selectCategory,
                                            onOpenPreferences = { showPreferencesSheet = true },
                                            onQuickSavePreferences = viewModel::updatePreferredCategories,
                                            onWatchCampaign = viewModel::startWatchingCampaign,
                                            onUpdateDailyGoal = viewModel::updateDailyGoal,
                                            isApplyingReferralCode = isApplyingReferralCode,
                                            onApplyReferralCode = viewModel::applyReferralCode,
                                            onClaimPendingReferral = { referral ->
                                                viewModel.claimPendingReferrerBonus(referral, isAutomatic = false)
                                            },
                                            onNotifyMessage = viewModel::notifyMessage,
                                            onNavigateToWallet = { navigateTo(ROUTE_WALLET) },
                                            onNavigateToSettings = {
                                                navController.navigate(ROUTE_SETTINGS) {
                                                    launchSingleTop = true
                                                }
                                            },
                                        )
                                    }

                                    composable(ROUTE_ANALYTICS) {
                                        AnalyticsScreen(
                                            data = data,
                                            analytics = analytics,
                                            selectedRange = analyticsRange,
                                            selectedDateKey = selectedDateKey,
                                            onSelectRange = viewModel::selectAnalyticsRange,
                                            onSelectDateKey = viewModel::selectDayForDrilldown,
                                        )
                                    }

                                    composable(ROUTE_WALLET) {
                                        WalletScreen(
                                            data = data,
                                            isPayoutSubmitting = isPayoutSubmitting,
                                            onRequestPayout = viewModel::requestPayout,
                                            onUpdateDailyGoal = viewModel::updateDailyGoal,
                                            onOpenPreferences = { showPreferencesSheet = true },
                                            onOpenSettings = {
                                                navController.navigate(ROUTE_SETTINGS) {
                                                    launchSingleTop = true
                                                }
                                            },
                                            onSignOut = {
                                                viewModel.cancelWatching()
                                                signOut(credentialManager, scope)
                                            },
                                        )
                                    }

                                    composable(ROUTE_SETTINGS) {
                                        SettingsScreen(
                                            data = data,
                                            isSavingPaymentSettings = isSavingPaymentSettings,
                                            onBack = { navController.popBackStack() },
                                            onSaveLinkedPaymentMethod = viewModel::saveLinkedPaymentMethod,
                                            onUnlinkPaymentMethod = viewModel::unlinkPaymentMethod,
                                            onOpenPreferences = { showPreferencesSheet = true },
                                            onSignOut = {
                                                viewModel.cancelWatching()
                                                signOut(credentialManager, scope)
                                            },
                                        )
                                    }

                                    if (isVerifiedOwner) {
                                        composable(ROUTE_OWNER_INCOME) {
                                            OwnerRevenueScreen(
                                                ownerState = ownerConsoleState,
                                                isOwnerConfigSaving = isOwnerConfigSaving,
                                                isOwnerWithdrawing = isOwnerWithdrawing,
                                                onSaveOwnerConfig = viewModel::saveOwnerPayoutConfig,
                                                onWithdrawOwnerIncome = viewModel::withdrawOwnerIncome,
                                            )
                                        }
                                    }
                                }
                            }

                            if (showPreferencesSheet) {
                                AdPreferencesBottomSheet(
                                    currentPreferences = data.profile.preferredCategories,
                                    onDismiss = { showPreferencesSheet = false },
                                    onSave = viewModel::updatePreferredCategories,
                                )
                            }
                        }
                    }
                }
            }
        }

        activeWatchSession?.let { session ->
            WatchAdOverlay(
                session = session,
                onCancelConfirmed = viewModel::cancelWatching,
                onVerifyAndClaim = viewModel::verifyAndClaimReward,
            )
        }

        rewardOutcome?.let { outcome ->
            RewardEarnedDialog(
                outcome = outcome,
                onDismiss = viewModel::dismissRewardOutcome,
            )
        }
    }
}
