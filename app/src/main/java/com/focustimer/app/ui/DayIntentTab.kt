package com.focustimer.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * One line a day, kept per date: the day's main task, or the day's goal. Written like a note,
 * but there is only ever one for today — which is the whole point of naming a single thing.
 *
 * Past days stay below, read-only, because the value of this is seeing the run of them.
 */
@Composable
fun DayIntentTab(
    title: String,
    hint: String,
    placeholder: String,
    today: String,
    saved: String,
    history: List<Pair<String, String>>,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var draft by remember(today, saved) { mutableStateOf(saved) }
    val dirty = draft.trim() != saved

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )

        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            placeholder = { Text(placeholder) },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { onSave(draft.trim()) }, enabled = dirty) {
                Text("Сохранить")
            }
            if (saved.isNotBlank()) {
                TextButton(
                    onClick = { draft = ""; onSave("") },
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text("Очистить", color = MaterialTheme.colorScheme.error)
                }
            }
            if (!dirty && saved.isNotBlank()) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(18.dp)
                )
                Text(
                    "записано на сегодня",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        val past = history.filter { it.first != today }
        if (past.isNotEmpty()) {
            Text(
                "Раньше",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 24.dp, bottom = 6.dp)
            )
            past.take(30).forEach { (date, text) ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            prettyDate(date),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

/** "2026-10-12" as "12 октября". */
private fun prettyDate(key: String): String {
    return try {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(key) ?: return key
        SimpleDateFormat("d MMMM", Locale("ru")).format(parsed)
    } catch (_: Exception) {
        key
    }
}
