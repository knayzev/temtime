package com.focustimer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focustimer.app.PrefsManager
import com.focustimer.app.RITUAL_PERIODS
import com.focustimer.app.ROUTINE_TASK_LIBRARY
import com.focustimer.app.RoutineTask
import com.focustimer.app.ritualPeriodEmoji
import com.focustimer.app.ritualPeriodTitle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
private val weekdayShort = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

private fun dateKeyOf(cal: Calendar): String = dateKeyFormat.format(cal.time)

private fun mondayOfWeek(base: Calendar): Calendar {
    val cal = base.clone() as Calendar
    val dow = cal.get(Calendar.DAY_OF_WEEK)
    val diff = if (dow == Calendar.SUNDAY) -6 else Calendar.MONDAY - dow
    cal.add(Calendar.DAY_OF_MONTH, diff)
    return cal
}

private fun datesBefore(anchor: String, count: Int): List<String> {
    val cal = Calendar.getInstance()
    cal.time = try { dateKeyFormat.parse(anchor) ?: Date() } catch (_: Exception) { Date() }
    val result = mutableListOf<String>()
    repeat(count) {
        cal.add(Calendar.DAY_OF_MONTH, -1)
        result.add(dateKeyOf(cal))
    }
    return result
}

private fun greetingFor(userName: String): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val base = when {
        hour < 5 -> "Доброй ночи"
        hour < 12 -> "Доброе утро"
        hour < 18 -> "Добрый день"
        else -> "Добрый вечер"
    }
    return if (userName.isNotBlank()) "$base, $userName" else base
}

@Composable
fun RoutineScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }
    val todayKey = remember { dateKeyOf(Calendar.getInstance()) }
    val previousDates = remember { datesBefore(todayKey, 60) }

    var tasks by remember { mutableStateOf(prefs.routineTasks) }
    var selectedDate by remember { mutableStateOf(todayKey) }
    var completedIds by remember { mutableStateOf(prefs.getCompletedRoutineIds(selectedDate)) }
    var showAddDialog by remember { mutableStateOf(false) }
    var taskPendingDelete by remember { mutableStateOf<RoutineTask?>(null) }

    var draftSummary by remember { mutableStateOf(prefs.getDaySummary(selectedDate)) }
    var editingSummary by remember { mutableStateOf(false) }

    fun switchDate(newDate: String) {
        selectedDate = newDate
        completedIds = prefs.getCompletedRoutineIds(newDate)
        draftSummary = prefs.getDaySummary(newDate)
        editingSummary = false
    }

    val isToday = selectedDate == todayKey
    val allDone = tasks.isNotEmpty() && tasks.all { completedIds.contains(it.id) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.padding(end = 4.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                }
                Column {
                    Text(
                        greetingFor(prefs.userName),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        displayDate(selectedDate),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            WeekStrip(
                todayKey = todayKey,
                selectedDate = selectedDate,
                onSelect = { switchDate(it) },
                modifier = Modifier.padding(top = 16.dp)
            )

            if (tasks.isNotEmpty()) {
                val done = tasks.count { completedIds.contains(it.id) }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Выполнено $done из ${tasks.size}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        LinearProgressIndicator(
                            progress = (if (tasks.isEmpty()) 0f else done.toFloat() / tasks.size).coerceIn(0f, 1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surface
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            // One section per ritual period. The colour index stays global so the alternating
            // rhythm doesn't restart in every section.
            RITUAL_PERIODS.forEach { period ->
                val periodTasks = tasks.filter { it.period == period }
                if (periodTasks.isEmpty()) return@forEach

                val periodMinutes = periodTasks.sumOf { it.durationMinutes }
                val periodDone = periodTasks.count { completedIds.contains(it.id) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        ritualPeriodEmoji(period) + "  " + ritualPeriodTitle(period),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "$periodDone/${periodTasks.size}" +
                            if (periodMinutes > 0) " · $periodMinutes мин" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                periodTasks.forEach { task ->
                    RoutineRow(
                        task = task,
                        index = tasks.indexOfFirst { it.id == task.id },
                        isDone = completedIds.contains(task.id),
                        streak = prefs.routineStreak(task.id, todayKey, previousDates),
                        interactive = isToday,
                        onToggle = {
                            val newDone = !completedIds.contains(task.id)
                            prefs.setRoutineTaskDone(selectedDate, task.id, newDone)
                            completedIds = prefs.getCompletedRoutineIds(selectedDate)
                        },
                        onLongPress = { taskPendingDelete = task }
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clickable { showAddDialog = true }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "+ Добавить задание",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (allDone) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "🎉 Рутина выполнена — итог дня",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        val savedSummary = prefs.getDaySummary(selectedDate)
                        if (!editingSummary && savedSummary.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    savedSummary,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    draftSummary = savedSummary
                                    editingSummary = true
                                }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Изменить итог",
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = draftSummary,
                                onValueChange = { draftSummary = it },
                                label = { Text("Как прошёл день?") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                if (savedSummary.isNotBlank()) {
                                    TextButton(onClick = { editingSummary = false }) {
                                        Text("Отмена")
                                    }
                                }
                                Button(onClick = {
                                    prefs.setDaySummary(selectedDate, draftSummary)
                                    editingSummary = false
                                }) {
                                    Text("Сохранить итог")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddRoutineTaskDialog(
            existingIds = tasks.map { it.id }.toSet(),
            onDismiss = { showAddDialog = false },
            onAdd = { task ->
                tasks = tasks + task
                prefs.routineTasks = tasks
                showAddDialog = false
            }
        )
    }

    taskPendingDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskPendingDelete = null },
            title = { Text("Удалить задание?") },
            text = { Text("«${task.title}» будет убрано из рутины. История выполнения сохранится.") },
            confirmButton = {
                TextButton(onClick = {
                    tasks = tasks.filter { it.id != task.id }
                    prefs.routineTasks = tasks
                    taskPendingDelete = null
                }) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { taskPendingDelete = null }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun WeekStrip(
    todayKey: String,
    selectedDate: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val monday = remember { mondayOfWeek(Calendar.getInstance()) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (i in 0 until 7) {
            val cal = monday.clone() as Calendar
            cal.add(Calendar.DAY_OF_MONTH, i)
            val key = dateKeyOf(cal)
            val isSelected = key == selectedDate
            val isToday = key == todayKey
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(44.dp)
            ) {
                Text(
                    weekdayShort[i],
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = CircleShape,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isToday -> MaterialTheme.colorScheme.primaryContainer
                        else -> Color.Transparent
                    },
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(36.dp)
                        .clickable { onSelect(key) }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            cal.get(Calendar.DAY_OF_MONTH).toString(),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun RoutineRow(
    task: RoutineTask,
    index: Int,
    isDone: Boolean,
    streak: Int,
    interactive: Boolean,
    onToggle: () -> Unit,
    onLongPress: () -> Unit
) {
    val containerColor = when (index % 3) {
        0 -> MaterialTheme.colorScheme.primaryContainer
        1 -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { if (interactive) onToggle() },
                onLongClick = onLongPress
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = CircleShape, color = containerColor, modifier = Modifier.size(44.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(task.icon, fontSize = 20.sp)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                task.title,
                fontWeight = FontWeight.Medium,
                textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
            )
            Row {
                if (streak > 0) {
                    Text(
                        "🔥 $streak дн. подряд",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (task.durationMinutes > 0) {
                    Text(
                        if (streak > 0) " · ${task.durationMinutes} мин" else "${task.durationMinutes} мин",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Surface(
            shape = CircleShape,
            color = if (isDone) MaterialTheme.colorScheme.primary else Color.Transparent,
            border = if (isDone) null else BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .size(32.dp)
                .clickable(enabled = interactive) { onToggle() }
        ) {
            if (isDone) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Готово",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddRoutineTaskDialog(
    existingIds: Set<String>,
    onDismiss: () -> Unit,
    onAdd: (RoutineTask) -> Unit
) {
    var mode by remember { mutableStateOf(0) } // 0 = library, 1 = custom
    var customTitle by remember { mutableStateOf("") }
    var customDuration by remember { mutableStateOf("") }
    val available = remember { ROUTINE_TASK_LIBRARY.filter { it.id !in existingIds } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новое задание") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModeChip("Готовые", mode == 0) { mode = 0 }
                    ModeChip("Своё", mode == 1) { mode = 1 }
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (mode == 0) {
                    if (available.isEmpty()) {
                        Text(
                            "Все готовые задания уже добавлены",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Column {
                            available.forEach { task ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onAdd(task) }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(task.icon, fontSize = 18.sp)
                                    Text(
                                        task.title,
                                        modifier = Modifier.padding(start = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = customTitle,
                        onValueChange = { customTitle = it },
                        label = { Text("Название задания") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customDuration,
                        onValueChange = { customDuration = it },
                        label = { Text("Минут (необязательно)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            if (mode == 1) {
                TextButton(onClick = {
                    if (customTitle.isNotBlank()) {
                        onAdd(
                            RoutineTask(
                                id = "routine_custom_${System.currentTimeMillis()}",
                                title = customTitle.trim(),
                                icon = "✅",
                                durationMinutes = customDuration.toIntOrNull() ?: 0
                            )
                        )
                    }
                }) { Text("Добавить") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        }
    )
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .height(32.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
            Text(
                label,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

private fun displayDate(dateKey: String): String {
    return try {
        val date = dateKeyFormat.parse(dateKey) ?: return dateKey
        val formatter = SimpleDateFormat("EEEE, d MMMM", Locale("ru"))
        formatter.format(date).replaceFirstChar { it.uppercase() }
    } catch (_: Exception) {
        dateKey
    }
}
