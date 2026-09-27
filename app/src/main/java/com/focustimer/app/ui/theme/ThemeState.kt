package com.focustimer.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Theme choices that have to repaint the whole app the moment they change. The values are also
 * persisted in PrefsManager; this holder is what Compose observes, so flipping a switch in
 * Settings recolours the app immediately instead of waiting for a restart.
 */
object ThemeState {
    var dynamicColor by mutableStateOf(false)
}
