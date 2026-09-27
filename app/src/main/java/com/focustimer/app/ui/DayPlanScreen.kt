package com.focustimer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focustimer.app.PLAN_TASK_LIBRARY
import com.focustimer.app.PlanHistoryEntry
import com.focustimer.app.PlanTask
import com.focustimer.app.PlanTemplate
import com.focustimer.app.PrefsManager
import com.focustimer.app.ui.theme.WorkColor
import kotlinx.coroutines.delay

@Composable
fun DayPlanScreen(
    onBack: () -> Unit,
    isSetupFlow: Boolean = false,
    onSetupComplete: (() -> Unit)? = null,
    embedded: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }

    var templates by remember { mutableStateOf(prefs.planTemplates) }
    var activeTemplateId by remember { mutableStateOf(prefs.activePlanTemplateId ?: templates.firstOrNull()?.id) }
    var isBuilding by remember { mutableStateOf(templates.isEmpty()) }
    var editingExistingId by remember { mutableStateOf<String?>(null) }
    var draftTasks by remember { mutableStateOf<List<PlanTask>>(emptyList()) }
    var draftName by remember { mutableStateOf("") }
    var customTitle by remember { mutableStateOf("") }
    var customDuration by remember { mutableStateOf("10") }
    var templatePendingDelete by remember { mutableStateOf<PlanTemplate?>(null) }

    var runningIndex by remember { mutableStateOf(-1) }
    var secondsLeft by remember { mutableStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var runCompleted by remember { mutableStateOf(false) }

    val activeTemplate = templates.find { it.id == activeTemplateId }
    val activeTasks = activeTemplate?.tasks ?: emptyList()

    fun persistTemplates(updated: List<PlanTemplate>) {
        templates = updated
        prefs.planTemplates = updated
    }

    fun stopRun() {
        runningIndex = -1
        isPaused = false
        runCompleted = false
    }

    fun selectTemplate(id: String) {
        activeTemplateId = id
        prefs.activePlanTemplateId = id
        stopRun()
    }

    fun startBuildNew() {
        editingExistingId = null
        draftTasks = emptyList()
        draftName = ""
        isBuilding = true
    }

    fun startEditExisting(template: PlanTemplate) {
        editingExistingId = template.id
        draftTasks = template.tasks
        draftName = template.name
        isBuilding = true
    }

    fun saveDraft() {
        val name = draftName.trim().ifBlank { "Список ${templates.size + 1}" }
        val id = editingExistingId ?: "plan_${System.currentTimeMillis()}"
        val newTemplate = PlanTemplate(id = id, name = name, tasks = draftTasks)
        val updated = if (editingExistingId != null) {
            templates.map { if (it.id == id) newTemplate else it }
        } else {
            templates + newTemplate
        }
        persistTemplates(updated)
        selectTemplate(id)
        isBuilding = false
    }

    fun startRun() {
        if (activeTasks.isEmpty()) return
        runningIndex = 0
        secondsLeft = activeTasks[0].durationMinutes * 60
        isPaused = false
        runCompleted = false
    }

    fun skipToNext() {
        if (runningIndex < activeTasks.lastIndex) {
            runningIndex += 1
            secondsLeft = activeTasks[runningIndex].durationMinutes * 60
        } else {
            runningIndex = -1
            runCompleted = true
            activeTemplate?.let { template ->
                prefs.addPlanHistoryEntry(
                    PlanHistoryEntry(
                        id = System.currentTimeMillis(),
                        templateName = template.name,
                        completedAtMillis = System.currentTimeMillis(),
                        taskCount = template.tasks.size,
                        totalMinutes = template.tasks.sumOf { it.durationMinutes }
                    )
                )
            }
        }
    }

    LaunchedEffect(runningIndex, isPaused, activeTemplateId) {
        if (runningIndex in activeTasks.indices && !isPaused) {
            while (secondsLeft > 0) {
                delay(1000)
                secondsLeft -= 1
            }
            skipToNext()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        if (isSetupFlow) {
            StepperHeader(
                currentStep = if (templates.isEmpty() || isBuilding) 2 else 3,
                labels = listOf("О себе", "План на день", "Готово"),
                modifier = Modifier.padding(bottom = 20.dp)
            )
            HeroGlyph(emoji = if (templates.isEmpty() || isBuilding) "🗓️" else "✨", size = 72.dp)
        }

        if (!embedded) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = if (isSetupFlow) 16.dp else 0.dp)
            ) {
                if (!isSetupFlow) {
                    IconButton(onClick = onBack, modifier = Modifier.padding(end = 4.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
                Text(
                    "План на день",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        if (isSetupFlow) {
            Text(
                "Соберите список действий на утро или на день — с таймером на каждое, которые будут запускаться одно за другим",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (!isBuilding && templates.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                templates.forEach { template ->
                    TemplateChip(
                        label = template.name,
                        selected = template.id == activeTemplateId,
                        onClick = { selectTemplate(template.id) }
                    )
                }
                TemplateChip(label = "+ Новый список", selected = false, onClick = { startBuildNew() })
            }
        }

        if (isBuilding) {
            PlanBuilder(
                draftTasks = draftTasks,
                draftName = draftName,
                customTitle = customTitle,
                customDuration = customDuration,
                professionName = prefs.profession,
                professionSummary = presetForProfession(prefs.profession).summary,
                planHints = planItemsFor(prefs.profession, prefs.professionDetails)
                    .mapNotNull { it.hint }
                    .distinctBy { it.text },
                onGenerateFromProfession = {
                    val stamp = System.currentTimeMillis()
                    draftTasks = planItemsFor(prefs.profession, prefs.professionDetails)
                        .mapIndexed { index, item ->
                            PlanTask(
                                id = "plantask_prof_${stamp}_$index",
                                title = item.title,
                                durationMinutes = item.durationMinutes
                            )
                        }
                    if (draftName.isBlank()) {
                        draftName = "День · " + presetForProfession(prefs.profession).name
                    }
                },
                onNameChange = { draftName = it },
                onCustomTitleChange = { customTitle = it },
                onCustomDurationChange = { customDuration = it },
                onAddFromLibrary = { task -> draftTasks = draftTasks + task },
                onAddCustom = {
                    if (customTitle.isNotBlank()) {
                        val minutes = customDuration.toIntOrNull()?.coerceIn(1, 240) ?: 10
                        draftTasks = draftTasks + PlanTask(
                            id = "plantask_custom_${System.currentTimeMillis()}",
                            title = customTitle.trim(),
                            durationMinutes = minutes
                        )
                        customTitle = ""
                        customDuration = "10"
                    }
                },
                onRemoveTask = { task -> draftTasks = draftTasks.filter { it.id != task.id } },
                onUpdateDuration = { task, minutes ->
                    draftTasks = draftTasks.map { if (it.id == task.id) it.copy(durationMinutes = minutes) else it }
                },
                onSave = ::saveDraft,
                onCancel = { isBuilding = false }.takeIf { templates.isNotEmpty() }
            )
        } else if (activeTemplate == null) {
            Text(
                "У вас пока нет списков дел. Создайте первый — из готовых действий или своих.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 24.dp)
            )
            Button(onClick = { startBuildNew() }, modifier = Modifier.padding(top = 16.dp)) {
                Text("Создать список")
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(activeTemplate.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row {
                    IconButton(onClick = { startEditExisting(activeTemplate) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Изменить список")
                    }
                    IconButton(onClick = { templatePendingDelete = activeTemplate }) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить список")
                    }
                }
            }

            Column(modifier = Modifier.padding(top = 8.dp)) {
                activeTasks.forEachIndexed { index, task ->
                    val isCurrent = index == runningIndex
                    PlanTaskRow(
                        task = task,
                        isCurrent = isCurrent,
                        isDone = runCompleted || runningIndex > index,
                        secondsLeft = if (isCurrent) secondsLeft else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            when {
                runCompleted -> {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "🎉 Все действия выполнены!",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Button(onClick = { startRun() }, modifier = Modifier.padding(top = 12.dp)) {
                                Text("Начать заново")
                            }
                        }
                    }
                }
                runningIndex >= 0 -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isPaused = !isPaused },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null)
                            Text(if (isPaused) " Продолжить" else " Пауза")
                        }
                        OutlinedButton(onClick = { skipToNext() }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.SkipNext, contentDescription = null)
                            Text(" Дальше")
                        }
                        OutlinedButton(onClick = { stopRun() }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Text(" Стоп")
                        }
                    }
                }
                else -> {
                    Button(
                        onClick = { startRun() },
                        enabled = activeTasks.isNotEmpty(),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text("Начать выполнение", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        if (isSetupFlow) {
            Button(
                onClick = { onSetupComplete?.invoke() },
                enabled = templates.isNotEmpty() && !isBuilding,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(top = 32.dp)
            ) {
                Text("Готово, начать пользоваться", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    templatePendingDelete?.let { template ->
        AlertDialog(
            onDismissRequest = { templatePendingDelete = null },
            title = { Text("Удалить список?") },
            text = { Text("«${template.name}» будет удалён без возможности восстановления.") },
            confirmButton = {
                TextButton(onClick = {
                    val updated = templates.filter { it.id != template.id }
                    persistTemplates(updated)
                    if (activeTemplateId == template.id) {
                        activeTemplateId = updated.firstOrNull()?.id
                        prefs.activePlanTemplateId = activeTemplateId
                        stopRun()
                    }
                    templatePendingDelete = null
                }) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { templatePendingDelete = null }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun PlanBuilder(
    draftTasks: List<PlanTask>,
    draftName: String,
    customTitle: String,
    customDuration: String,
    professionName: String,
    professionSummary: String,
    planHints: List<PlanHint>,
    onGenerateFromProfession: () -> Unit,
    onNameChange: (String) -> Unit,
    onCustomTitleChange: (String) -> Unit,
    onCustomDurationChange: (String) -> Unit,
    onAddFromLibrary: (PlanTask) -> Unit,
    onAddCustom: () -> Unit,
    onRemoveTask: (PlanTask) -> Unit,
    onUpdateDuration: (PlanTask, Int) -> Unit,
    onSave: () -> Unit,
    onCancel: (() -> Unit)?
) {
    val addedIds = draftTasks.map { it.id }.toSet()
    val available = PLAN_TASK_LIBRARY.filter { it.id !in addedIds }
    val uriHandler = LocalUriHandler.current

    Column(modifier = Modifier.padding(top = 16.dp)) {
        // Profession-based generation sits above the manual tools: it fills the draft in one tap,
        // and everything below still works for editing it or building from scratch.
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "План под вашу специальность",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    if (professionName.isBlank()) "Специальность не выбрана" else professionName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(top = 4.dp)
                )
                if (professionSummary.isNotBlank()) {
                    Text(
                        professionSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Button(
                    onClick = onGenerateFromProfession,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Text(if (draftTasks.isEmpty()) "Сгенерировать план" else "Сгенерировать заново")
                }
            }
        }

        if (planHints.isNotEmpty()) {
            Text(
                "Почему так",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
            planHints.forEach { hint ->
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(hint.text, style = MaterialTheme.typography.bodySmall)
                    if (hint.source.isNotBlank()) {
                        Text(
                            hint.source,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .then(
                                    if (hint.url.isNotBlank()) {
                                        Modifier.clickable { uriHandler.openUri(hint.url) }
                                    } else {
                                        Modifier
                                    }
                                )
                        )
                    }
                }
            }
        }

        Text(
            "Готовые действия",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            available.forEach { task ->
                TemplateChip(label = "${task.title} · ${task.durationMinutes}м", selected = false) {
                    onAddFromLibrary(task)
                }
            }
        }

        Text(
            "Своё действие",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = customTitle,
                onValueChange = onCustomTitleChange,
                label = { Text("Действие") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = customDuration,
                onValueChange = onCustomDurationChange,
                label = { Text("Мин") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .width(80.dp)
                    .padding(start = 8.dp)
            )
            TextButton(onClick = onAddCustom, modifier = Modifier.padding(start = 4.dp)) {
                Text("+")
            }
        }

        if (draftTasks.isNotEmpty()) {
            Text(
                "Список действий",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 20.dp)
            )
            draftTasks.forEachIndexed { index, task ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${index + 1}.", modifier = Modifier.width(24.dp))
                    Text(task.title, modifier = Modifier.weight(1f))
                    OutlinedTextField(
                        value = task.durationMinutes.toString(),
                        onValueChange = { input ->
                            input.toIntOrNull()?.let { onUpdateDuration(task, it.coerceIn(1, 240)) }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(70.dp)
                    )
                    IconButton(onClick = { onRemoveTask(task) }) {
                        Icon(Icons.Default.Close, contentDescription = "Удалить ${task.title}")
                    }
                }
            }
        }

        OutlinedTextField(
            value = draftName,
            onValueChange = onNameChange,
            label = { Text("Название списка") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onCancel != null) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("Отмена")
                }
            }
            Button(
                onClick = onSave,
                enabled = draftTasks.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Сохранить список")
            }
        }
    }
}

@Composable
private fun TemplateChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier
            .height(36.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp)
        ) {
            Text(
                label,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun PlanTaskRow(task: PlanTask, isCurrent: Boolean, isDone: Boolean, secondsLeft: Int?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(
                if (isCurrent) WorkColor.copy(alpha = 0.12f) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = when {
                isCurrent -> WorkColor
                isDone -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.width(10.dp).height(10.dp)
        ) {}

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(task.title, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal)
            if (isCurrent && secondsLeft != null) {
                val minutes = secondsLeft / 60
                val seconds = secondsLeft % 60
                Text(
                    "%02d:%02d".format(minutes, seconds),
                    style = MaterialTheme.typography.bodyMedium,
                    color = WorkColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                val total = (task.durationMinutes * 60).coerceAtLeast(1)
                LinearProgressIndicator(
                    progress = (1f - secondsLeft.toFloat() / total).coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .height(6.dp),
                    color = WorkColor
                )
            } else {
                Text(
                    "${task.durationMinutes} мин",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
