package com.focustimer.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import java.util.Locale

/**
 * The morning alarm: rings, reads the day out, and keeps going until it is stopped. It runs as a
 * foreground service so the system does not cut it short, and holds a short wake lock so the
 * speech is not clipped by the phone dozing off again.
 */
class AlarmService : Service() {

    private var player: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var ringing = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopEverything()
                return START_NOT_STICKY
            }
            else -> ring()
        }
        return START_STICKY
    }

    private fun ring() {
        if (ringing) return
        ringing = true
        val prefs = PrefsManager(applicationContext)
        startForeground(NOTIF_ID, buildNotification())

        @Suppress("DEPRECATION")
        wakeLock = getSystemService(PowerManager::class.java)
            ?.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "epv:alarm"
            )
            ?.also { runCatching { it.acquire(5 * 60 * 1000L) } }

        if (prefs.alarmSound) startRingtone()
        if (prefs.alarmSpeaks) startBriefing(prefs)
    }

    private fun startRingtone() {
        runCatching {
            val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: return
            player = MediaPlayer().apply {
                setDataSource(applicationContext, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        }
    }

    /**
     * The ringtone is ducked while the briefing plays, otherwise the two talk over each other.
     */
    private fun startBriefing(prefs: PrefsManager) {
        val text = morningBriefing(prefs)
        tts = TextToSpeech(applicationContext) { status ->
            if (status != TextToSpeech.SUCCESS) return@TextToSpeech
            val engine = tts ?: return@TextToSpeech
            engine.setLanguage(if (prefs.voiceLanguage == "English") Locale.US else Locale("ru"))
            engine.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    runCatching { player?.setVolume(0.15f, 0.15f) }
                }
                override fun onDone(utteranceId: String?) {
                    runCatching { player?.setVolume(1f, 1f) }
                }
                @Deprecated("Required by the interface")
                override fun onError(utteranceId: String?) {
                    runCatching { player?.setVolume(1f, 1f) }
                }
            })
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
        }
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stop = PendingIntent.getBroadcast(
            this, 1, Intent(this, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val snooze = PendingIntent.getBroadcast(
            this, 2, Intent(this, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_SNOOZE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val snoozeMinutes = PrefsManager(applicationContext).alarmSnoozeMinutes
        return NotificationCompat.Builder(this, CHANNEL_ALARM)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Подъём")
            .setContentText("План на день сейчас прозвучит")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(open)
            // Wakes the screen where the right is granted; elsewhere it shows as a heads-up.
            .setFullScreenIntent(open, true)
            .addAction(0, "Стоп", stop)
            .addAction(0, "Ещё $snoozeMinutes мин", snooze)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ALARM) != null) return
        val channel = NotificationChannel(
            CHANNEL_ALARM, "Будильник", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Утренний подъём с планом на день"
            // The service plays the sound itself, on the alarm stream.
            setSound(null, null)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
        }
        manager.createNotificationChannel(channel)
    }

    private fun stopEverything() {
        ringing = false
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        runCatching { tts?.stop() }
        runCatching { tts?.shutdown() }
        tts = null
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
        wakeLock = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopEverything()
        super.onDestroy()
    }

    companion object {
        const val ACTION_RING = "com.focustimer.app.ALARM_RING"
        const val ACTION_STOP = "com.focustimer.app.ALARM_SERVICE_STOP"
        private const val CHANNEL_ALARM = "epv_alarm"
        private const val NOTIF_ID = 4201
        private const val UTTERANCE_ID = "epv_morning_briefing"
    }
}

