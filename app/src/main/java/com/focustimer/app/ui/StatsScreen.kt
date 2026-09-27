package com.focustimer.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.focustimer.app.PrefsManager
import com.focustimer.app.SessionRecord
import java.util.Calendar

private data class Achievement(val label: String, val current: Int, val target: Int) {
    val unlocked: Boolean get() = current >= target
}

@Composable
fun StatsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }
    val liveSteps = rememberLiveStepCount(prefs.stepsEnabled)
    val history = remember { prefs.getHistory() }
    val workEntries = remember(history) { history.filter { it.phase == "WORK" } }

    val todayStart = remember { startOfDay(System.currentTimeMillis()) }
    val weekStart = remember { todayStart - 6L * DAY_MILLIS }
    val monthStart = remember { startOfMonth(System.currentTimeMillis()) }

    val todaySeconds = remember(workEntries) {
        workEntries.filter { it.startTimeMillis >= todayStart }.sumOf { it.durationSeconds }
    }
    val weekSeconds = remember(workEntries) {
        workEntries.filter { it.startTimeMillis >= weekStart }.sumOf { it.durationSeconds }
    }
    val monthSeconds = remember(workEntries) {
        workEntries.filter { it.startTimeMillis >= monthStart }.sumOf { it.durationSeconds }
    }

    // Two productivity windows, classified by when a session started.
    val windows = remember {
        listOf(
            FocusWindow("Первая половина", prefs.focusWindowOneStart, prefs.focusWindowOneEnd),
            FocusWindow("Вторая половина", prefs.focusWindowTwoStart, prefs.focusWindowTwoEnd)
        )
    }
    val windowStats = remember(workEntries, windows) {
        windows.map { window ->
            val inWindow = workEntries.filter { window.contains(minutesOfDay(it.startTimeMillis)) }
            WindowStats(
                window = window,
                today = inWindow.filter { it.startTimeMillis >= todayStart }.sumOf { it.durationSeconds },
                week = inWindow.filter { it.startTimeMillis >= weekStart }.sumOf { it.durationSeconds },
                month = inWindow.filter { it.startTimeMillis >= monthStart }.sumOf { it.durationSeconds }
            )
        }
    }
    val outsideMonth = remember(workEntries, windows) {
        workEntries
            .filter { it.startTimeMillis >= monthStart }
            .filterNot { entry -> windows.any { it.contains(minutesOfDay(entry.startTimeMillis)) } }
            .sumOf { it.durationSeconds }
    }
    val completedCount = remember(workEntries) { workEntries.count { !it.interrupted } }
    val totalCount = workEntries.size
    val streak = remember(workEntries) { computeStreak(workEntries) }
    val categoryBreakdown = remember(workEntries) {
        workEntries
            .groupBy { it.category.ifBlank { "Без категории" } }
            .mapValues { (_, entries) -> entries.sumOf { it.durationSeconds } }
            .entries
            .sortedByDescending { it.value }
    }

    val achievements = remember(completedCount, streak) {
        listOf(1, 10, 50, 100, 500).map { Achievement("$it завершённых сессий", completedCount, it) } +
            listOf(3, 7, 30, 100).map { Achievement("Стрик ${it} ${daysWord(it)}", streak, it) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        StatRow("Сегодня", formatDuration(todaySeconds))
        StatRow("За неделю", formatDuration(weekSeconds))
        StatRow("За месяц", formatDuration(monthSeconds))
        StatRow("Завершено сессий", "$completedCount из $totalCount")
        StatRow("Текущий стрик", "$streak ${daysWord(streak)}")
        if (prefs.stepsEnabled) {
            StatRow("Шаги сегодня", liveSteps?.toString() ?: "…")
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))
        Text("Окна продуктивности", style = MaterialTheme.typography.titleMedium)
        Text(
            "Сегодня · неделя · месяц",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
        )
        windowStats.forEach { stats ->
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                Text(
                    "${stats.window.label} · ${stats.window.start}–${stats.window.end}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    formatDuration(stats.today) + "  ·  " +
                        formatDuration(stats.week) + "  ·  " +
                        formatDuration(stats.month),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (outsideMonth > 0) {
            StatRow("Вне окон, за месяц", formatDuration(outsideMonth))
        }

        if (categoryBreakdown.isNotEmpty()) {
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            Text("По категориям", style = MaterialTheme.typography.titleMedium)
            categoryBreakdown.forEach { (category, seconds) ->
                StatRow(category, formatDuration(seconds))
            }
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))
        Text("Достижения", style = MaterialTheme.typography.titleMedium)
        achievements.forEach { achievement ->
            AchievementRow(achievement)
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AchievementRow(achievement: Achievement) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (achievement.unlocked) Icons.Default.CheckCircle else Icons.Default.Lock,
            contentDescription = null,
            tint = if (achievement.unlocked) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline
        )
        Column(modifier = Modifier.padding(start = 12.dp).fillMaxWidth()) {
            Text(achievement.label)
            LinearProgressIndicator(
                progress = (achievement.current.toFloat() / achievement.target).coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
    }
}

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

/** A productivity window, compared against the minute-of-day a session started at. */
private data class FocusWindow(val label: String, val start: String, val end: String) {
    private val startMinutes = parseTimeToMinutes(start)
    private val endMinutes = parseTimeToMinutes(end)

    fun contains(minuteOfDay: Int): Boolean =
        minuteOfDay >= startMinutes && minuteOfDay < endMinutes
}

private data class WindowStats(
    val window: FocusWindow,
    val today: Int,
    val week: Int,
    val month: Int
)

private fun parseTimeToMinutes(text: String): Int {
    val parts = text.split(":")
    val hours = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 0
    val minutes = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
    return hours * 60 + minutes
}

private fun minutesOfDay(millis: Long): Int {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = millis
    return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
}

private fun startOfMonth(millis: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = millis
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

private fun startOfDay(millis: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = millis
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

private fun computeStreak(workEntries: List<SessionRecord>): Int {
    val completedDays = workEntries.filter { !it.interrupted }.map { startOfDay(it.startTimeMillis) }.toSet()
    var streak = 0
    var dayCursor = startOfDay(System.currentTimeMillis())
    while (completedDays.contains(dayCursor)) {
        streak++
        dayCursor -= DAY_MILLIS
    }
    return streak
}

private fun formatDuration(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) "$hours ч $minutes мин" else "$minutes мин"
}

private fun daysWord(n: Int): String {
    val mod100 = n % 100
    val mod10 = n % 10
    return when {
        mod100 in 11..14 -> "дней"
        mod10 == 1 -> "день"
        mod10 in 2..4 -> "дня"
        else -> "дней"
    }
}
