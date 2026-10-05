package com.focustimer.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.focustimer.app.MAX_PLAN_ITEMS
import com.focustimer.app.PlanItem
import com.focustimer.app.PrefsManager

/**
 * Step two: the day, laid out from the profession preset so the user starts with something usable
 * rather than an empty list. Everything here is editable before the day begins.
 */
@Composable
fun ScheduleSetupScreen(onDone: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }
    val uriHandler = LocalUriHandler.current
    val preset = remember { presetForProfession(prefs.profession) }

    val generated = remember {
        val items = planItemsFor(prefs.profession, prefs.professionDetails)
        // Presets carry durations but no clock times, so lay them out back to back, starting an
        // hour after waking — the first hour belongs to getting going, not to planned work.
        var cursor = parseMinutes(prefs.wakeTime.ifBlank { "07:00" }) + 60
        val stamp = System.currentTimeMillis()
        items.mapIndexed { index, item ->
            val at = cursor
            cursor += item.durationMinutes
            PlanItem(
                id = "planitem_${stamp}_$index",
                title = item.title,
                time = minutesToClock(at),
                minutes = item.durationMinutes,
                comment = ""
            )
        }
    }
    val hints = remember {
        planItemsFor(prefs.profession, prefs.professionDetails).filter { it.hint != null }
    }

    var items by remember { mutableStateOf(generated) }
    var showHints by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<PlanItem?>(null) }
    var adding by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        StepperHeader(
            currentStep = 2,
            labels = listOf("О себе", "Расписание", "Готово"),
            modifier = Modifier.padding(bottom = 18.dp)
        )

        Text("Ваш день", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Собрано под «${preset.name}». Время и длительность можно поменять — нажмите на пункт.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )

        if (hints.isNotEmpty()) {
            TextButton(onClick = { showHints = !showHints }, modifier = Modifier.padding(top = 6.dp)) {
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

        DaySchedule(
            items = items,
            doneIds = emptySet(),
            runningId = null,
            nowMinutes = -1,
            onSelect = { editing = it },
            onToggleDone = { },
            onEdit = { editing = it },
            onAdd = { adding = true },
            showProgress = false,
            modifier = Modifier.padding(top = 18.dp)
        )

        Button(
            onClick = {
                prefs.planItems = items.sortedBy { it.time }.take(MAX_PLAN_ITEMS)
                prefs.isOnboarded = true
                onDone()
            },
            enabled = items.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
                .heightIn(min = 52.dp)
        ) {
            Text("Начать день")
        }
        if (items.isEmpty()) {
            Text(
                "Оставьте хотя бы один пункт",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }

    if (adding) {
        PlanItemDialog(
            existing = null,
            defaultTime = minutesToClock(
                (items.maxOfOrNull { parseMinutes(it.time) + it.minutes } ?: (12 * 60))
            ),
            onDismiss = { adding = false },
            onConfirm = { item ->
                items = (items + item).sortedBy { it.time }
                adding = false
            }
        )
    }

    editing?.let { item ->
        PlanItemDialog(
            existing = item,
            defaultTime = item.time,
            onDismiss = { editing = null },
            onConfirm = { updated ->
                items = items.map { if (it.id == updated.id) updated else it }.sortedBy { it.time }
                editing = null
            },
            onDelete = {
                items = items.filterNot { it.id == item.id }
                editing = null
            }
        )
    }
}
