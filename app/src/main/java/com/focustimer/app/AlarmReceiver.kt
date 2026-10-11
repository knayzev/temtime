package com.focustimer.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/**
 * Three jobs: ring when the booked time arrives, re-book everything after a reboot (alarms do
 * not survive one), and carry the Stop and Snooze taps from the notification.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_FIRE -> {
                // Book the next one first: if anything below fails, tomorrow still rings.
                AlarmScheduler.reschedule(context)
                ContextCompat.startForegroundService(
                    context, Intent(context, AlarmService::class.java).setAction(AlarmService.ACTION_RING)
                )
            }
            ACTION_STOP -> context.startService(
                Intent(context, AlarmService::class.java).setAction(AlarmService.ACTION_STOP)
            )
            ACTION_SNOOZE -> {
                val minutes = PrefsManager(context).alarmSnoozeMinutes
                AlarmScheduler.scheduleAt(context, System.currentTimeMillis() + minutes * 60_000L)
                context.startService(
                    Intent(context, AlarmService::class.java).setAction(AlarmService.ACTION_STOP)
                )
            }
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON" -> AlarmScheduler.reschedule(context)
        }
    }

    companion object {
        const val ACTION_FIRE = "com.focustimer.app.ALARM_FIRE"
        const val ACTION_STOP = "com.focustimer.app.ALARM_STOP"
        const val ACTION_SNOOZE = "com.focustimer.app.ALARM_SNOOZE"
    }
}
