package com.focustimer.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.focustimer.app.PLAN_TASK_LIBRARY
import com.focustimer.app.PlanItem
import java.util.Calendar

/** Entries offered as one-tap fills in the add dialog, so the quick path is not another long list. */
private val QUICK_FILL_TITLES = listOf(
    "Завтрак", "Зарядка", "Глубокая работа", "Созвоны", "Обед",
    "Прогулка после обеда", "Рабочий блок", "Учёба и развитие", "Тренажёрный зал", "Ужин", "Чтение"
)

/**
 * The day's schedule as a list. Used both on the timer screen and during setup, so it takes every
 * action as a callback and owns no state of its own.
 */
@Composable
fun DaySchedule(
    items: List<PlanItem>,
    doneIds: Set<String>,
    runningId: String?,
    nowMinutes: Int,
    onSelect: (PlanItem) -> Unit,
    onToggleDone: (PlanItem) -> Unit,
    onEdit: (PlanItem) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    showProgress: Boolean = true
) {
    val totalMinutes = items.sumOf { it.minutes }
    val doneMinutes = items.filter { doneIds.contains(it.id) }.sumOf { it.minutes }
    val nextId = items.firstOrNull { !doneIds.contains(it.id) }?.id

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text("План на сегодня", style = MaterialTheme.typography.titleMedium)
            Text(
                "${doneIds.count { id -> items.any { it.id == id } }}/${items.size} · " +
                    "${formatSpan(doneMinutes)} из ${formatSpan(totalMinutes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (showProgress && items.isNotEmpty()) {
            val fraction = doneIds.count { id -> items.any { it.id == id } }.toFloat() / items.size
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 10.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .height(6.dp)
                        .background(MaterialTheme.colorScheme.tertiary)
                )
            }
        }

        if (items.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("План на день пуст", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Добавьте первый пункт — он появится здесь и запустится в таймере.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            items.forEach { item ->
                PlanItemRow(
                    item = item,
                    isDone = doneIds.contains(item.id),
                    isRunning = runningId == item.id,
                    isNext = nextId == item.id && runningId != item.id,
                    isLate = !doneIds.contains(item.id) && runningId != item.id &&
                        parseMinutes(item.time) + item.minutes < nowMinutes,
                    onSelect = { onSelect(item) },
                    onToggleDone = { onToggleDone(item) },
                    onEdit = { onEdit(item) }
                )
            }
        }

        OutlinedButton(
            onClick = onAdd,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("Добавить занятие", modifier = Modifier.padding(start = 6.dp))
        }
    }
}

@Composable
private fun PlanItemRow(
    item: PlanItem,
    isDone: Boolean,
    isRunning: Boolean,
    isNext: Boolean,
    isLate: Boolean,
    onSelect: () -> Unit,
    onToggleDone: () -> Unit,
    onEdit: () -> Unit
) {
    // A bordered card: white while the entry waits, tinted blue while it is in the timer, greyed
    // once it is done.
    val background = when {
        isRunning -> MaterialTheme.colorScheme.tertiaryContainer
        isDone -> MaterialTheme.colorScheme.surfaceContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val cardBorder = when {
        isRunning -> BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
        isDone -> null
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = background,
        border = cardBorder,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
        ) {
            // A circle rather than a checkbox: the title is the tap target for the timer, and this
            // one tap must not be mistaken for it. The ring is what keeps an empty circle visible
            // on a white card, and the area around it is tappable so the target is not 24 dp.
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggleDone),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .then(
                            if (isDone) {
                                Modifier.background(MaterialTheme.colorScheme.primary)
                            } else {
                                Modifier
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(
                                        2.dp,
                                        if (isRunning) MaterialTheme.colorScheme.tertiary
                                        else MaterialTheme.colorScheme.outline,
                                        CircleShape
                                    )
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Снять отметку",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Text(
                item.time,
                style = MaterialTheme.typography.labelLarge,
                color = when {
                    isDone -> MaterialTheme.colorScheme.onSurfaceVariant
                    isRunning -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier
                    .padding(start = 4.dp)
                    .width(46.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
                    .clickable(onClick = onSelect)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface
                    )
                    if (isNext) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Text(
                                "дальше",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    buildString {
                        append("${item.minutes} мин")
                        if (item.comment.isNotBlank()) append(" · ${item.comment}")
                        if (isLate) append(" · просрочено")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Изменить",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Add or edit one entry. [existing] null means a new one, and only then are the quick fills
 * offered — they would overwrite what the user already typed otherwise.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanItemDialog(
    existing: PlanItem?,
    defaultTime: String,
    onDismiss: () -> Unit,
    onConfirm: (PlanItem) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var time by remember { mutableStateOf(existing?.time ?: defaultTime) }
    var minutes by remember { mutableStateOf((existing?.minutes ?: 30).toString()) }
    var comment by remember { mutableStateOf(existing?.comment ?: "") }
    var showTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Новое занятие" else "Изменить занятие") },
        text = {
            Column {
                if (existing == null) {
                    Text(
                        "Быстрый выбор",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 10.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        QUICK_FILL_TITLES.forEach { name ->
                            val lib = PLAN_TASK_LIBRARY.firstOrNull { it.title == name }
                            if (lib != null) {
                                // The chip that filled the fields stays black, so it is clear
                                // where the values below came from.
                                val picked = title == lib.title
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (picked) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .clickable {
                                            title = lib.title
                                            time = lib.timeOfDay
                                            minutes = lib.durationMinutes.toString()
                                        }
                                ) {
                                    Text(
                                        lib.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (picked) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Время: $time")
                    }
                    OutlinedTextField(
                        value = minutes,
                        onValueChange = { minutes = it.filter { ch -> ch.isDigit() }.take(3) },
                        label = { Text("мин") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .width(96.dp)
                    )
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Комментарий") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = title.trim()
                    if (clean.isEmpty()) return@Button
                    onConfirm(
                        PlanItem(
                            id = existing?.id ?: "planitem_${System.currentTimeMillis()}",
                            title = clean,
                            time = time,
                            minutes = minutes.toIntOrNull()?.coerceAtLeast(1) ?: 30,
                            comment = comment.trim()
                        )
                    )
                }
            ) { Text("Сохранить") }
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

    if (showTimePicker) {
        val (hour, minute) = parseTime(time)
        val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    time = "%02d:%02d".format(state.hour, state.minute)
                    showTimePicker = false
                }) { Text("ОК") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Отмена") }
            },
            text = { TimePicker(state = state) }
        )
    }
}

/** "HH:MM" as minutes since midnight. */
fun parseMinutes(text: String): Int {
    val (hour, minute) = parseTime(text)
    return hour * 60 + minute
}

fun minutesToClock(total: Int): String {
    val clamped = total.coerceIn(0, 23 * 60 + 59)
    return "%02d:%02d".format(clamped / 60, clamped % 60)
}

fun formatSpan(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        if (minutes > 0) "$hours ч $minutes мин" else "$hours ч"
    } else {
        "$minutes мин"
    }
}

/** Today as "yyyy-MM-dd", the key completions are stored under. */
fun dayStamp(millis: Long = System.currentTimeMillis()): String {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = millis
    return "%04d-%02d-%02d".format(
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH)
    )
}

fun nowMinutesOfDay(): Int {
    val calendar = Calendar.getInstance()
    return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
}
