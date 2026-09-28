package com.focustimer.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.focustimer.app.MAX_PLAN_TEMPLATES
import com.focustimer.app.PLAN_QUICK_PICK_IDS
import com.focustimer.app.PLAN_TASK_LIBRARY
import com.focustimer.app.PlanTask
import com.focustimer.app.PlanTemplate
import com.focustimer.app.PrefsManager

/**
 * Step two: build one plan. Items each carry their own time of day, so the plan is a schedule
 * rather than just an ordered list.
 *
 * @param editingPlanId an existing plan to edit, or null to start a new one.
 */
@Composable
fun PlanEditorScreen(
    onDone: (String) -> Unit,
    editingPlanId: String? = null,
    isSetupFlow: Boolean = false,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }
    val uriHandler = LocalUriHandler.current

    val existing = remember(editingPlanId) {
        editingPlanId?.let { id -> prefs.planTemplates.firstOrNull { it.id == id } }
    }

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var comment by remember { mutableStateOf(existing?.comment ?: "") }
    var items by remember { mutableStateOf(existing?.tasks ?: emptyList()) }
    var customTitle by remember { mutableStateOf("") }
    var customMinutes by remember { mutableStateOf("15") }
    var editingItem by remember { mutableStateOf<PlanTask?>(null) }
    var timePickerFor by remember { mutableStateOf<PlanTask?>(null) }
    var hints by remember { mutableStateOf<List<ProfessionPlanItem>>(emptyList()) }
    var showHints by remember { mutableStateOf(false) }

    val addedIds = items.map { it.id }.toSet()
    val preset = remember { presetForProfession(prefs.profession) }

    /** Items read as a schedule, so they are always kept in time order; untimed ones sink. */
    fun sorted(list: List<PlanTask>): List<PlanTask> =
        list.sortedBy { if (it.timeOfDay.isBlank()) "99:99" else it.timeOfDay }

    fun addLibraryItem(task: PlanTask) {
        if (task.id in addedIds) return
        items = sorted(items + task)
    }

    fun removeItem(id: String) {
        items = items.filterNot { it.id == id }
    }

    fun replaceItem(updated: PlanTask) {
        items = sorted(items.map { if (it.id == updated.id) updated else it })
    }

    /**
     * Order comes from the time of day, so moving a card means the two neighbours trade times —
     * that keeps the plan chronological instead of fighting the sort. Items sharing a time (or
     * without one) have nothing to trade, so they swap positions instead.
     */
    fun moveItem(index: Int, delta: Int) {
        val target = index + delta
        if (target < 0 || target > items.lastIndex) return
        val a = items[index]
        val b = items[target]
        items = if (a.timeOfDay.isNotBlank() && b.timeOfDay.isNotBlank() && a.timeOfDay != b.timeOfDay) {
            sorted(
                items.map {
                    when (it.id) {
                        a.id -> it.copy(timeOfDay = b.timeOfDay)
                        b.id -> it.copy(timeOfDay = a.timeOfDay)
                        else -> it
                    }
                }
            )
        } else {
            items.toMutableList().also {
                it[index] = b
                it[target] = a
            }
        }
    }

    fun generateFromProfession() {
        val generated = planItemsFor(prefs.profession, prefs.professionDetails)
        if (generated.isEmpty()) return
        // The generated list carries durations but no clock times, so lay them out back to back
        // from the user's wake time.
        var cursor = parseTime(prefs.wakeTime.ifBlank { "07:00" }).let { it.first * 60 + it.second }
        val stamp = System.currentTimeMillis()
        items = generated.mapIndexed { index, item ->
            val at = cursor.coerceAtMost(23 * 60 + 59)
            cursor += item.durationMinutes
            PlanTask(
                id = "plantask_gen_${stamp}_$index",
                title = item.title,
                durationMinutes = item.durationMinutes,
                timeOfDay = "%02d:%02d".format(at / 60, at % 60)
            )
        }
        hints = generated.filter { it.hint != null }
        showHints = hints.isNotEmpty()
        if (name.isBlank()) name = "День: ${preset.name}"
    }

    fun save() {
        val templates = prefs.planTemplates
        val isNew = existing == null
        if (isNew && templates.size >= MAX_PLAN_TEMPLATES) return
        val id = existing?.id ?: "plan_${System.currentTimeMillis()}"
        val template = PlanTemplate(
            id = id,
            name = name.trim().ifBlank { "План ${templates.size + 1}" },
            tasks = sorted(items),
            comment = comment.trim()
        )
        prefs.planTemplates =
            if (isNew) templates + template else templates.map { if (it.id == id) template else it }
        prefs.activePlanTemplateId = id
        onDone(id)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        if (isSetupFlow) {
            StepperHeader(
                currentStep = 2,
                labels = listOf("О себе", "План на день", "Готово"),
                modifier = Modifier.padding(bottom = 20.dp)
            )
            HeroGlyph(emoji = "🗓️", size = 72.dp)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.padding(end = 4.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                }
            }
            Text(
                if (existing == null) "Новый план" else "Правка плана",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Название плана") },
            placeholder = { Text("Например, «Обычный рабочий день»") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )

        OutlinedButton(
            onClick = { generateFromProfession() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("Собрать по профессии: ${preset.name}", modifier = Modifier.padding(start = 8.dp))
        }

        if (hints.isNotEmpty()) {
            TextButton(
                onClick = { showHints = !showHints },
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(if (showHints) "Скрыть «почему так»" else "Почему так")
            }
            if (showHints) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        hints.forEach { item ->
                            val hint = item.hint ?: return@forEach
                            Text(
                                item.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                hint.text,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                            )
                            if (hint.source.isNotBlank()) {
                                Text(
                                    hint.source,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    textDecoration = if (hint.url.isNotBlank()) TextDecoration.Underline
                                    else TextDecoration.None,
                                    modifier = Modifier
                                        .padding(bottom = 12.dp)
                                        .clickable(enabled = hint.url.isNotBlank()) {
                                            uriHandler.openUri(hint.url)
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }

        Text(
            "Быстрый выбор",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 20.dp)
        )
        QuickPickChips(
            addedIds = addedIds,
            onAdd = { addLibraryItem(it) },
            onRemove = { removeItem(it) }
        )

        val available = PLAN_TASK_LIBRARY.filterNot { it.id in addedIds }
        if (available.isNotEmpty()) {
            DropdownField(
                label = "Добавить из списка",
                selected = "Выберите занятие",
                options = available.map { "${it.timeOfDay}  ${it.title}" },
                onSelected = { label ->
                    val title = label.substringAfter("  ")
                    available.firstOrNull { it.title == title }?.let { addLibraryItem(it) }
                },
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = customTitle,
                onValueChange = { customTitle = it },
                label = { Text("Своё занятие") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = customMinutes,
                onValueChange = { customMinutes = it.filter { ch -> ch.isDigit() }.take(3) },
                label = { Text("мин") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .padding(start = 8.dp)
                    .width(84.dp)
            )
            IconButton(
                onClick = {
                    val title = customTitle.trim()
                    if (title.isEmpty()) return@IconButton
                    items = sorted(
                        items + PlanTask(
                            id = "plantask_custom_${System.currentTimeMillis()}",
                            title = title,
                            durationMinutes = customMinutes.toIntOrNull()?.coerceAtLeast(1) ?: 15,
                            timeOfDay = nextFreeTime(items)
                        )
                    )
                    customTitle = ""
                    customMinutes = "15"
                },
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить занятие")
            }
        }

        Text(
            if (items.isEmpty()) "Пока ничего не добавлено"
            else "В плане ${items.size} ${itemsWord(items.size)} · ${formatTotal(items.sumOf { it.durationMinutes })}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
        )

        items.forEachIndexed { index, item ->
            PlanItemRow(
                item = item,
                onTimeClick = { timePickerFor = item },
                onEditClick = { editingItem = item },
                onRemove = { removeItem(item.id) },
                onMoveUp = if (index > 0) ({ moveItem(index, -1) }) else null,
                onMoveDown = if (index < items.lastIndex) ({ moveItem(index, 1) }) else null
            )
        }

        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            label = { Text("Комментарий к плану") },
            placeholder = { Text("Что важно помнить об этом дне") },
            minLines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
        )

        Button(
            onClick = { save() },
            enabled = items.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
        ) {
            Text("Приступить к выполнению")
        }
        if (items.isEmpty()) {
            Text(
                "Добавьте хотя бы одно занятие",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }

    timePickerFor?.let { item ->
        TimeChooserDialog(
            initial = item.timeOfDay,
            onDismiss = { timePickerFor = null },
            onConfirm = { time ->
                replaceItem(item.copy(timeOfDay = time))
                timePickerFor = null
            }
        )
    }

    editingItem?.let { item ->
        ItemEditDialog(
            item = item,
            onDismiss = { editingItem = null },
            onConfirm = { updated ->
                replaceItem(updated)
                editingItem = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickPickChips(
    addedIds: Set<String>,
    onAdd: (PlanTask) -> Unit,
    onRemove: (String) -> Unit
) {
    val quickPicks = remember {
        PLAN_QUICK_PICK_IDS.mapNotNull { id -> PLAN_TASK_LIBRARY.firstOrNull { it.id == id } }
    }
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        quickPicks.forEach { task ->
            val added = task.id in addedIds
            FilterChip(
                selected = added,
                onClick = { if (added) onRemove(task.id) else onAdd(task) },
                label = { Text(task.title) },
                leadingIcon = if (added) {
                    {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    null
                }
            )
        }
    }
}

@Composable
private fun PlanItemRow(
    item: PlanTask,
    onTimeClick: () -> Unit,
    onEditClick: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
        ) {
            // The time is its own tap target, since changing it is the most common edit.
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.clickable(onClick = onTimeClick)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = "Изменить время",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        item.timeOfDay.ifBlank { "--:--" },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
                    .clickable(onClick = onEditClick)
            ) {
                Text(item.title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${item.durationMinutes} мин",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Disabled rather than hidden at the ends of the list, so the row never reflows.
            Column {
                IconButton(
                    onClick = { onMoveUp?.invoke() },
                    enabled = onMoveUp != null,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Выше")
                }
                IconButton(
                    onClick = { onMoveDown?.invoke() },
                    enabled = onMoveDown != null,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Ниже")
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Убрать из плана")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeChooserDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val (hour, minute) = parseTime(initial.ifBlank { "08:00" })
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm("%02d:%02d".format(state.hour, state.minute)) }) {
                Text("ОК")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        text = { TimePicker(state = state) }
    )
}

@Composable
private fun ItemEditDialog(
    item: PlanTask,
    onDismiss: () -> Unit,
    onConfirm: (PlanTask) -> Unit
) {
    var title by remember { mutableStateOf(item.title) }
    var minutes by remember { mutableStateOf(item.durationMinutes.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Занятие") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { minutes = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text("Длительность, мин") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    item.copy(
                        title = title.trim().ifBlank { item.title },
                        durationMinutes = minutes.toIntOrNull()?.coerceAtLeast(1) ?: item.durationMinutes
                    )
                )
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

/** Places a new custom item after everything already planned, rather than at a fixed hour. */
private fun nextFreeTime(items: List<PlanTask>): String {
    val last = items.filter { it.timeOfDay.isNotBlank() }.maxByOrNull { it.timeOfDay }
        ?: return "08:00"
    val (hour, minute) = parseTime(last.timeOfDay)
    val total = (hour * 60 + minute + last.durationMinutes).coerceAtMost(23 * 60 + 59)
    return "%02d:%02d".format(total / 60, total % 60)
}

private fun itemsWord(n: Int): String {
    val mod100 = n % 100
    val mod10 = n % 10
    return when {
        mod100 in 11..14 -> "занятий"
        mod10 == 1 -> "занятие"
        mod10 in 2..4 -> "занятия"
        else -> "занятий"
    }
}

private fun formatTotal(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "$hours ч $minutes мин" else "$minutes мин"
}
