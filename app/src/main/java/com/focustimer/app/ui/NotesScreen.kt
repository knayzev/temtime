package com.focustimer.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.focustimer.app.MAX_NOTES
import com.focustimer.app.Note
import com.focustimer.app.PrefsManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A plain notebook. A note is just text: the first line becomes its heading in the list, so
 * writing something down takes one tap and no decisions.
 */
@Composable
fun NotesScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }

    var notes by remember { mutableStateOf(prefs.notes) }
    var editing by remember { mutableStateOf<Note?>(null) }
    var creating by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Note?>(null) }

    fun persist(next: List<Note>) {
        prefs.notes = next
        notes = prefs.notes
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp)
        ) {
            if (notes.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Заметок пока нет", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Мысли, решения, всё, что не дело со сроком. Первая строка станет заголовком.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            notes.forEach { note ->
                NoteCard(
                    note = note,
                    onOpen = { editing = note },
                    onTogglePin = {
                        persist(notes.map { if (it.id == note.id) it.copy(pinned = !it.pinned) else it })
                    }
                )
            }

            if (notes.size >= MAX_NOTES) {
                Text(
                    "Достигнут предел — $MAX_NOTES заметок",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }

        FloatingActionButton(
            onClick = { creating = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Новая заметка")
        }
    }

    if (creating) {
        NoteEditor(
            initial = "",
            title = "Новая заметка",
            onDismiss = { creating = false },
            onSave = { text ->
                if (notes.size < MAX_NOTES) {
                    persist(
                        notes + Note(
                            id = "note_${System.currentTimeMillis()}",
                            text = text,
                            updatedAtMillis = System.currentTimeMillis()
                        )
                    )
                }
                creating = false
            }
        )
    }

    editing?.let { note ->
        NoteEditor(
            initial = note.text,
            title = "Заметка",
            onDismiss = { editing = null },
            onDelete = { pendingDelete = note; editing = null },
            onSave = { text ->
                persist(
                    notes.map {
                        if (it.id == note.id) it.copy(text = text, updatedAtMillis = System.currentTimeMillis())
                        else it
                    }
                )
                editing = null
            }
        )
    }

    pendingDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Удалить заметку?") },
            text = { Text(note.heading.ifBlank { "Без заголовка" }) },
            confirmButton = {
                TextButton(onClick = {
                    persist(notes.filterNot { it.id == note.id })
                    pendingDelete = null
                }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun NoteCard(note: Note, onOpen: () -> Unit, onTogglePin: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (note.pinned) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpen)
            ) {
                Text(
                    note.heading.ifBlank { "Без заголовка" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                if (note.body.isNotBlank()) {
                    Text(
                        note.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Text(
                    noteStamp(note.updatedAtMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            IconButton(onClick = onTogglePin) {
                Icon(
                    if (note.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = if (note.pinned) "Открепить" else "Закрепить",
                    tint = if (note.pinned) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun NoteEditor(
    initial: String,
    title: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Первая строка — заголовок") },
                minLines = 6,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onSave(text.trim()) },
                enabled = text.isNotBlank()
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
}

private fun noteStamp(millis: Long): String {
    if (millis <= 0L) return ""
    val now = System.currentTimeMillis()
    val diff = (now - millis) / 1000
    return when {
        diff < 60 -> "только что"
        diff < 3600 -> "${diff / 60} мин назад"
        diff < 86400 -> "${diff / 3600} ч назад"
        else -> SimpleDateFormat("d MMM, HH:mm", Locale("ru")).format(Date(millis))
    }
}
