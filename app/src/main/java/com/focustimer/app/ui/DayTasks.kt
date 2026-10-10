package com.focustimer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.focustimer.app.DayTask
import com.focustimer.app.MAX_DAY_TASKS

/**
 * One-off things to do today, next to the repeating schedule. They carry no time and no timer:
 * the point is a list you can empty, not another thing to run.
 */
@Composable
fun DayTasks(
    tasks: List<DayTask>,
    onToggle: (DayTask) -> Unit,
    onRemove: (DayTask) -> Unit,
    onAdd: (String) -> Unit,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true
) {
    var draft by remember { mutableStateOf("") }
    val doneCount = tasks.count { it.done }

    fun commit() {
        val clean = draft.trim()
        if (clean.isEmpty() || tasks.size >= MAX_DAY_TASKS) return
        onAdd(clean)
        draft = ""
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (showHeader) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text("Задачи на день", style = MaterialTheme.typography.titleMedium)
                if (tasks.isNotEmpty()) {
                    Text(
                        "$doneCount/${tasks.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (tasks.isEmpty()) {
            Text(
                "Что нужно успеть сегодня, без времени и таймера.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        tasks.forEach { task ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(
                                if (task.done) MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable { onToggle(task) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (task.done) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Снять отметку",
                                tint = MaterialTheme.colorScheme.onTertiary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    Text(
                        task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (task.done) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 10.dp)
                    )
                    IconButton(onClick = { onRemove(task) }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Убрать задачу",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (tasks.size < MAX_DAY_TASKS) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("Новая задача") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { commit() },
                    enabled = draft.isNotBlank(),
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить задачу")
                }
            }
        } else {
            Text(
                "Достигнут предел — $MAX_DAY_TASKS задач на день",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
