package com.focustimer.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/**
 * Books the morning alarm with the system. [AlarmManager.setAlarmClock] is used deliberately:
 * besides being exact and surviving doze, reacting to it is one of the few cases where an app
 * may start a foreground service from the background — which is how the ringing works.
 */
object AlarmScheduler {

    const val REQUEST_ALARM = 7001
    const val REQUEST_SHOW = 7002

    fun reschedule(context: Context) {
        val prefs = PrefsManager(context)
        cancel(context)
        if (!prefs.alarmEnabled) return
        val at = nextTrigger(prefs.alarmTime, prefs.alarmDays) ?: return
        schedule(context, at)
    }

    /** Used by snooze, which is a one-off ring that does not touch the daily schedule. */
    fun scheduleAt(context: Context, atMillis: Long) = schedule(context, atMillis)

    fun cancel(context: Context) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        manager.cancel(firePendingIntent(context))
    }

    /** True when the system still lets us set an exact alarm; false means it will be approximate. */
    fun canBeExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val manager = context.getSystemService(AlarmManager::class.java) ?: return false
        return manager.canScheduleExactAlarms()
    }

    private fun schedule(context: Context, atMillis: Long) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        val fire = firePendingIntent(context)
        if (canBeExact(context)) {
            val show = PendingIntent.getActivity(
                context, REQUEST_SHOW,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            manager.setAlarmClock(AlarmManager.AlarmClockInfo(atMillis, show), fire)
        } else {
            // Without the exact-alarm right the best available is a window; say so in settings
            // rather than pretend the minute is guaranteed.
            manager.setWindow(
                AlarmManager.RTC_WAKEUP, atMillis, 10 * 60 * 1000L, fire
            )
        }
    }

    private fun firePendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, REQUEST_ALARM,
        Intent(context, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_FIRE),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    /**
     * The next moment matching [time] on one of [days], counting from now. Returns null only if
     * [days] is empty, which the preference never allows.
     */
    fun nextTrigger(time: String, days: Set<Int>, from: Long = System.currentTimeMillis()): Long? {
        if (days.isEmpty()) return null
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 7
        val minute = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0

        val cal = Calendar.getInstance()
        cal.timeInMillis = from
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        // Today at that time has already gone by, so start looking at tomorrow.
        if (cal.timeInMillis <= from) cal.add(Calendar.DAY_OF_MONTH, 1)

        repeat(8) {
            val weekday = ((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1
            if (days.contains(weekday)) return cal.timeInMillis
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return null
    }
}

/**
 * What the alarm reads out. Only the first few entries are spoken: a briefing that lists nine
 * items is one nobody listens to.
 */
fun morningBriefing(prefs: PrefsManager, nowMillis: Long = System.currentTimeMillis()): String {
    val cal = Calendar.getInstance()
    cal.timeInMillis = nowMillis
    val weekday = ((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1
    val items = prefs.planItems.filter { it.onDay(weekday) }.sortedBy { it.time }

    val parts = StringBuilder("Доброе утро. ")
    if (items.isEmpty()) {
        parts.append("На сегодня ничего не запланировано.")
    } else {
        val total = items.sumOf { it.minutes }
        parts.append("Сегодня ")
            .append(items.size).append(' ').append(plannedWord(items.size))
            .append(", ").append(spokenSpan(total)).append(". ")
        val first = items.first()
        parts.append("Начало в ").append(spokenTime(first.time)).append(" — ").append(first.title).append(". ")
        items.drop(1).take(2).forEach { item ->
            parts.append("Потом в ").append(spokenTime(item.time)).append(" — ").append(item.title).append(". ")
        }
        val rest = items.size - minOf(items.size, 3)
        if (rest > 0) parts.append("И ещё ").append(rest).append(' ').append(plannedWord(rest)).append(". ")
    }

    val dateKey = "%04d-%02d-%02d".format(
        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)
    )
    val tasks = prefs.dayTasks(dateKey).filter { !it.done }
    if (tasks.isNotEmpty()) {
        parts.append("Задач на день: ").append(tasks.size).append(". ")
        parts.append("Первая — ").append(tasks.first().title).append(".")
    }
    return parts.toString().trim()
}

/** "07:05" reads better as "семь ноль пять" than as a bare pair of numbers. */
private fun spokenTime(time: String): String {
    val parts = time.split(":")
    val hour = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: return time
    val minute = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
    return if (minute == 0) "$hour" + " часов" else "$hour:" + "%02d".format(minute)
}

private fun spokenSpan(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "$hours ${hoursWord(hours)} $minutes минут"
        hours > 0 -> "$hours ${hoursWord(hours)}"
        else -> "$minutes минут"
    }
}

private fun hoursWord(n: Int): String {
    val mod100 = n % 100
    val mod10 = n % 10
    return when {
        mod100 in 11..14 -> "часов"
        mod10 == 1 -> "час"
        mod10 in 2..4 -> "часа"
        else -> "часов"
    }
}

private fun plannedWord(n: Int): String {
    val mod100 = n % 100
    val mod10 = n % 10
    return when {
        mod100 in 11..14 -> "занятий"
        mod10 == 1 -> "занятие"
        mod10 in 2..4 -> "занятия"
        else -> "занятий"
    }
}
