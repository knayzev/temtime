package com.focustimer.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.focustimer.app.ui.AuthScreen
import com.focustimer.app.ui.HistoryScreen
import com.focustimer.app.ui.LifestyleQuestionsScreen
import com.focustimer.app.ui.OnboardingScreen
import com.focustimer.app.ui.PlanEditorScreen
import com.focustimer.app.ui.ProfileScreen
import com.focustimer.app.ui.ProfileSetupScreen
import com.focustimer.app.ui.RoutineScreen
import com.focustimer.app.ui.SettingsScreen
import com.focustimer.app.ui.StatsScreen
import com.focustimer.app.ui.TimerScreen
import com.focustimer.app.ui.theme.FocusTimerTheme
import com.focustimer.app.ui.theme.RestColor
import com.focustimer.app.ui.theme.ThemeState
import com.focustimer.app.ui.theme.WorkColor

class MainActivity : ComponentActivity() {
    private val timerViewModel: TimerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemeState.dynamicColor = PrefsManager(this).dynamicColorEnabled
        setContent {
            FocusTimerTheme(dynamicColor = ThemeState.dynamicColor) {
                RootNavigator(timerViewModel)
            }
        }
    }
}

private enum class RootScreen { AUTH, PROFILE_SETUP, PLAN_SETUP, MAIN }

private enum class OverlayScreen { ROUTINE, PLAN_EDITOR, LIFESTYLE, DETAILS }

/** One bottom-bar destination. The filled icon marks the active tab, the outlined one the rest. */
private data class NavItem(val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

private val NAV_ITEMS = listOf(
    NavItem("Таймер", Icons.Outlined.Timer, Icons.Filled.Timer),
    NavItem("Планы", Icons.Outlined.EventNote, Icons.Filled.EventNote),
    NavItem("Статистика", Icons.Outlined.BarChart, Icons.Filled.BarChart),
    NavItem("Настройки", Icons.Outlined.Settings, Icons.Filled.Settings),
    NavItem("Профиль", Icons.Outlined.Person, Icons.Filled.Person)
)

private const val TAB_PLANS = 1

@Composable
fun RootNavigator(timerViewModel: TimerViewModel) {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }

    var screen by remember {
        mutableStateOf(
            when {
                !prefs.isRegistered || !prefs.isLoggedIn -> RootScreen.AUTH
                !prefs.isOnboarded -> RootScreen.PROFILE_SETUP
                else -> RootScreen.MAIN
            }
        )
    }
    // Set when setup ends, so the plan that was just built is the one already open on arrival.
    var freshPlanId by remember { mutableStateOf<String?>(null) }

    when (screen) {
        RootScreen.MAIN -> AppRoot(
            timerViewModel = timerViewModel,
            onLogout = {
                prefs.isLoggedIn = false
                screen = RootScreen.AUTH
            },
            initialTab = if (freshPlanId != null) TAB_PLANS else 0,
            initialExpandedPlanId = freshPlanId
        )
        // The pre-MAIN flow has no Scaffold of its own, so it keeps clear of the system bars
        // itself now that the activity draws edge to edge.
        else -> Box(modifier = Modifier.safeDrawingPadding()) {
            when (screen) {
                RootScreen.AUTH -> AuthScreen(
                    startInLoginMode = prefs.isRegistered,
                    onAuthenticated = {
                        screen = if (prefs.isOnboarded) RootScreen.MAIN else RootScreen.PROFILE_SETUP
                    }
                )
                RootScreen.PROFILE_SETUP -> ProfileSetupScreen(
                    onCreatePlan = { screen = RootScreen.PLAN_SETUP }
                )
                RootScreen.PLAN_SETUP -> PlanEditorScreen(
                    isSetupFlow = true,
                    onDone = { planId ->
                        // Finishing the first plan is what completes setup.
                        prefs.isOnboarded = true
                        freshPlanId = planId
                        screen = RootScreen.MAIN
                    }
                )
                RootScreen.MAIN -> Unit
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    timerViewModel: TimerViewModel,
    onLogout: () -> Unit,
    initialTab: Int = 0,
    initialExpandedPlanId: String? = null
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val tabs = NAV_ITEMS.map { it.label }

    var overlayScreen by remember { mutableStateOf<OverlayScreen?>(null) }
    // The plan the editor is opened on; null means the editor builds a new one.
    var editorPlanId by remember { mutableStateOf<String?>(null) }
    var expandedPlanId by remember { mutableStateOf(initialExpandedPlanId) }

    val context = LocalContext.current
    val view = LocalView.current
    val prefs = remember { PrefsManager(context) }
    val timerState by timerViewModel.uiState.collectAsState()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Coming back to the app (from launcher, Recents, or a notification tap) counts as
    // acknowledging any pending phase-change/escalation, since the user is now looking at it.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                timerViewModel.acknowledge()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(timerState.isRunning) {
        view.keepScreenOn = timerState.isRunning && prefs.keepScreenOn
    }

    Scaffold(
        topBar = {
            TopAppBar(
                // Naming the current section here lets each screen drop its own duplicate
                // headline, which buys back a chunk of vertical space on a phone.
                title = {
                    Text(
                        when (overlayScreen) {
                            OverlayScreen.ROUTINE -> "Ритуалы"
                            OverlayScreen.PLAN_EDITOR -> "План"
                            OverlayScreen.LIFESTYLE -> "Образ жизни"
                            OverlayScreen.DETAILS -> "Подробная анкета"
                            null -> tabs[selectedTab]
                        }
                    )
                },
                actions = {
                    // One destination, so it opens directly instead of through a one-item menu.
                    IconButton(onClick = { overlayScreen = OverlayScreen.ROUTINE }) {
                        Icon(Icons.Outlined.Checklist, contentDescription = "Ритуалы")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NAV_ITEMS.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index && overlayScreen == null,
                        onClick = { selectedTab = index; overlayScreen = null },
                        icon = {
                            Icon(
                                if (selectedTab == index && overlayScreen == null) item.selectedIcon
                                else item.icon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            val phaseFresh = timerState.secondsLeft == (if (timerState.phase == TimerPhase.WORK) timerState.workMinutes else timerState.restMinutes) * 60
            val showMiniTimer = timerState.isRunning || !phaseFresh
            if (showMiniTimer) {
                MiniTimerBar(
                    timerState = timerState,
                    onClick = { selectedTab = 0; overlayScreen = null }
                )
            }
            when (overlayScreen) {
                OverlayScreen.ROUTINE -> RoutineScreen(onBack = { overlayScreen = null })
                OverlayScreen.PLAN_EDITOR -> PlanEditorScreen(
                    editingPlanId = editorPlanId,
                    onBack = { overlayScreen = null },
                    onDone = { planId ->
                        expandedPlanId = planId
                        overlayScreen = null
                    }
                )
                OverlayScreen.LIFESTYLE -> LifestyleQuestionsScreen(
                    onComplete = { overlayScreen = null }
                )
                // The long questionnaire is no longer part of setup, but it is the only place
                // some schedule inputs are collected, so it stays reachable from the profile.
                OverlayScreen.DETAILS -> OnboardingScreen(
                    onComplete = { overlayScreen = null }
                )
                null -> when (selectedTab) {
                    0 -> TimerScreen(timerViewModel)
                    1 -> HistoryScreen(
                        onCreatePlan = {
                            editorPlanId = null
                            overlayScreen = OverlayScreen.PLAN_EDITOR
                        },
                        onEditPlan = { id ->
                            editorPlanId = id
                            overlayScreen = OverlayScreen.PLAN_EDITOR
                        },
                        expandedPlanId = expandedPlanId
                    )
                    2 -> StatsScreen()
                    3 -> SettingsScreen(onLogout = onLogout)
                    4 -> ProfileScreen(
                        onOpenLifestyle = { overlayScreen = OverlayScreen.LIFESTYLE },
                        onOpenDetails = { overlayScreen = OverlayScreen.DETAILS }
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniTimerBar(timerState: TimerUiState, onClick: () -> Unit) {
    val phaseColor = if (timerState.phase == TimerPhase.WORK) WorkColor else RestColor
    val phaseLabel = if (timerState.phase == TimerPhase.WORK) "Работа" else "Отдых"
    val minutes = timerState.secondsLeft / 60
    val seconds = timerState.secondsLeft % 60
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(phaseColor.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (timerState.isRunning) Icons.Default.PlayArrow else Icons.Default.Pause,
                contentDescription = null,
                tint = phaseColor
            )
            Text(
                phaseLabel,
                color = phaseColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Text(
            "%02d:%02d".format(minutes, seconds),
            color = phaseColor,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
    }
}
