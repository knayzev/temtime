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
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Divider
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
import com.focustimer.app.ui.MoreScreen
import com.focustimer.app.ui.NotesScreen
import com.focustimer.app.ui.PlanScreen
import com.focustimer.app.ui.LifestyleQuestionsScreen
import com.focustimer.app.ui.OnboardingScreen
import com.focustimer.app.ui.ScheduleSetupScreen
import com.focustimer.app.ui.ProfileSetupScreen
import com.focustimer.app.ui.StatsScreen
import com.focustimer.app.ui.TimerScreen
import com.focustimer.app.ui.theme.FocusTimerTheme
import com.focustimer.app.ui.theme.ThemeState

class MainActivity : ComponentActivity() {
    private val timerViewModel: TimerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Lifts the old plan containers into the flat schedule; a no-op after the first run.
        PrefsManager(this).migratePlansIfNeeded()
        ThemeState.dynamicColor = PrefsManager(this).dynamicColorEnabled
        setContent {
            FocusTimerTheme(dynamicColor = ThemeState.dynamicColor) {
                RootNavigator(timerViewModel)
            }
        }
    }
}

private enum class RootScreen { PROFILE_SETUP, SCHEDULE_SETUP, MAIN }

private enum class OverlayScreen { LIFESTYLE, DETAILS }

/** One bottom-bar destination. The filled icon marks the active tab, the outlined one the rest. */
private const val TAB_PLAN = 1

private data class NavItem(val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

private val NAV_ITEMS = listOf(
    NavItem("Сегодня", Icons.Outlined.Timer, Icons.Filled.Timer),
    NavItem("План", Icons.Outlined.EventNote, Icons.Filled.EventNote),
    NavItem("Заметки", Icons.Outlined.StickyNote2, Icons.Filled.StickyNote2),
    NavItem("Итоги", Icons.Outlined.BarChart, Icons.Filled.BarChart),
    NavItem("Ещё", Icons.Outlined.Tune, Icons.Filled.Tune)
)

@Composable
fun RootNavigator(timerViewModel: TimerViewModel) {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }

    // There is no account to sign in to: the app opens on the setup once and on the day itself
    // ever after. Name and e-mail are optional and live in the profile.
    var screen by remember {
        mutableStateOf(if (prefs.isOnboarded) RootScreen.MAIN else RootScreen.PROFILE_SETUP)
    }

    when (screen) {
        RootScreen.MAIN -> AppRoot(timerViewModel)
        // The pre-MAIN flow has no Scaffold of its own, so it keeps clear of the system bars
        // itself now that the activity draws edge to edge.
        else -> Box(modifier = Modifier.safeDrawingPadding()) {
            when (screen) {
                RootScreen.PROFILE_SETUP -> ProfileSetupScreen(
                    onNext = { screen = RootScreen.SCHEDULE_SETUP }
                )
                RootScreen.SCHEDULE_SETUP -> ScheduleSetupScreen(
                    onDone = { screen = RootScreen.MAIN }
                )
                RootScreen.MAIN -> Unit
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(timerViewModel: TimerViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = NAV_ITEMS.map { it.label }

    var overlayScreen by remember { mutableStateOf<OverlayScreen?>(null) }

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
                            OverlayScreen.LIFESTYLE -> "Образ жизни"
                            OverlayScreen.DETAILS -> "Подробная анкета"
                            null -> tabs[selectedTab]
                        }
                    )
                },
            )
        },
        bottomBar = {
            // A white bar under a hairline rather than a tinted slab: with a black accent the
            // default tonal surface reads as a grey block at the bottom of every screen.
            Column {
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
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
                OverlayScreen.LIFESTYLE -> LifestyleQuestionsScreen(
                    onComplete = { overlayScreen = null }
                )
                // The long questionnaire is no longer part of setup, but it is the only place
                // some schedule inputs are collected, so it stays reachable from the profile.
                OverlayScreen.DETAILS -> OnboardingScreen(
                    onComplete = { overlayScreen = null }
                )
                null -> when (selectedTab) {
                    0 -> TimerScreen(
                        viewModel = timerViewModel,
                        // The week is a tab now, so the link jumps there instead of stacking
                        // a second way to reach the same screen.
                        onOpenWeek = { selectedTab = TAB_PLAN }
                    )
                    1 -> PlanScreen()
                    2 -> NotesScreen()
                    3 -> StatsScreen()
                    4 -> MoreScreen(
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
    val phaseColor = if (timerState.phase == TimerPhase.WORK) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.tertiary
    val phaseLabel = if (timerState.phase == TimerPhase.WORK) "Работа" else "Отдых"
    val minutes = timerState.secondsLeft / 60
    val seconds = timerState.secondsLeft % 60
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(phaseColor.copy(alpha = 0.1f))
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
