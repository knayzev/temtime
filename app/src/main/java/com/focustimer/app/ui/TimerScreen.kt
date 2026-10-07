package com.focustimer.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focustimer.app.DayTask
import com.focustimer.app.MAX_PLAN_ITEMS
import com.focustimer.app.PlanItem
import com.focustimer.app.PrefsManager
import com.focustimer.app.TimerPhase
import com.focustimer.app.TimerPreset
import com.focustimer.app.TimerViewModel

@Composable
fun TimerScreen(
    viewModel: TimerViewModel,
    onOpenWeek: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val minutes = state.secondsLeft / 60
    val seconds = state.secondsLeft % 60
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }
    val categories = remember { prefs.categories }
    val liveSteps = rememberLiveStepCount(prefs.stepsEnabled)

    val todayKey = remember { dayStamp() }
    val weekday = remember { todayWeekday() }
    var planItems by remember { mutableStateOf(prefs.planItems) }
    var doneIds by remember { mutableStateOf(prefs.completedPlanIds(todayKey)) }
    var selectedPlanId by remember { mutableStateOf<String?>(null) }
    var editingItem by remember { mutableStateOf<PlanItem?>(null) }
    var addingItem by remember { mutableStateOf(false) }
    var wordHidden by remember { mutableStateOf(prefs.wordSeenDate == todayKey) }
    var showCustomTimer by remember { mutableStateOf(false) }
    var dayTasks by remember { mutableStateOf(prefs.dayTasks(todayKey)) }
    val term = remember { wordOfTheDay() }

    // The schedule is kept per weekday, so only what belongs to today reaches this screen.
    val todayItems = planItems.filter { it.onDay(weekday) }
    val selectedPlan = todayItems.firstOrNull { it.id == selectedPlanId }

    /** Loads an entry into the timer without starting it: the user decides when to begin. */
    fun selectPlan(item: PlanItem) {
        selectedPlanId = item.id
        viewModel.setWorkMinutes(item.minutes)
        viewModel.setCategory(item.title)
    }

    // The service flips WORK to REST on its own when the work phase runs out. That transition is
    // what "the entry is finished" means here, so it ticks the entry and queues the next one.
    var lastPhase by remember { mutableStateOf(state.phase) }
    LaunchedEffect(state.phase) {
        if (lastPhase == TimerPhase.WORK && state.phase == TimerPhase.REST) {
            selectedPlanId?.let { finishedId ->
                prefs.setPlanItemDone(todayKey, finishedId, true)
                doneIds = prefs.completedPlanIds(todayKey)
                val next = todayItems.firstOrNull { !doneIds.contains(it.id) }
                selectedPlanId = next?.id
                if (next != null) {
                    viewModel.setWorkMinutes(next.minutes)
                    viewModel.setCategory(next.title)
                }
            }
        }
        lastPhase = state.phase
    }

    // Being on this screen counts as noticing the current phase.
    LaunchedEffect(Unit) {
        viewModel.acknowledge()
    }

    var settingsExpanded by remember { mutableStateOf(false) }
    var showCommentEditor by remember { mutableStateOf(false) }
    var draftComment by remember { mutableStateOf(state.currentComment) }
    // Work and rest are roles of the scheme rather than fixed colours, so the ring stays visible
    // on the dark theme, where the accent turns white.
    val phaseColor = if (state.phase == TimerPhase.WORK) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.tertiary

    Column(
        modifier = modifier
            .fillMaxSize()
            // The schedule under the timer makes this screen taller than any phone, so it scrolls.
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (state.escalationActive) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Окно пропущено — вас уведомили",
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                TextButton(onClick = { viewModel.acknowledge() }) {
                    Text("Я тут")
                }
            }
        }

        state.motivationQuote?.let { quote ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
                ) {
                    Text(
                        quote,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.dismissQuote() }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Скрыть",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }

        // The timer comes first and the schedule right after it: those two are the screen.
        // Everything that is set once and then left alone is folded into one line between them.
        val phaseMinutes = if (state.phase == TimerPhase.WORK) state.workMinutes else state.restMinutes
        val phaseSeconds = phaseMinutes * 60
        TimerDial(
            // Fraction of the phase still to go, so the ring empties as the time runs out.
            fraction = if (phaseSeconds > 0) state.secondsLeft.toFloat() / phaseSeconds else 0f,
            // With an entry loaded the ring says what it is, not just that work is happening.
            phaseLabel = when {
                state.phase == TimerPhase.REST -> "Отдых"
                selectedPlan != null -> selectedPlan.title
                // Without a rest phase there is no work/rest cycle to name.
                state.restMinutes <= 0 -> "Таймер"
                else -> "Работа"
            },
            timeText = "%02d:%02d".format(minutes, seconds),
            caption = when {
                state.phase == TimerPhase.REST -> "перерыв"
                selectedPlan != null -> "по плану в ${selectedPlan.time}"
                else -> null
            },
            phaseColor = phaseColor,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DialAction(
                icon = Icons.Default.Stop,
                label = "Стоп",
                onClick = { viewModel.stop() },
                primary = false
            )
            Spacer(modifier = Modifier.width(28.dp))
            DialAction(
                icon = if (state.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                label = if (state.isRunning) "Пауза" else "Старт",
                onClick = { if (state.isRunning) viewModel.pause() else viewModel.start() },
                primary = true
            )
        }

        // One tap starts a plain countdown. It deliberately sits above the fold, next to the
        // buttons, because it is the shortest path on this screen.
        if (!state.isRunning) {
            QuickTimerRow(
                onPick = { minutes ->
                    selectedPlanId = null
                    viewModel.startPlain(minutes)
                },
                onCustom = { showCustomTimer = true },
                modifier = Modifier.padding(top = 18.dp)
            )
        }

        if (liveSteps != null) {
            Text(
                "Шаги сегодня: $liveSteps",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        TimerSettingsCard(
            summary = "Работа ${state.workMinutes} мин · " +
                if (state.restMinutes > 0) "отдых ${state.restMinutes} мин" else "без отдыха",
            expanded = settingsExpanded,
            onToggle = { settingsExpanded = !settingsExpanded }
        ) {
            if (!state.isRunning) {
                PresetRow(
                    onApply = { preset ->
                        viewModel.setWorkMinutes(preset.workMinutes)
                        viewModel.setRestMinutes(preset.restMinutes)
                        if (preset.comment.isNotBlank()) viewModel.setComment(preset.comment)
                    }
                )
            }

            if (categories.isNotEmpty()) {
                DropdownField(
                    label = "Чем занимаетесь",
                    selected = state.currentCategory.ifBlank { categories.first() },
                    options = categories,
                    onSelected = { viewModel.setCategory(it) },
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (!state.isRunning) {
                MinutesInputRow(
                    label = "Время работы",
                    minutes = state.workMinutes,
                    onMinutesChange = { viewModel.setWorkMinutes(it) },
                    modifier = Modifier.padding(top = 16.dp)
                )
                Slider(
                    value = state.workMinutes.toFloat().coerceIn(5f, 100f),
                    onValueChange = { viewModel.setWorkMinutes(it.toInt()) },
                    valueRange = 5f..100f,
                    steps = 18
                )
                MinutesInputRow(
                    label = "Время отдыха",
                    minutes = state.restMinutes,
                    onMinutesChange = { viewModel.setRestMinutes(it) },
                    modifier = Modifier.padding(top = 8.dp)
                )
                Slider(
                    value = state.restMinutes.toFloat().coerceIn(5f, 100f),
                    onValueChange = { viewModel.setRestMinutes(it.toInt()) },
                    valueRange = 5f..100f,
                    steps = 18
                )
            } else {
                Text(
                    "Длительность можно менять, когда таймер стоит",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            if (!showCommentEditor) {
                if (state.currentComment.isBlank()) {
                    TextButton(
                        onClick = {
                            draftComment = state.currentComment
                            showCommentEditor = true
                        }
                    ) {
                        Text("Добавить комментарий")
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            state.currentComment,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        IconButton(onClick = {
                            draftComment = state.currentComment
                            showCommentEditor = true
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Изменить комментарий")
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = draftComment,
                    onValueChange = { draftComment = it },
                    label = { Text("Комментарий к сессии") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showCommentEditor = false }) {
                        Text("Отмена")
                    }
                    Button(onClick = {
                        viewModel.setComment(draftComment)
                        showCommentEditor = false
                    }) {
                        Text("Сохранить")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        DaySchedule(
            items = todayItems,
            doneIds = doneIds,
            runningId = if (state.phase == TimerPhase.WORK) selectedPlanId else null,
            nowMinutes = nowMinutesOfDay(),
            onSelect = { selectPlan(it) },
            onToggleDone = { item ->
                val nowDone = !doneIds.contains(item.id)
                prefs.setPlanItemDone(todayKey, item.id, nowDone)
                doneIds = prefs.completedPlanIds(todayKey)
                // A ticked entry should not stay loaded in the timer as if it were still ahead.
                if (nowDone && selectedPlanId == item.id) selectedPlanId = null
            },
            onEdit = { editingItem = it },
            onAdd = { addingItem = true }
        )
        if (planItems.size >= MAX_PLAN_ITEMS) {
            Text(
                "Достигнут предел — $MAX_PLAN_ITEMS пунктов на день",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        TextButton(
            onClick = onOpenWeek,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        ) {
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text("Весь график по дням", modifier = Modifier.padding(start = 6.dp))
        }

        Divider(modifier = Modifier.padding(top = 20.dp, bottom = 18.dp))

        DayTasks(
            tasks = dayTasks,
            onToggle = { task ->
                prefs.setDayTasks(
                    todayKey,
                    dayTasks.map { if (it.id == task.id) it.copy(done = !it.done) else it }
                )
                dayTasks = prefs.dayTasks(todayKey)
            },
            onRemove = { task ->
                prefs.setDayTasks(todayKey, dayTasks.filterNot { it.id == task.id })
                dayTasks = prefs.dayTasks(todayKey)
            },
            onAdd = { title ->
                prefs.setDayTasks(
                    todayKey,
                    dayTasks + DayTask(id = "daytask_${System.currentTimeMillis()}", title = title)
                )
                dayTasks = prefs.dayTasks(todayKey)
            }
        )

        // Read once and dismissed, so it sits under the schedule instead of pushing the timer down.
        if (!wordHidden) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Слово дня",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            term.word,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            term.meaning,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    IconButton(onClick = {
                        prefs.wordSeenDate = todayKey
                        wordHidden = true
                    }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Скрыть до завтра",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }

    if (showCustomTimer) {
        CustomTimerDialog(
            onDismiss = { showCustomTimer = false },
            onStart = { value ->
                selectedPlanId = null
                viewModel.startPlain(value)
                showCustomTimer = false
            }
        )
    }

    if (addingItem) {
        PlanItemDialog(
            existing = null,
            defaultTime = minutesToClock(
                planItems.maxOfOrNull { parseMinutes(it.time) + it.minutes } ?: nowMinutesOfDay()
            ),
            onDismiss = { addingItem = false },
            onConfirm = { item ->
                if (planItems.size < MAX_PLAN_ITEMS) {
                    prefs.planItems = planItems + item
                    planItems = prefs.planItems
                }
                addingItem = false
            }
        )
    }

    editingItem?.let { item ->
        PlanItemDialog(
            existing = item,
            defaultTime = item.time,
            onDismiss = { editingItem = null },
            onConfirm = { updated ->
                prefs.planItems = planItems.map { if (it.id == updated.id) updated else it }
                planItems = prefs.planItems
                // Picking up a new length only makes sense while that entry is not mid-run.
                if (selectedPlanId == updated.id && !state.isRunning) selectPlan(updated)
                editingItem = null
            },
            onDelete = {
                prefs.planItems = planItems.filterNot { it.id == item.id }
                planItems = prefs.planItems
                if (selectedPlanId == item.id) selectedPlanId = null
                editingItem = null
            }
        )
    }
}

@Composable
private fun MinutesInputRow(
    label: String,
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember(minutes) { mutableStateOf(minutes.toString()) }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$label, мин")
        OutlinedTextField(
            value = text,
            onValueChange = { input ->
                text = input
                input.toIntOrNull()?.let { value ->
                    if (value in 1..300) onMinutesChange(value)
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(90.dp)
        )
    }
}

@Composable
private fun PresetRow(onApply: (TimerPreset) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }
    var presets by remember { mutableStateOf(prefs.presets) }
    var editingPreset by remember { mutableStateOf<TimerPreset?>(null) }
    var showAddNew by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        presets.forEach { preset ->
            PresetChip(
                preset = preset,
                onClick = { onApply(preset) },
                onEditClick = { editingPreset = preset }
            )
        }
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable { showAddNew = true }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    "+ Добавить",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }

    if (editingPreset != null || showAddNew) {
        val current = editingPreset
        PresetEditDialog(
            preset = current,
            onDismiss = {
                editingPreset = null
                showAddNew = false
            },
            onSave = { updated ->
                presets = if (current != null) {
                    presets.map { if (it.id == updated.id) updated else it }
                } else {
                    presets + updated
                }
                prefs.presets = presets
                editingPreset = null
                showAddNew = false
            },
            onDelete = if (current != null) {
                {
                    presets = presets.filter { it.id != current.id }
                    prefs.presets = presets
                    editingPreset = null
                }
            } else null
        )
    }
}

@Composable
private fun PresetChip(preset: TimerPreset, onClick: () -> Unit, onEditClick: () -> Unit) {
    // White with a hairline: the chips sit on the grey settings card, where a tinted chip
    // would disappear.
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.height(36.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                preset.label,
                modifier = Modifier
                    .clickable(onClick = onClick)
                    .padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Изменить ${preset.label}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun PresetEditDialog(
    preset: TimerPreset?,
    onDismiss: () -> Unit,
    onSave: (TimerPreset) -> Unit,
    onDelete: (() -> Unit)?
) {
    var label by remember { mutableStateOf(preset?.label ?: "") }
    var work by remember { mutableStateOf((preset?.workMinutes ?: 25).toString()) }
    var rest by remember { mutableStateOf((preset?.restMinutes ?: 5).toString()) }
    var comment by remember { mutableStateOf(preset?.comment ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (preset != null) "Изменить таб" else "Новый таб") },
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = work,
                        onValueChange = { work = it },
                        label = { Text("Работа, мин") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = rest,
                        onValueChange = { rest = it },
                        label = { Text("Отдых, мин") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Комментарий по умолчанию") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (label.isNotBlank()) {
                    onSave(
                        TimerPreset(
                            id = preset?.id ?: "preset_${System.currentTimeMillis()}",
                            label = label,
                            workMinutes = work.toIntOrNull()?.coerceIn(1, 180) ?: 25,
                            restMinutes = rest.toIntOrNull()?.coerceIn(1, 180) ?: 5,
                            comment = comment
                        )
                    )
                }
            }) { Text("Сохранить") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Удалить", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Отмена") }
            }
        }
    )
}

/**
 * Everything about the timer that is set once and then left alone — presets, lengths, category,
 * comment — folded into one line, so the day's schedule sits right under the controls. The line
 * itself says what is set, so it rarely needs opening.
 */
@Composable
private fun TimerSettingsCard(
    summary: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text("Настройка таймера", style = MaterialTheme.typography.titleSmall)
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Свернуть" else "Развернуть",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (expanded) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    content = content
                )
            }
        }
    }
}

/**
 * The countdown as a ring that drains over the phase. [fraction] is the share of the phase still
 * left, so a full ring means the phase has just begun. [caption] is the small line under the
 * digits: when the loaded entry is planned for, or that this is a break.
 */
@Composable
private fun TimerDial(
    fraction: Float,
    phaseLabel: String,
    timeText: String,
    caption: String?,
    phaseColor: Color,
    modifier: Modifier = Modifier
) {
    // Animating the sweep keeps the ring from stepping a visible notch every second.
    val sweep by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "timerSweep"
    )
    Box(modifier = modifier.size(232.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = phaseColor.copy(alpha = 0.12f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke)
            )
            drawArc(
                color = phaseColor,
                startAngle = -90f,
                sweepAngle = 360f * sweep,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                phaseLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = phaseColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                // An entry's title can be long; it stays inside the ring on two centred lines.
                // Only the title is inset: the digits need the full width for a three-digit count.
                modifier = Modifier.padding(horizontal = 34.dp)
            )
            Text(
                timeText,
                fontSize = 56.sp,
                // Without its own line height the digits take the body text's, which is shorter
                // than they are, and they run into the lines above and below.
                lineHeight = 64.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            if (caption != null) {
                Text(
                    caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** A round tap target with its name underneath, so the icon never has to carry the meaning alone. */
@Composable
private fun DialAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    primary: Boolean
) {
    val haptics = LocalHapticFeedback.current
    val diameter = if (primary) 72.dp else 56.dp
    val background =
        if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer
    val foreground =
        if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(diameter)
                .clip(CircleShape)
                .background(background)
                .clickable {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = foreground,
                modifier = Modifier.size(if (primary) 32.dp else 22.dp)
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

/** Durations offered for a one-tap countdown. */
private val QUICK_TIMER_MINUTES = listOf(5, 10, 15, 25, 45, 60)

/**
 * A plain countdown: pick a length and it starts. No rest phase, no category, no plan entry —
 * the shortest thing this screen can do.
 */
@Composable
private fun QuickTimerRow(
    onPick: (Int) -> Unit,
    onCustom: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "Быстрый таймер",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QUICK_TIMER_MINUTES.forEach { value ->
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { onPick(value) }
                ) {
                    Text(
                        "$value мин",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onCustom)
            ) {
                Text(
                    "Своё время",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                )
            }
        }
    }
}

@Composable
private fun CustomTimerDialog(onDismiss: () -> Unit, onStart: (Int) -> Unit) {
    var minutes by remember { mutableStateOf("") }
    val parsed = minutes.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Свой таймер") },
        text = {
            Column {
                Text(
                    "Отсчёт на указанное время. Без отдыха после — просто закончится.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { minutes = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text("Минут") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (parsed != null && parsed > 0) onStart(parsed) },
                enabled = parsed != null && parsed > 0
            ) { Text("Запустить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
