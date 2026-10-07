package com.focustimer.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

enum class TimerPhase { WORK, REST }

data class TimerUiState(
    val phase: TimerPhase = TimerPhase.WORK,
    val isRunning: Boolean = false,
    val secondsLeft: Int = 0,
    val workMinutes: Int = 25,
    val restMinutes: Int = 5,
    val escalationActive: Boolean = false,
    val currentComment: String = "",
    val currentCategory: String = "",
    val motivationQuote: String? = null
)

val MOTIVATIONAL_QUOTES = listOf(
    "Успех — это способность идти от одной неудачи к другой, не теряя энтузиазма. — Уинстон Черчилль",
    "Единственный способ сделать великую работу — любить то, что ты делаешь. — Стив Джобс",
    "Не бойтесь совершенства — вам его не достичь. — Сальвадор Дали",
    "Дисциплина — это мост между целями и результатом. — Джим Рон",
    "Я не терпел неудачу. Я просто нашёл 10 000 способов, которые не работают. — Томас Эдисон",
    "Секрет продвижения вперёд — начать. — Марк Твен",
    "Тяжело в учении — легко в бою. — Александр Суворов",
    "Будущее принадлежит тем, кто верит в красоту своей мечты. — Элеонора Рузвельт",
    "Маленькие ежедневные улучшения со временем дают потрясающие результаты. — Робин Шарма",
    "Ты никогда не будешь готов на 100%. Начни с тем, что есть. — Наполеон Хилл",
    "Не считай дни, делай дни значимыми. — Мухаммед Али",
    "Лучшее время посадить дерево было 20 лет назад. Второе лучшее — сейчас. — китайская пословица",
    "Делай то, что можешь, с тем, что имеешь, там, где ты есть. — Теодор Рузвельт",
    "Мотивация — то, что заставляет тебя начать. Привычка — то, что заставляет продолжать. — Джим Рон",
    "Единственный, кто может остановить тебя, — это ты сам. — неизвестный автор"
)

val WORK_DONE_PHRASES_RU = listOf(
    "Хэй, привет! Ну что, поработал? Пора бы и отдохнуть!",
    "Стоп машина! Работа подождёт — самое время выдохнуть.",
    "Есть! Рабочий блок закрыт. Отдых, встречай!",
    "Отличная работа. Дайте себе немного тишины и покоя.",
    "Всё, шабаш! Заслуженный перерыв уже ждёт.",
    "Мозг просит паузы — дайте ему то, что он хочет.",
    "Рабочий этап завершён. Забота о себе — тоже часть продуктивности.",
    "Ты справился! Теперь можно и ноги на стол.",
    "Тайм-аут! Тело и голова скажут спасибо за пару минут отдыха.",
    "Готово! Сделайте паузу — вы это заслужили."
)

val REST_DONE_PHRASES_RU = listOf(
    "Так, так, время пришло работать! Вперёд и с песней!",
    "Отдых закончен, будильник для мозга прозвенел. За работу!",
    "Перерыв — в архив. Погнали делать великие дела!",
    "Время снова включиться в работу. У вас точно получится.",
    "Батарейка заряжена на сто процентов — пора выдавать результат!",
    "Отдохнули — красота. Теперь покажем, на что способны!",
    "Рабочий режим активирован. Приступаем!",
    "Хватит бездельничать — шучу! Но работать и правда пора.",
    "Соберитесь — сейчас будет продуктивно и красиво.",
    "Вперёд, покоритель дедлайнов! Работа ждёт."
)

val WORK_DONE_PHRASES_EN = listOf(
    "Hey there! You worked hard, huh? Time to rest!",
    "Stop the presses! Work can wait — time to breathe out.",
    "Done and done! Work block closed. Hello, rest!",
    "Great work. Give yourself a little peace and quiet.",
    "That's a wrap! Your well-earned break is waiting.",
    "Your brain is asking for a pause — give it what it wants.",
    "Work block complete. Self-care is productivity too.",
    "You did it! Time to kick back for a bit.",
    "Time out! Your body and mind will thank you for a short rest.",
    "All done! Take a break — you've earned it."
)

val REST_DONE_PHRASES_EN = listOf(
    "Alright, alright, it's time to work! Onward, with a song!",
    "Break's over, the brain alarm just went off. Let's work!",
    "Break — archived. Let's go make great things happen!",
    "Time to get back into it. You've got this.",
    "Battery's at one hundred percent — time to deliver!",
    "Nicely rested. Now let's show what you can do!",
    "Work mode: activated. Let's go!",
    "Enough lounging around — just kidding! But it really is time to work.",
    "Get focused — this is about to be productive and great.",
    "Onward, deadline conqueror! Work awaits."
)

/**
 * Runs the work/rest countdown as a foreground service so it keeps going (and can escalate a
 * missed phase change) even while the app is backgrounded or the task is swiped away.
 */
class TimerService : Service() {

    inner class LocalBinder : Binder() {
        val service: TimerService get() = this@TimerService
    }

    private val binder = LocalBinder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var prefs: PrefsManager

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> get() = _uiState

    private var timerJob: Job? = null
    private var graceJob: Job? = null
    private var sessionStartMillis: Long? = null
    private var announcedThisPhase = false
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var nextVoiceFemale = true

    override fun onCreate() {
        super.onCreate()
        prefs = PrefsManager(applicationContext)
        _uiState.update {
            it.copy(
                workMinutes = prefs.workMinutes,
                restMinutes = prefs.restMinutes,
                secondsLeft = prefs.workMinutes * 60,
                currentCategory = prefs.categories.firstOrNull() ?: ""
            )
        }
        createNotificationChannels()
        tts = TextToSpeech(applicationContext) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_ACKNOWLEDGE) {
            acknowledge()
        }
        return START_STICKY
    }

    fun setWorkMinutes(minutes: Int) {
        prefs.workMinutes = minutes
        _uiState.update {
            if (!it.isRunning && it.phase == TimerPhase.WORK) {
                it.copy(workMinutes = minutes, secondsLeft = minutes * 60)
            } else {
                it.copy(workMinutes = minutes)
            }
        }
    }

    fun setRestMinutes(minutes: Int) {
        prefs.restMinutes = minutes
        _uiState.update {
            if (!it.isRunning && it.phase == TimerPhase.REST) {
                it.copy(restMinutes = minutes, secondsLeft = minutes * 60)
            } else {
                it.copy(restMinutes = minutes)
            }
        }
    }

    fun setComment(text: String) {
        _uiState.update { it.copy(currentComment = text) }
    }

    fun setCategory(category: String) {
        _uiState.update { it.copy(currentCategory = category) }
    }

    fun dismissQuote() {
        _uiState.update { it.copy(motivationQuote = null) }
    }

    fun start() {
        if (_uiState.value.isRunning) return
        if (sessionStartMillis == null) {
            sessionStartMillis = System.currentTimeMillis()
        }
        startForegroundCompat(buildOngoingNotification())
        _uiState.update { it.copy(isRunning = true) }
        timerJob?.cancel()
        timerJob = scope.launch {
            while (_uiState.value.secondsLeft > 0) {
                delay(1000)
                _uiState.update { it.copy(secondsLeft = it.secondsLeft - 1) }
                updateOngoingNotification()
                maybeAnnounceUpcomingPhase()
            }
            onPhaseFinished()
        }
    }

    private fun maybeAnnounceUpcomingPhase() {
        if (announcedThisPhase || !prefs.voiceAnnounceEnabled) return
        val lead = prefs.voiceAnnounceLeadSeconds()
        if (lead <= 0) return
        if (_uiState.value.secondsLeft <= lead) {
            announcedThisPhase = true
            val current = _uiState.value.phase
            val english = prefs.voiceLanguage == "English"
            val text = if (english) {
                if (current == TimerPhase.WORK) "Rest time is approaching" else "Work time is approaching"
            } else {
                if (current == TimerPhase.WORK) "Приближается время отдыха" else "Приближается время работы"
            }
            speak(text)
        }
    }

    private fun currentVoiceLocale(): Locale =
        if (prefs.voiceLanguage == "English") Locale.US else Locale("ru")

    private fun speak(text: String) {
        val female = nextVoiceFemale
        nextVoiceFemale = !nextVoiceFemale

        if (prefs.elevenLabsEnabled && prefs.elevenLabsApiKey.isNotBlank()) {
            scope.launch(Dispatchers.IO) {
                val audioFile = if (isNetworkAvailable()) fetchElevenLabsAudio(text, female) else null
                withContext(Dispatchers.Main) {
                    if (audioFile != null) {
                        playElevenLabsAudio(audioFile)
                    } else {
                        speakNative(text, female)
                    }
                }
            }
        } else {
            speakNative(text, female)
        }
    }

    /**
     * Falls back to the on-device TextToSpeech engine — used both when ElevenLabs is disabled
     * and whenever the ElevenLabs call fails (offline, invalid key, quota exceeded, etc.), so
     * voice announcements never silently stop working.
     */
    private fun speakNative(text: String, female: Boolean) {
        val engine = tts ?: return
        if (!ttsReady) return

        val locale = currentVoiceLocale()
        engine.setLanguage(locale)

        val voice = pickVoice(engine, locale, female)
        if (voice != null) {
            engine.setVoice(voice)
        }
        // Even when we can't reliably tell voices apart by gender, a pitch shift keeps the
        // alternation audible and pushes the flat default voice a bit further from "robotic".
        engine.setPitch(if (female) 1.12f else 0.86f)
        engine.setSpeechRate(1.0f)

        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "focus_timer_announce")
    }

    /** Runs on Dispatchers.IO. Returns null (never throws) so the caller can fall back to speakNative. */
    private fun fetchElevenLabsAudio(text: String, female: Boolean): File? {
        val voiceId = if (female) prefs.elevenLabsVoiceIdFemale else prefs.elevenLabsVoiceIdMale
        var connection: HttpURLConnection? = null
        return try {
            val url = URL("https://api.elevenlabs.io/v1/text-to-speech/$voiceId")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 15000
                setRequestProperty("xi-api-key", prefs.elevenLabsApiKey)
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "audio/mpeg")
            }
            val body = JSONObject().apply {
                put("text", text)
                put("model_id", "eleven_multilingual_v2")
                put("voice_settings", JSONObject().apply {
                    put("stability", 0.5)
                    put("similarity_boost", 0.75)
                })
            }
            OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }
            if (connection.responseCode != 200) return null

            val file = File(cacheDir, "eleven_${System.currentTimeMillis()}.mp3")
            connection.inputStream.use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            }
            file
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun playElevenLabsAudio(file: File) {
        try {
            val player = MediaPlayer()
            player.setDataSource(file.absolutePath)
            player.setOnCompletionListener {
                it.release()
                file.delete()
            }
            player.setOnErrorListener { mp, _, _ ->
                mp.release()
                file.delete()
                true
            }
            player.prepare()
            player.start()
        } catch (_: Exception) {
            file.delete()
        }
    }

    /**
     * Best-effort pick of a natural, gendered voice. Voice availability/naming/quality varies by
     * device and TTS engine, so this quietly falls back to the engine default when no match is
     * found — it never fails the TTS setup. Prefers the highest-quality (typically network/
     * WaveNet-style) voices over the flatter on-device ones, but only considers network voices
     * when a connection is actually up — picking one offline would just fail silently.
     */
    private fun pickVoice(engine: TextToSpeech, locale: Locale, female: Boolean): android.speech.tts.Voice? {
        val voices = engine.voices ?: return null
        val online = isNetworkAvailable()
        val candidates = voices
            .filter { it.locale.language == locale.language }
            .filter { online || !it.isNetworkConnectionRequired }
            .sortedByDescending { it.quality }
        if (candidates.isEmpty()) return null

        fun matches(voice: android.speech.tts.Voice): Boolean {
            val name = voice.name.lowercase()
            return if (female) {
                name.contains("female") || Regex("x-...-f(-|$)").containsMatchIn(name)
            } else {
                (name.contains("male") && !name.contains("female")) || Regex("x-...-[dm](-|$)").containsMatchIn(name)
            }
        }

        return candidates.firstOrNull { matches(it) } ?: candidates.firstOrNull()
    }

    private fun isNetworkAvailable(): Boolean {
        return try {
            val cm = getSystemService(ConnectivityManager::class.java) ?: return false
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * A plain countdown of [minutes], with no rest phase after it. Everything is set in one go
     * because setWorkMinutes alone refuses to move the clock while a phase is mid-flight.
     */
    fun startPlain(minutes: Int) {
        timerJob?.cancel()
        graceJob?.cancel()
        graceJob = null
        flushHistoryEntry(interrupted = true)
        prefs.workMinutes = minutes
        prefs.restMinutes = 0
        announcedThisPhase = false
        _uiState.update {
            it.copy(
                phase = TimerPhase.WORK,
                workMinutes = minutes,
                restMinutes = 0,
                secondsLeft = minutes * 60,
                isRunning = false,
                escalationActive = false
            )
        }
        start()
    }

    fun pause() {
        timerJob?.cancel()
        _uiState.update { it.copy(isRunning = false) }
        updateOngoingNotification()
    }

    fun stop() {
        timerJob?.cancel()
        graceJob?.cancel()
        graceJob = null
        flushHistoryEntry(interrupted = true)
        val minutes = _uiState.value.workMinutes
        announcedThisPhase = false
        _uiState.update {
            it.copy(isRunning = false, phase = TimerPhase.WORK, secondsLeft = minutes * 60, escalationActive = false)
        }
        clearNotifications()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun acknowledge() {
        graceJob?.cancel()
        graceJob = null
        if (_uiState.value.escalationActive) {
            _uiState.update { it.copy(escalationActive = false) }
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.cancel(NOTIF_ID_PHASE)
        nm.cancel(NOTIF_ID_ESCALATION)
    }

    private fun onPhaseFinished() {
        val finishedPhase = _uiState.value.phase
        val quote = if (finishedPhase == TimerPhase.WORK) MOTIVATIONAL_QUOTES.random() else null
        flushHistoryEntry(interrupted = false, quote = quote ?: "")

        // Rest set to zero means a plain countdown: it ends here instead of rolling into a rest
        // phase, so there is nothing to come back to and no escalation to arm.
        if (finishedPhase == TimerPhase.WORK && _uiState.value.restMinutes <= 0) {
            timerJob?.cancel()
            graceJob?.cancel()
            graceJob = null
            announcedThisPhase = false
            _uiState.update {
                it.copy(
                    phase = TimerPhase.WORK,
                    secondsLeft = it.workMinutes * 60,
                    isRunning = false,
                    escalationActive = false,
                    motivationQuote = quote ?: it.motivationQuote
                )
            }
            notifyTimerFinished()
            repeatAlert(times = 3)
            if (prefs.voiceAnnounceEnabled) {
                speak(if (prefs.voiceLanguage == "English") "Timer finished" else "Время вышло")
            }
            updateOngoingNotification()
            return
        }

        val nextPhase = if (finishedPhase == TimerPhase.WORK) TimerPhase.REST else TimerPhase.WORK
        val minutes = if (nextPhase == TimerPhase.WORK) _uiState.value.workMinutes else _uiState.value.restMinutes
        announcedThisPhase = false
        _uiState.update {
            it.copy(
                phase = nextPhase,
                secondsLeft = minutes * 60,
                isRunning = false,
                motivationQuote = quote ?: it.motivationQuote
            )
        }
        notifyPhaseChanged(nextPhase)
        armGraceTimer()
        start()
        repeatAlert(times = if (finishedPhase == TimerPhase.WORK) 3 else 1)
        if (prefs.voiceAnnounceEnabled) {
            val english = prefs.voiceLanguage == "English"
            val phrase = if (finishedPhase == TimerPhase.WORK) {
                (if (english) WORK_DONE_PHRASES_EN else WORK_DONE_PHRASES_RU).random()
            } else {
                (if (english) REST_DONE_PHRASES_EN else REST_DONE_PHRASES_RU).random()
            }
            speak(phrase)
        }
    }

    private fun repeatAlert(times: Int) {
        scope.launch {
            repeat(times) { index ->
                alertUser()
                if (index < times - 1) delay(1200)
            }
        }
    }

    private fun flushHistoryEntry(interrupted: Boolean, quote: String = "") {
        val start = sessionStartMillis ?: return
        val state = _uiState.value
        val plannedSeconds = (if (state.phase == TimerPhase.WORK) state.workMinutes else state.restMinutes) * 60
        val elapsedSeconds = if (interrupted) (plannedSeconds - state.secondsLeft).coerceAtLeast(0) else plannedSeconds
        prefs.addHistoryEntry(
            SessionRecord(
                id = start,
                phase = state.phase.name,
                startTimeMillis = start,
                durationSeconds = elapsedSeconds,
                interrupted = interrupted,
                comment = state.currentComment,
                category = state.currentCategory,
                quote = quote
            )
        )
        sessionStartMillis = null
        _uiState.update { it.copy(currentComment = "") }
    }

    private fun armGraceTimer() {
        graceJob?.cancel()
        graceJob = scope.launch {
            delay(MISSED_WINDOW_GRACE_MS)
            escalate()
        }
    }

    private fun escalate() {
        _uiState.update { it.copy(escalationActive = true) }
        showEscalationNotification()
        if (prefs.vibrationEnabled) vibrateNow()

        if (prefs.telegramEnabled && prefs.telegramBotToken.isNotBlank() && prefs.telegramChatId.isNotBlank()) {
            val phaseLabel = if (_uiState.value.phase == TimerPhase.WORK) "работа" else "отдых"
            sendTelegramMessage(
                "⏰ Focus Timer: окно \"$phaseLabel\" пропущено — приложение не открыли вовремя."
            )
        }

        if (prefs.autoCallEnabled && prefs.autoCallNumber.isNotBlank()) {
            placeAutoCall(prefs.autoCallNumber)
        }
    }

    private fun placeAutoCall(number: String) {
        val hasPermission = ActivityCompat.checkSelfPermission(
            this, android.Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return
        val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${Uri.encode(number)}")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            startActivity(callIntent)
        } catch (_: Exception) {
        }
    }

    private fun sendTelegramMessage(text: String) {
        val token = prefs.telegramBotToken
        val chatId = prefs.telegramChatId
        scope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://api.telegram.org/bot$token/sendMessage")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                val body = "chat_id=${URLEncoder.encode(chatId, "UTF-8")}&text=${URLEncoder.encode(text, "UTF-8")}"
                OutputStreamWriter(connection.outputStream).use { it.write(body) }
                connection.responseCode
                connection.disconnect()
            } catch (_: Exception) {
            }
        }
    }

    private fun alertUser() {
        if (prefs.vibrationEnabled) vibrateNow()
        if (prefs.soundEnabled) {
            try {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                RingtoneManager.getRingtone(applicationContext, uri)?.play()
            } catch (_: Exception) {
            }
        }
    }

    private fun vibrateNow() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID_ONGOING, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID_ONGOING, notification)
        }
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ONGOING, "Таймер", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Текущий отсчёт времени"
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_PHASE, "Смена этапа", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Работа/отдых начались"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ESCALATION, "Пропущенное окно", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Окно таймера пропущено"
            }
        )
    }

    private fun openAppPendingIntent(): PendingIntent {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
            ?: Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun acknowledgePendingIntent(requestCode: Int): PendingIntent {
        val intent = Intent(this, TimerService::class.java).apply { action = ACTION_ACKNOWLEDGE }
        return PendingIntent.getService(
            this, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildOngoingNotification(): Notification {
        val state = _uiState.value
        val phaseLabel = if (state.phase == TimerPhase.WORK) "Работа" else "Отдых"
        val minutes = state.secondsLeft / 60
        val seconds = state.secondsLeft % 60
        val timeText = "%02d:%02d".format(minutes, seconds)
        return NotificationCompat.Builder(this, CHANNEL_ONGOING)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle("$phaseLabel — $timeText")
            .setContentText(if (state.isRunning) "Таймер идёт" else "На паузе")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppPendingIntent())
            .build()
    }

    private fun updateOngoingNotification() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID_ONGOING, buildOngoingNotification())
    }

    /** The plain-countdown counterpart of [notifyPhaseChanged]: nothing starts after this one. */
    private fun notifyTimerFinished() {
        val notification = NotificationCompat.Builder(this, CHANNEL_PHASE)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle("Время вышло")
            .setContentText("Таймер завершён")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent())
            .build()
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID_PHASE, notification)
    }

    private fun notifyPhaseChanged(phase: TimerPhase) {
        val label = if (phase == TimerPhase.WORK) "Работа" else "Отдых"
        val notification = NotificationCompat.Builder(this, CHANNEL_PHASE)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle("$label началось")
            .setContentText("Откройте приложение или нажмите \"Я тут\" — иначе через минуту придёт напоминание")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent())
            .addAction(0, "Я тут", acknowledgePendingIntent(1))
            .build()
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID_PHASE, notification)
    }

    private fun showEscalationNotification() {
        val phaseLabel = if (_uiState.value.phase == TimerPhase.WORK) "Работа" else "Отдых"
        val notification = NotificationCompat.Builder(this, CHANNEL_ESCALATION)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle("Окно \"$phaseLabel\" пропущено!")
            .setContentText("Вы не отреагировали вовремя. Нажмите, чтобы открыть таймер.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent())
            .addAction(0, "Я тут", acknowledgePendingIntent(2))
            .build()
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID_ESCALATION, notification)
    }

    private fun clearNotifications() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.cancel(NOTIF_ID_PHASE)
        nm.cancel(NOTIF_ID_ESCALATION)
    }

    override fun onDestroy() {
        timerJob?.cancel()
        graceJob?.cancel()
        tts?.stop()
        tts?.shutdown()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ONGOING = "timer_ongoing"
        const val CHANNEL_PHASE = "timer_phase_change"
        const val CHANNEL_ESCALATION = "timer_escalation"
        const val NOTIF_ID_ONGOING = 1
        const val NOTIF_ID_PHASE = 2
        const val NOTIF_ID_ESCALATION = 3
        const val ACTION_ACKNOWLEDGE = "com.focustimer.app.ACTION_ACKNOWLEDGE"
        const val MISSED_WINDOW_GRACE_MS = 60_000L
    }
}
