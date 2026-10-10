package com.focustimer.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * How the week is set up: the timed schedule and the rituals around it. Both are things you
 * arrange once and then follow, which is why they share a tab instead of competing for the
 * day screen.
 */
@Composable
fun PlanScreen(modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Неделя") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Ритуалы") }
            )
        }
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> WeekScreen()
                1 -> RoutineScreen()
            }
        }
    }
}
