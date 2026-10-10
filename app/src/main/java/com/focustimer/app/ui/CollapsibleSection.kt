package com.focustimer.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.focustimer.app.PrefsManager

/**
 * A section of the day screen that can be folded away. The open/closed state is stored under
 * [key], so the screen comes back the length it was left — the day screen is long enough that
 * re-collapsing the same blocks every time would be its own chore.
 *
 * [summary] is what the header says while the section is shut, so collapsing never hides the
 * fact that something is in there.
 */
@Composable
fun CollapsibleSection(
    key: String,
    title: String,
    summary: String? = null,
    defaultExpanded: Boolean = true,
    action: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }
    var expanded by remember(key) { mutableStateOf(prefs.sectionExpanded(key, defaultExpanded)) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    expanded = !expanded
                    prefs.setSectionExpanded(key, expanded)
                }
                .padding(vertical = 6.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (summary != null) {
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Свернуть" else "Развернуть",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }

        // The action sits outside the header row so tapping it never folds the section.
        if (action != null && expanded) {
            Box(modifier = Modifier.padding(bottom = 2.dp)) { action() }
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.fillMaxWidth(), content = content)
        }
    }
}
