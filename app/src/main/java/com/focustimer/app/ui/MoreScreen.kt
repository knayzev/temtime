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
 * Everything you set once and rarely open again. Settings and the profile were a tab each,
 * which spent two of five slots on configuration.
 */
@Composable
fun MoreScreen(
    onOpenLifestyle: () -> Unit = {},
    onOpenDetails: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Настройки") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Профиль") }
            )
        }
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> SettingsScreen()
                1 -> ProfileScreen(
                    onOpenLifestyle = onOpenLifestyle,
                    onOpenDetails = onOpenDetails
                )
            }
        }
    }
}
