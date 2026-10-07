package com.focustimer.app

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Thin proxy around [TimerService]. The countdown itself lives in the service so it survives
 * the app being backgrounded or the task being swiped away.
 */
class TimerViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState

    private var service: TimerService? = null
    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as TimerService.LocalBinder
            service = localBinder.service
            bound = true
            viewModelScope.launch {
                localBinder.service.uiState.collect { state ->
                    _uiState.value = state
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            bound = false
        }
    }

    init {
        val context = getApplication<Application>()
        context.bindService(Intent(context, TimerService::class.java), connection, Context.BIND_AUTO_CREATE)
    }

    fun start() {
        val context = getApplication<Application>()
        ContextCompat.startForegroundService(context, Intent(context, TimerService::class.java))
        service?.start()
    }

    /** A plain countdown of [minutes] with no rest phase after it. */
    fun startPlain(minutes: Int) {
        val context = getApplication<Application>()
        ContextCompat.startForegroundService(context, Intent(context, TimerService::class.java))
        service?.startPlain(minutes)
    }

    fun pause() = service?.pause()

    fun stop() = service?.stop()

    fun setWorkMinutes(minutes: Int) = service?.setWorkMinutes(minutes)

    fun setRestMinutes(minutes: Int) = service?.setRestMinutes(minutes)

    fun acknowledge() = service?.acknowledge()

    fun setComment(text: String) = service?.setComment(text)

    fun setCategory(category: String) = service?.setCategory(category)

    fun dismissQuote() = service?.dismissQuote()

    override fun onCleared() {
        if (bound) {
            getApplication<Application>().unbindService(connection)
            bound = false
        }
        super.onCleared()
    }
}
