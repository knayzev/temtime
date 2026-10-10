package com.focustimer.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.focustimer.app.JiraIssue

/**
 * What is in progress in Jira, read-only. Tapping an issue opens it in the browser; the plus
 * copies it into today's tasks, which is where it can actually be ticked off.
 */
@Composable
fun JiraTasks(
    issues: List<JiraIssue>,
    loading: Boolean,
    error: String?,
    syncedLabel: String?,
    onRefresh: () -> Unit,
    onOpen: (JiraIssue) -> Unit,
    onAddToDay: (JiraIssue) -> Unit,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (showHeader) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Над чем работаю", style = MaterialTheme.typography.titleMedium)
                    val caption = when {
                        loading -> "Обновляю…"
                        error != null -> error
                        syncedLabel != null -> "Из Jira · $syncedLabel"
                        else -> "Из Jira"
                    }
                    Text(
                        caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (error != null) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (loading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(20.dp)
                    )
                } else {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить из Jira")
                    }
                }
            }
        }

        if (issues.isEmpty() && !loading && error == null) {
            Text(
                "Ничего в работе — или запрос ничего не вернул.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        issues.forEach { issue ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpen(issue) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                issue.key,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (issue.status.isNotBlank()) {
                                Text(
                                    issue.status,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            issue.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    IconButton(onClick = { onAddToDay(issue) }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Добавить в задачи на день",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
