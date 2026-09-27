package com.focustimer.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focustimer.app.PrefsManager
import com.focustimer.app.TimerPhase
import com.focustimer.app.TimerPreset
import com.focustimer.app.TimerViewModel
import com.focustimer.app.ui.theme.RestColor
import com.focustimer.app.ui.theme.WorkColor

@Composable
fun TimerScreen(viewModel: TimerViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    val minutes = state.secondsLeft / 60
    val seconds = state.secondsLeft % 60
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }
    val categories = remember { prefs.categories }
    val liveSteps = rememberLiveStepCount(prefs.stepsEnabled)

    // Being on this screen counts as noticing the current phase.
    LaunchedEffect(Unit) {
        viewModel.acknowledge()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            // The dial makes this screen tall enough to overflow a small phone, so it scrolls;
            // Center still applies while the content is shorter than the viewport.
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (state.escalationActive) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
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

        if (!state.isRunning) {
            PresetRow(
                onApply = { preset ->
                    viewModel.setWorkMinutes(preset.workMinutes)
                    viewModel.setRestMinutes(preset.restMinutes)
                    if (preset.comment.isNotBlank()) viewModel.setComment(preset.comment)
                }
            )
        }

        if (liveSteps != null) {
            Text(
                "Шаги сегодня: $liveSteps",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        state.motivationQuote?.let { quote ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
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

        if (categories.isNotEmpty()) {
            DropdownField(
                label = "Чем занимаетесь",
                selected = state.currentCategory.ifBlank { categories.first() },
                options = categories,
                onSelected = { viewModel.setCategory(it) },
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        val phaseMinutes = if (state.phase == TimerPhase.WORK) state.workMinutes else state.restMinutes
        val phaseSeconds = phaseMinutes * 60
        TimerDial(
            // Fraction of the phase still to go, so the ring empties as the time runs out.
            fraction = if (phaseSeconds > 0) state.secondsLeft.toFloat() / phaseSeconds else 0f,
            phaseLabel = if (state.phase == TimerPhase.WORK) "Работа" else "Отдых",
            timeText = "%02d:%02d".format(minutes, seconds),
            phaseColor = if (state.phase == TimerPhase.WORK) WorkColor else RestColor,
            modifier = Modifier.padding(vertical = 20.dp)
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
            Spacer(modifier = Modifier.width(32.dp))
            DialAction(
                icon = if (state.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                label = if (state.isRunning) "Пауза" else "Старт",
                onClick = { if (state.isRunning) viewModel.pause() else viewModel.start() },
                primary = true
            )
        }

        var showCommentEditor by remember { mutableStateOf(false) }
        var draftComment by remember { mutableStateOf(state.currentComment) }

        if (!showCommentEditor) {
            if (state.currentComment.isBlank()) {
                TextButton(
                    onClick = {
                        draftComment = state.currentComment
                        showCommentEditor = true
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Добавить комментарий")
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
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
                    .padding(top = 16.dp)
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

        if (!state.isRunning) {
            Column(modifier = Modifier.padding(top = 32.dp).fillMaxWidth()) {
                MinutesInputRow(
                    label = "Время работы",
                    minutes = state.workMinutes,
                    onMinutesChange = { viewModel.setWorkMinutes(it) }
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
                    modifier = Modifier.padding(top = 16.dp)
                )
                Slider(
                    value = state.restMinutes.toFloat().coerceIn(5f, 100f),
                    onValueChange = { viewModel.setRestMinutes(it.toInt()) },
                    valueRange = 5f..100f,
                    steps = 18
                )
            }
        }
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
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .height(36.dp)
                .clickable { showAddNew = true }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text("+ Добавить", color = MaterialTheme.colorScheme.onPrimaryContainer)
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
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.height(36.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                preset.label,
                modifier = Modifier
                    .clickable(onClick = onClick)
                    .padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Изменить ${preset.label}",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
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
 * The countdown as a ring that drains over the phase. [fraction] is the share of the phase still
 * left, so a full ring means the phase has just begun.
 */
@Composable
private fun TimerDial(
    fraction: Float,
    phaseLabel: String,
    timeText: String,
    phaseColor: Color,
    modifier: Modifier = Modifier
) {
    // Animating the sweep keeps the ring from stepping a visible notch every second.
    val sweep by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "timerSweep"
    )
    Box(modifier = modifier.size(248.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 18.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = phaseColor.copy(alpha = 0.15f),
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
                color = phaseColor
            )
            Text(
                timeText,
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp)
            )
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
    val diameter = if (primary) 76.dp else 58.dp
    val background =
        if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val foreground =
        if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

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
                modifier = Modifier.size(if (primary) 34.dp else 24.dp)
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
