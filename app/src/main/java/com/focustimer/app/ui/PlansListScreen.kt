package com.focustimer.app.ui

import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.focustimer.app.MAX_PLAN_TEMPLATES
import com.focustimer.app.PlanHistoryEntry
import com.focustimer.app.PlanTask
import com.focustimer.app.PlanTemplate
import com.focustimer.app.PrefsManager
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Step three: every saved plan, each collapsible down to its items. Creating another one is the
 * plus in the bottom corner, up to [MAX_PLAN_TEMPLATES].
 */
@Composable
fun PlansListScreen(
    onCreatePlan: () -> Unit,
    onEditPlan: (String) -> Unit,
    expandedPlanId: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }

    var plans by remember { mutableStateOf(prefs.planTemplates) }
    var expandedId by remember {
        mutableStateOf(expandedPlanId ?: prefs.activePlanTemplateId ?: plans.firstOrNull()?.id)
    }
    var planPendingDelete by remember { mutableStateOf<PlanTemplate?>(null) }
    var showLimitNotice by remember { mutableStateOf(false) }
    var runningPlanId by remember { mutableStateOf<String?>(null) }

    val atLimit = plans.size >= MAX_PLAN_TEMPLATES

    val runningPlan = plans.firstOrNull { it.id == runningPlanId }
    if (runningPlan != null) {
        PlanRunner(
            plan = runningPlan,
            onFinished = { entry ->
                prefs.addPlanHistoryEntry(entry)
                runningPlanId = null
            },
            onStop = { runningPlanId = null },
            modifier = modifier
        )
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                "Мои планы",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Text(
                "${plans.size} из $MAX_PLAN_TEMPLATES",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (plans.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Планов пока нет", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Нажмите плюс, чтобы собрать первый план на день",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            plans.forEach { plan ->
                PlanCard(
                    plan = plan,
                    isActive = plan.id == prefs.activePlanTemplateId,
                    expanded = plan.id == expandedId,
                    onToggle = { expandedId = if (expandedId == plan.id) null else plan.id },
                    onStart = {
                        prefs.activePlanTemplateId = plan.id
                        runningPlanId = plan.id
                    },
                    onEdit = { onEditPlan(plan.id) },
                    onDelete = { planPendingDelete = plan }
                )
            }

            // Keeps the last card clear of the floating button.
            Spacer(modifier = Modifier.size(88.dp))
        }

        FloatingActionButton(
            onClick = { if (atLimit) showLimitNotice = true else onCreatePlan() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Создать план")
        }
    }

    if (showLimitNotice) {
        AlertDialog(
            onDismissRequest = { showLimitNotice = false },
            title = { Text("Достигнут предел") },
            text = { Text("Пока можно держать не больше $MAX_PLAN_TEMPLATES планов. Удалите ненужный, чтобы создать новый.") },
            confirmButton = {
                TextButton(onClick = { showLimitNotice = false }) { Text("Понятно") }
            }
        )
    }

    planPendingDelete?.let { plan ->
        AlertDialog(
            onDismissRequest = { planPendingDelete = null },
            title = { Text("Удалить план?") },
            text = { Text("«${plan.name}» будет удалён вместе со списком занятий.") },
            confirmButton = {
                TextButton(onClick = {
                    val updated = plans.filterNot { it.id == plan.id }
                    prefs.planTemplates = updated
                    if (prefs.activePlanTemplateId == plan.id) {
                        prefs.activePlanTemplateId = updated.firstOrNull()?.id
                    }
                    plans = updated
                    if (expandedId == plan.id) expandedId = updated.firstOrNull()?.id
                    planPendingDelete = null
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { planPendingDelete = null }) { Text("Отмена") }
            }
        )
    }
}

/** One plan: a header that is always visible, and its items revealed on tap. */
@Composable
private fun PlanCard(
    plan: PlanTemplate,
    isActive: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val totalMinutes = plan.tasks.sumOf { it.durationMinutes }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 14.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            plan.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (isActive) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Текущий план",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .size(16.dp)
                            )
                        }
                    }
                    val firstTime = plan.tasks.firstOrNull { it.timeOfDay.isNotBlank() }?.timeOfDay
                    val lastTime = plan.tasks.lastOrNull { it.timeOfDay.isNotBlank() }?.timeOfDay
                    Text(
                        buildString {
                            append("${plan.tasks.size} ${itemsWordList(plan.tasks.size)}")
                            append(" · ${formatTotalList(totalMinutes)}")
                            if (firstTime != null && lastTime != null) append(" · $firstTime–$lastTime")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Свернуть" else "Развернуть"
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    plan.tasks.forEach { task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                task.timeOfDay.ifBlank { "--:--" },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(52.dp)
                            )
                            Text(task.title, modifier = Modifier.weight(1f))
                            Text(
                                "${task.durationMinutes} мин",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (plan.comment.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Text(
                                plan.comment,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(onClick = onStart, modifier = Modifier.weight(1f)) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Приступить", modifier = Modifier.padding(start = 6.dp))
                        }
                        IconButton(onClick = onEdit, modifier = Modifier.padding(start = 4.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Править план")
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить план")
                        }
                    }
                }
            }
        }
    }
}

/** Runs a plan item by item, each for its own duration, and records the run when it ends. */
@Composable
private fun PlanRunner(
    plan: PlanTemplate,
    onFinished: (PlanHistoryEntry) -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }

    var index by remember(plan.id) { mutableStateOf(0) }
    var secondsLeft by remember(plan.id) { mutableStateOf(plan.tasks.firstOrNull()?.durationMinutes?.times(60) ?: 0) }
    var paused by remember(plan.id) { mutableStateOf(false) }

    fun advance() {
        if (index < plan.tasks.lastIndex) {
            index += 1
            secondsLeft = plan.tasks[index].durationMinutes * 60
        } else {
            onFinished(
                PlanHistoryEntry(
                    id = System.currentTimeMillis(),
                    templateName = plan.name,
                    completedAtMillis = System.currentTimeMillis(),
                    taskCount = plan.tasks.size,
                    totalMinutes = plan.tasks.sumOf { it.durationMinutes }
                )
            )
        }
    }

    // A heads-up while the current item is still running beats one that arrives with it.
    val speak = rememberPlanSpeaker(
        if (prefs.voiceLanguage == "English") Locale.US else Locale("ru")
    )

    LaunchedEffect(index, paused, plan.id) {
        if (paused) return@LaunchedEffect
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
            if (secondsLeft == ANNOUNCE_LEAD_SECONDS) {
                plan.tasks.getOrNull(index + 1)?.let { next ->
                    speak("Через 15 секунд: " + next.title)
                }
            }
        }
        advance()
    }

    val current = plan.tasks.getOrNull(index) ?: return

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            plan.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Шаг ${index + 1} из ${plan.tasks.size}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )

        Text(
            current.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 24.dp)
        )
        if (current.timeOfDay.isNotBlank()) {
            Text(
                "по плану в ${current.timeOfDay}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            "%02d:%02d".format(secondsLeft / 60, secondsLeft % 60),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 20.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { paused = !paused }) {
                Text(if (paused) "Продолжить" else "Пауза")
            }
            OutlinedButton(onClick = { advance() }) { Text("Дальше") }
        }
        OutlinedButton(onClick = onStop, modifier = Modifier.padding(top = 12.dp)) {
            Text("Остановить")
        }

        val remaining = plan.tasks.drop(index + 1)
        if (remaining.isNotEmpty()) {
            Text(
                "Дальше в плане",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 28.dp, bottom = 4.dp)
            )
            remaining.forEach { task -> UpcomingRow(task) }
        }
    }
}

@Composable
private fun UpcomingRow(task: PlanTask) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            task.timeOfDay.ifBlank { "--:--" },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(52.dp)
        )
        Text(task.title, modifier = Modifier.weight(1f))
        Text(
            "${task.durationMinutes} мин",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun itemsWordList(n: Int): String {
    val mod100 = n % 100
    val mod10 = n % 10
    return when {
        mod100 in 11..14 -> "занятий"
        mod10 == 1 -> "занятие"
        mod10 in 2..4 -> "занятия"
        else -> "занятий"
    }
}

private fun formatTotalList(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "$hours ч $minutes мин" else "$minutes мин"
}

/** How long before an item starts its name is announced. */
private const val ANNOUNCE_LEAD_SECONDS = 15

/**
 * A speech engine tied to the composable's lifetime. Returns a function that says one line, or
 * does nothing when the device has no usable engine — the countdown must not depend on speech.
 */
@Composable
private fun rememberPlanSpeaker(locale: Locale): (String) -> Unit {
    val context = LocalContext.current
    val ready = remember { mutableStateOf(false) }
    val engine = remember {
        var created: TextToSpeech? = null
        created = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                created?.setLanguage(locale)
                ready.value = true
            }
        }
        created
    }

    DisposableEffect(engine) {
        onDispose {
            engine?.stop()
            engine?.shutdown()
        }
    }

    return remember(engine) {
        { text: String ->
            if (ready.value) {
                engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "plan_runner_announce")
            }
        }
    }
}
