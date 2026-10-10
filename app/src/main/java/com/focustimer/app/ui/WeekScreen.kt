package com.focustimer.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.focustimer.app.MAX_PLAN_ITEMS
import com.focustimer.app.PlanItem
import com.focustimer.app.PrefsManager

/**
 * The week as seven lists. An entry belongs to one or more weekdays, so the same breakfast is one
 * entry shown on every day rather than seven copies.
 */
@Composable
fun WeekScreen(onBack: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }

    var items by remember { mutableStateOf(prefs.planItems) }
    var editing by remember { mutableStateOf<PlanItem?>(null) }
    var addingForDay by remember { mutableStateOf<Int?>(null) }
    val today = remember { todayWeekday() }

    fun persist(next: List<PlanItem>) {
        prefs.planItems = next
        items = prefs.planItems
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 32.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) IconButton(onClick = onBack, modifier = Modifier.padding(end = 4.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
            }
            Text(
                "График по дням",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            "У каждого дня недели свой список дел. Пустой день заполняется в один тап — " +
                "и повторяется каждую неделю.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
        )

        WEEKDAY_NAMES.forEachIndexed { index, name ->
            val day = index + 1
            val dayItems = items.filter { it.onDay(day) }.sortedBy { it.time }
            val total = dayItems.sumOf { it.minutes }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                if (day == today) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        Text(
                            "сегодня",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f))
                Text(
                    if (dayItems.isEmpty()) "ничего не запланировано"
                    else "${dayItems.size} ${itemsWord(dayItems.size)} · ${formatSpan(total)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (dayItems.isEmpty()) {
                OutlinedButton(
                    onClick = { addingForDay = day },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Создать список дел на день", modifier = Modifier.padding(start = 6.dp))
                }
            } else {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    dayItems.forEach { item ->
                        WeekRow(
                            item = item,
                            everyDay = item.days.size == 7,
                            onClick = { editing = item }
                        )
                    }
                    OutlinedButton(
                        onClick = { addingForDay = day },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Добавить в $name", modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }

            if (index < WEEKDAY_NAMES.lastIndex) {
                Divider(modifier = Modifier.padding(vertical = 18.dp))
            }
        }
    }

    addingForDay?.let { day ->
        PlanItemDialog(
            existing = null,
            defaultTime = minutesToClock(
                items.filter { it.onDay(day) }
                    .maxOfOrNull { parseMinutes(it.time) + it.minutes } ?: (9 * 60)
            ),
            // A day's own button means that day, not the whole week.
            defaultDays = setOf(day),
            onDismiss = { addingForDay = null },
            onConfirm = { item ->
                if (items.size < MAX_PLAN_ITEMS) persist(items + item)
                addingForDay = null
            }
        )
    }

    editing?.let { item ->
        PlanItemDialog(
            existing = item,
            defaultTime = item.time,
            onDismiss = { editing = null },
            onConfirm = { updated ->
                persist(items.map { if (it.id == updated.id) updated else it })
                editing = null
            },
            onDelete = {
                persist(items.filterNot { it.id == item.id })
                editing = null
            }
        )
    }
}

@Composable
private fun WeekRow(item: PlanItem, everyDay: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                item.time,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(46.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    buildString {
                        append("${item.minutes} мин")
                        // Worth saying, because editing it here changes every other day too.
                        if (everyDay) append(" · каждый день")
                        if (item.comment.isNotBlank()) append(" · ${item.comment}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
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
