package com.focustimer.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.focustimer.app.PlanHistoryEntry
import com.focustimer.app.PrefsManager
import com.focustimer.app.SessionRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("История") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("План на день") }
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> HistoryTab()
                1 -> DayPlanScreen(onBack = {}, embedded = true)
            }
        }
    }
}

@Composable
private fun HistoryTab(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }
    var planHistory by remember { mutableStateOf(prefs.getPlanHistory()) }
    var sessionHistory by remember { mutableStateOf(prefs.getHistory()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            "Завершённые планы",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (planHistory.isEmpty()) {
            Text(
                "Здесь появятся планы на день, которые вы выполнили полностью",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )
        } else {
            Column(modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) {
                planHistory.forEach { entry ->
                    PlanHistoryRow(
                        entry = entry,
                        onNoteChange = { note ->
                            prefs.updatePlanHistoryNote(entry.id, note)
                            planHistory = prefs.getPlanHistory()
                        },
                        onDelete = {
                            prefs.deletePlanHistoryEntry(entry.id)
                            planHistory = prefs.getPlanHistory()
                        }
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            "Сессии таймера",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (sessionHistory.isEmpty()) {
            Text(
                "Пока пусто — здесь появится история ваших сессий работы и отдыха",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                sessionHistory.forEach { entry ->
                    SessionHistoryRow(
                        entry = entry,
                        onCommentChange = { newComment ->
                            prefs.updateHistoryComment(entry.id, newComment)
                            sessionHistory = prefs.getHistory()
                        }
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun PlanHistoryRow(entry: PlanHistoryEntry, onNoteChange: (String) -> Unit, onDelete: () -> Unit) {
    var note by remember(entry.id) { mutableStateOf(entry.note) }
    val dateText = remember(entry.completedAtMillis) {
        SimpleDateFormat("dd MMM, HH:mm", Locale("ru")).format(Date(entry.completedAtMillis))
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🎉 ${entry.templateName}", fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(dateText, style = MaterialTheme.typography.bodySmall)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Close, contentDescription = "Удалить запись")
                }
            }
        }
        Text(
            "${entry.taskCount} действий · ${entry.totalMinutes} мин",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        OutlinedTextField(
            value = note,
            onValueChange = {
                note = it
                onNoteChange(it)
            },
            label = { Text("Заметка") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
    }
}

@Composable
private fun SessionHistoryRow(entry: SessionRecord, onCommentChange: (String) -> Unit) {
    var comment by remember(entry.id) { mutableStateOf(entry.comment) }
    val phaseLabel = if (entry.phase == "WORK") "Работа" else "Отдых"
    val dateText = remember(entry.startTimeMillis) {
        SimpleDateFormat("dd MMM, HH:mm", Locale("ru")).format(Date(entry.startTimeMillis))
    }
    val minutes = entry.durationSeconds / 60
    val seconds = entry.durationSeconds % 60

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = phaseLabel + if (entry.interrupted) " (прервано)" else "",
                fontWeight = FontWeight.Bold
            )
            Text(dateText, style = MaterialTheme.typography.bodySmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("%d:%02d".format(minutes, seconds), style = MaterialTheme.typography.bodyMedium)
            if (entry.category.isNotBlank()) {
                Text(
                    "• ${entry.category}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (entry.quote.isNotBlank()) {
            Text(
                entry.quote,
                style = MaterialTheme.typography.bodySmall,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        OutlinedTextField(
            value = comment,
            onValueChange = {
                comment = it
                onCommentChange(it)
            },
            label = { Text("Комментарий") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
    }
}
