package com.focustimer.app

import android.content.Context
import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject

data class SessionRecord(
    val id: Long,
    val phase: String,
    val startTimeMillis: Long,
    val durationSeconds: Int,
    val interrupted: Boolean,
    val comment: String,
    val category: String = "",
    val quote: String = ""
)

data class TimerPreset(
    val id: String,
    val label: String,
    val workMinutes: Int,
    val restMinutes: Int,
    val comment: String = ""
)

data class DayPlan(
    val tasks: String = "",
    val priority: String = "",
    val dontForget: String = "",
    val updatedAtMillis: Long = 0L
) {
    val isEmpty: Boolean get() = tasks.isBlank() && priority.isBlank() && dontForget.isBlank()
}

/** Ritual periods. Stored as plain strings so old saved data keeps parsing. */
const val RITUAL_MORNING = "morning"
const val RITUAL_DAY = "day"
const val RITUAL_EVENING = "evening"
const val RITUAL_NIGHT = "night"

val RITUAL_PERIODS = listOf(RITUAL_MORNING, RITUAL_DAY, RITUAL_EVENING, RITUAL_NIGHT)

fun ritualPeriodTitle(period: String): String = when (period) {
    RITUAL_DAY -> "День"
    RITUAL_EVENING -> "Вечер"
    RITUAL_NIGHT -> "Перед сном"
    else -> "Утро"
}

fun ritualPeriodEmoji(period: String): String = when (period) {
    RITUAL_DAY -> "☀️"
    RITUAL_EVENING -> "🌆"
    RITUAL_NIGHT -> "🌙"
    else -> "🌅"
}

data class RoutineTask(
    val id: String,
    val title: String,
    val icon: String = "✅",
    val durationMinutes: Int = 0,
    val period: String = RITUAL_MORNING
)

val ROUTINE_TASK_LIBRARY = listOf(
    // Утро
    RoutineTask("routine_wake7", "Проснуться в 7 утра", "⏰", 0, RITUAL_MORNING),
    RoutineTask("routine_lie5", "Полежать 5 минут", "🛌", 5, RITUAL_MORNING),
    RoutineTask("routine_sit5", "Посидеть 5 минут", "🧘", 5, RITUAL_MORNING),
    RoutineTask("routine_water", "Выпить стакан воды", "💧", 1, RITUAL_MORNING),
    RoutineTask("routine_exercise", "Сделать зарядку", "🤸", 10, RITUAL_MORNING),
    RoutineTask("routine_teeth", "Почистить зубы", "🪥", 3, RITUAL_MORNING),
    RoutineTask("routine_shower", "Принять душ", "🚿", 10, RITUAL_MORNING),
    RoutineTask("routine_breakfast", "Позавтракать", "🍳", 15, RITUAL_MORNING),
    RoutineTask("routine_stretch", "Растяжка", "🤾", 10, RITUAL_MORNING),
    RoutineTask("routine_journal", "Записать мысли в дневник", "📓", 5, RITUAL_MORNING),
    RoutineTask("routine_plan", "Составить план на день", "📝", 5, RITUAL_MORNING),
    RoutineTask("routine_standup", "Утренний созвон", "📞", 15, RITUAL_MORNING),

    // День
    RoutineTask("routine_deepwork", "Блок глубокой работы", "🎯", 50, RITUAL_DAY),
    RoutineTask("routine_break", "Перерыв без экрана", "☕", 10, RITUAL_DAY),
    RoutineTask("routine_lunch", "Пообедать", "🍽️", 30, RITUAL_DAY),
    RoutineTask("routine_daywalk", "Выйти на воздух", "🌤️", 15, RITUAL_DAY),
    RoutineTask("routine_inbox", "Разобрать почту и сообщения", "📥", 20, RITUAL_DAY),
    RoutineTask("routine_eyes", "Гимнастика для глаз", "👀", 2, RITUAL_DAY),
    RoutineTask("routine_water_day", "Стакан воды", "💧", 1, RITUAL_DAY),

    // Вечер
    RoutineTask("routine_dinner", "Поужинать", "🍲", 30, RITUAL_EVENING),
    RoutineTask("routine_evwalk", "Вечерняя прогулка", "🚶", 20, RITUAL_EVENING),
    RoutineTask("routine_tidy", "Прибраться на столе", "🧹", 10, RITUAL_EVENING),
    RoutineTask("routine_review", "Подвести итоги дня", "✅", 10, RITUAL_EVENING),
    RoutineTask("routine_planned", "Спланировать завтра", "🗓️", 10, RITUAL_EVENING),
    RoutineTask("routine_family", "Время с близкими", "❤️", 30, RITUAL_EVENING),

    // Перед сном
    RoutineTask("routine_noscreen", "Убрать телефон", "📵", 0, RITUAL_NIGHT),
    RoutineTask("routine_warmshower", "Тёплый душ", "🛁", 15, RITUAL_NIGHT),
    RoutineTask("routine_air", "Проветрить спальню", "🪟", 10, RITUAL_NIGHT),
    RoutineTask("routine_read", "Чтение книги", "📖", 20, RITUAL_NIGHT),
    RoutineTask("routine_brain_dump", "Выписать тревожные мысли", "📝", 5, RITUAL_NIGHT),
    RoutineTask("routine_breath", "Дыхание 4-7-8", "🌬️", 5, RITUAL_NIGHT),
    RoutineTask("routine_bed", "Отбой", "😴", 0, RITUAL_NIGHT)
)

fun routineLibraryFor(period: String): List<RoutineTask> =
    ROUTINE_TASK_LIBRARY.filter { it.period == period }

val DEFAULT_ROUTINE_TASKS = listOf(
    "routine_wake7", "routine_water", "routine_exercise", "routine_teeth", "routine_breakfast",
    "routine_deepwork", "routine_lunch", "routine_daywalk",
    "routine_dinner", "routine_review", "routine_planned",
    "routine_noscreen", "routine_air", "routine_bed"
).mapNotNull { id -> ROUTINE_TASK_LIBRARY.firstOrNull { it.id == id } }

/**
 * One entry in the day's schedule: a single activity with its own clock time and length. This
 * replaced the earlier "plan holds a list of tasks" shape — a plan is now the activity itself.
 */
data class PlanItem(
    val id: String,
    val title: String,
    /** When it happens, as "HH:MM". */
    val time: String,
    val minutes: Int,
    val comment: String = "",
    /**
     * The weekdays it repeats on, 1 = Monday … 7 = Sunday. An empty set is read as every day, so
     * an entry can never end up invisible on every screen.
     */
    val days: Set<Int> = ALL_WEEKDAYS
) {
    fun onDay(weekday: Int): Boolean = days.isEmpty() || days.contains(weekday)
}

/** Monday-first, matching how the week is shown. */
val ALL_WEEKDAYS: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7)

/**
 * A one-off thing to do on a given date. Unlike [PlanItem] it has no time, no length and does not
 * repeat — it belongs to that date only.
 */
data class DayTask(
    val id: String,
    val title: String,
    val done: Boolean = false
)

/** How many one-off tasks a single day may hold. */
const val MAX_DAY_TASKS = 30

/**
 * A free-form note. The first line doubles as the heading in the list, so a note needs no
 * separate title field — one less thing to fill in before writing the thought down.
 */
data class Note(
    val id: String,
    val text: String,
    val updatedAtMillis: Long,
    val pinned: Boolean = false
) {
    val heading: String get() = text.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
    val body: String
        get() = text.lineSequence()
            .dropWhile { it.isBlank() }
            .drop(1)
            .joinToString(" ") { it.trim() }
            .trim()
}

/** Plenty for a personal notebook, and keeps the stored blob small. */
const val MAX_NOTES = 200

/** One Jira issue, cut down to what the day screen shows. */
data class JiraIssue(
    val key: String,
    val summary: String,
    val status: String
)

/**
 * Filtering by status category pulls in everything Jira calls "indeterminate" — on this board
 * that includes SUSPENDED and announcement. The plain status is what "working on it" means, and
 * the query is editable because that name differs per board and per language.
 */
const val DEFAULT_JIRA_JQL = "assignee = currentUser() AND status = \"В работе\" ORDER BY updated DESC"

/** How many entries one day may hold. */
const val MAX_PLAN_ITEMS = 20

data class PlanTask(
    val id: String,
    val title: String,
    val durationMinutes: Int,
    /** When the item is meant to happen, as "HH:MM". Blank means no time was set. */
    val timeOfDay: String = ""
)

data class PlanTemplate(
    val id: String,
    val name: String,
    val tasks: List<PlanTask>,
    val comment: String = ""
)

data class PlanHistoryEntry(
    val id: Long,
    val templateName: String,
    val completedAtMillis: Long,
    val taskCount: Int,
    val totalMinutes: Int,
    val note: String = ""
)

/** How many plans a user may keep at once. */
const val MAX_PLAN_TEMPLATES = 10

/**
 * Suggested items for a day, ordered from waking up to going to bed. The times are defaults the
 * user is expected to move; they exist so a freshly added item is never left without one.
 */
val PLAN_TASK_LIBRARY = listOf(
    PlanTask("plantask_wake", "Проснуться", 1, "06:30"),
    PlanTask("plantask_water", "Стакан воды", 1, "06:35"),
    PlanTask("plantask_exercise", "Зарядка", 15, "06:45"),
    PlanTask("plantask_stretch", "Растяжка", 10, "07:00"),
    PlanTask("plantask_shower", "Душ", 10, "07:15"),
    PlanTask("plantask_teeth", "Почистить зубы", 3, "07:25"),
    PlanTask("plantask_breakfast", "Завтрак", 25, "07:30"),
    PlanTask("plantask_plan", "Планирование дня", 10, "08:00"),
    PlanTask("plantask_meditate", "Медитация", 10, "08:10"),
    PlanTask("plantask_commute_work", "Дорога на работу", 30, "08:20"),
    PlanTask("plantask_deep_work", "Глубокая работа", 90, "09:00"),
    PlanTask("plantask_mail", "Разбор почты", 20, "10:30"),
    PlanTask("plantask_calls", "Созвоны", 45, "11:00"),
    PlanTask("plantask_snack", "Перекус", 10, "11:45"),
    PlanTask("plantask_work", "Рабочий блок", 60, "12:00"),
    PlanTask("plantask_lunch", "Обед", 40, "13:00"),
    PlanTask("plantask_walk_lunch", "Прогулка после обеда", 20, "13:40"),
    PlanTask("plantask_work_second", "Рабочий блок, вторая половина", 90, "14:00"),
    PlanTask("plantask_break", "Перерыв", 15, "15:30"),
    PlanTask("plantask_study", "Учёба и развитие", 45, "16:00"),
    PlanTask("plantask_tomorrow", "Разбор задач на завтра", 15, "17:30"),
    PlanTask("plantask_commute_home", "Дорога домой", 30, "18:00"),
    PlanTask("plantask_gym", "Тренажёрный зал", 75, "18:30"),
    PlanTask("plantask_shower_gym", "Душ после зала", 15, "20:00"),
    PlanTask("plantask_dinner", "Ужин", 30, "20:20"),
    PlanTask("plantask_family", "Время с близкими", 45, "21:00"),
    PlanTask("plantask_read", "Чтение", 30, "21:45"),
    PlanTask("plantask_walk_evening", "Прогулка перед сном", 20, "22:15"),
    PlanTask("plantask_no_screens", "Без экранов", 30, "22:35"),
    PlanTask("plantask_bed_prep", "Подготовка ко сну", 15, "23:00"),
    PlanTask("plantask_sleep", "Отбой", 1, "23:15")
)

/** The handful offered as chips, so the quick picks do not become another long list. */
val PLAN_QUICK_PICK_IDS = listOf(
    "plantask_breakfast",
    "plantask_exercise",
    "plantask_plan",
    "plantask_deep_work",
    "plantask_lunch",
    "plantask_walk_lunch",
    "plantask_work_second",
    "plantask_study",
    "plantask_gym",
    "plantask_dinner",
    "plantask_read",
    "plantask_bed_prep"
)


val DEFAULT_CATEGORIES = listOf("Работа", "Учёба", "Соцсети", "Прокрастинация", "Другое")

val DEFAULT_PRESETS = listOf(
    TimerPreset("preset_work25", "Работа 25 мин", 25, 5),
    TimerPreset("preset_deep50", "Глубокая работа 50 мин", 50, 10),
    TimerPreset("preset_study45", "Учёба 45 мин", 45, 15),
    TimerPreset("preset_sprint15", "Спринт 15 мин", 15, 5)
)

fun sha256(text: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

class PrefsManager(context: Context) {
    private val prefs = context.getSharedPreferences("focus_timer_prefs", Context.MODE_PRIVATE)

    var userName: String
        get() = prefs.getString(KEY_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_NAME, value).apply()

    var lastName: String
        get() = prefs.getString(KEY_LAST_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_NAME, value).apply()

    /** Drives which day-plan preset and advice the generator picks. */
    var profession: String
        get() = prefs.getString(KEY_PROFESSION, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PROFESSION, value).apply()

    /** Free-form extra context the user adds on top of the profession. */
    var professionDetails: String
        get() = prefs.getString(KEY_PROFESSION_DETAILS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PROFESSION_DETAILS, value).apply()

    /**
     * Two productivity windows the day is measured in. Stats are grouped by these instead of
     * one flat daily total, so a morning block and an afternoon block can be compared.
     */
    var focusWindowOneStart: String
        get() = prefs.getString(KEY_FOCUS_W1_START, "08:00") ?: "08:00"
        set(value) = prefs.edit().putString(KEY_FOCUS_W1_START, value).apply()

    var focusWindowOneEnd: String
        get() = prefs.getString(KEY_FOCUS_W1_END, "15:00") ?: "15:00"
        set(value) = prefs.edit().putString(KEY_FOCUS_W1_END, value).apply()

    var focusWindowTwoStart: String
        get() = prefs.getString(KEY_FOCUS_W2_START, "15:30") ?: "15:30"
        set(value) = prefs.edit().putString(KEY_FOCUS_W2_START, value).apply()

    var focusWindowTwoEnd: String
        get() = prefs.getString(KEY_FOCUS_W2_END, "19:00") ?: "19:00"
        set(value) = prefs.edit().putString(KEY_FOCUS_W2_END, value).apply()

    var dataConsentGiven: Boolean
        get() = prefs.getBoolean(KEY_DATA_CONSENT, false)
        set(value) = prefs.edit().putBoolean(KEY_DATA_CONSENT, value).apply()

    var stepsEnabled: Boolean
        get() = prefs.getBoolean(KEY_STEPS_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_STEPS_ENABLED, value).apply()

    var stepsBaselineDate: String
        get() = prefs.getString(KEY_STEPS_BASELINE_DATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_STEPS_BASELINE_DATE, value).apply()

    var stepsBaselineCount: Int
        get() = prefs.getInt(KEY_STEPS_BASELINE_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_STEPS_BASELINE_COUNT, value).apply()

    var voiceAnnounceEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOICE_ANNOUNCE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_VOICE_ANNOUNCE_ENABLED, value).apply()

    var voiceAnnounceLeadValue: Int
        get() = prefs.getInt(KEY_VOICE_ANNOUNCE_VALUE, 30)
        set(value) = prefs.edit().putInt(KEY_VOICE_ANNOUNCE_VALUE, value).apply()

    var voiceAnnounceUnit: String
        get() = prefs.getString(KEY_VOICE_ANNOUNCE_UNIT, "Секунды") ?: "Секунды"
        set(value) = prefs.edit().putString(KEY_VOICE_ANNOUNCE_UNIT, value).apply()

    fun voiceAnnounceLeadSeconds(): Int =
        if (voiceAnnounceUnit == "Минуты") voiceAnnounceLeadValue * 60 else voiceAnnounceLeadValue

    var voiceLanguage: String
        get() = prefs.getString(KEY_VOICE_LANGUAGE, "Русский") ?: "Русский"
        set(value) = prefs.edit().putString(KEY_VOICE_LANGUAGE, value).apply()

    var elevenLabsEnabled: Boolean
        get() = prefs.getBoolean(KEY_ELEVEN_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ELEVEN_ENABLED, value).apply()

    var elevenLabsApiKey: String
        get() = prefs.getString(KEY_ELEVEN_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ELEVEN_API_KEY, value).apply()

    var elevenLabsVoiceIdMale: String
        get() = prefs.getString(KEY_ELEVEN_VOICE_MALE, DEFAULT_ELEVEN_VOICE_MALE) ?: DEFAULT_ELEVEN_VOICE_MALE
        set(value) = prefs.edit().putString(KEY_ELEVEN_VOICE_MALE, value).apply()

    var elevenLabsVoiceIdFemale: String
        get() = prefs.getString(KEY_ELEVEN_VOICE_FEMALE, DEFAULT_ELEVEN_VOICE_FEMALE) ?: DEFAULT_ELEVEN_VOICE_FEMALE
        set(value) = prefs.edit().putString(KEY_ELEVEN_VOICE_FEMALE, value).apply()

    var photoUri: String?
        get() = prefs.getString(KEY_PHOTO, null)
        set(value) = prefs.edit().putString(KEY_PHOTO, value).apply()

    var workMinutes: Int
        get() = prefs.getInt(KEY_WORK_MIN, 25)
        set(value) = prefs.edit().putInt(KEY_WORK_MIN, value).apply()

    var restMinutes: Int
        get() = prefs.getInt(KEY_REST_MIN, 5)
        set(value) = prefs.edit().putInt(KEY_REST_MIN, value).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND, value).apply()

    var vibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATION, value).apply()

    var keepScreenOn: Boolean
        get() = prefs.getBoolean(KEY_KEEP_SCREEN_ON, true)
        set(value) = prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, value).apply()

    /** Material You: recolour the app from the wallpaper on Android 12+. Off = brand palette. */
    var dynamicColorEnabled: Boolean
        get() = prefs.getBoolean(KEY_DYNAMIC_COLOR, false)
        set(value) = prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, value).apply()

    var telegramEnabled: Boolean
        get() = prefs.getBoolean(KEY_TG_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_TG_ENABLED, value).apply()

    var telegramBotToken: String
        get() = prefs.getString(KEY_TG_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TG_TOKEN, value).apply()

    var telegramChatId: String
        get() = prefs.getString(KEY_TG_CHAT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TG_CHAT, value).apply()

    var autoCallEnabled: Boolean
        get() = prefs.getBoolean(KEY_CALL_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_CALL_ENABLED, value).apply()

    var autoCallNumber: String
        get() = prefs.getString(KEY_CALL_NUMBER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CALL_NUMBER, value).apply()

    var email: String
        get() = prefs.getString(KEY_EMAIL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_EMAIL, value).apply()

    var weightKg: String
        get() = prefs.getString(KEY_WEIGHT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_WEIGHT, value).apply()

    var heightCm: String
        get() = prefs.getString(KEY_HEIGHT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_HEIGHT, value).apply()

    var age: String
        get() = prefs.getString(KEY_AGE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_AGE, value).apply()

    var maritalStatus: String
        get() = prefs.getString(KEY_MARITAL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_MARITAL, value).apply()

    var gender: String
        get() = prefs.getString(KEY_GENDER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GENDER, value).apply()

    var personalityType: String
        get() = prefs.getString(KEY_PERSONALITY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PERSONALITY, value).apply()

    var nightWakeFrequency: String
        get() = prefs.getString(KEY_NIGHT_WAKE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_NIGHT_WAKE, value).apply()

    var wakeTime: String
        get() = prefs.getString(KEY_WAKE_TIME, "07:00") ?: "07:00"
        set(value) = prefs.edit().putString(KEY_WAKE_TIME, value).apply()

    var bedTime: String
        get() = prefs.getString(KEY_BED_TIME, "23:00") ?: "23:00"
        set(value) = prefs.edit().putString(KEY_BED_TIME, value).apply()

    var isWorking: Boolean
        get() = prefs.getBoolean(KEY_IS_WORKING, true)
        set(value) = prefs.edit().putBoolean(KEY_IS_WORKING, value).apply()

    var accountPasswordHash: String
        get() = prefs.getString(KEY_PASSWORD_HASH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PASSWORD_HASH, value).apply()

    var isRegistered: Boolean
        get() = prefs.getBoolean(KEY_IS_REGISTERED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_REGISTERED, value).apply()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    var isOnboarded: Boolean
        get() = prefs.getBoolean(KEY_IS_ONBOARDED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_ONBOARDED, value).apply()

    var breakfastTime: String
        get() = prefs.getString(KEY_BREAKFAST_TIME, "08:00") ?: "08:00"
        set(value) = prefs.edit().putString(KEY_BREAKFAST_TIME, value).apply()

    var lunchTime: String
        get() = prefs.getString(KEY_LUNCH_TIME, "13:00") ?: "13:00"
        set(value) = prefs.edit().putString(KEY_LUNCH_TIME, value).apply()

    var dinnerTime: String
        get() = prefs.getString(KEY_DINNER_TIME, "19:00") ?: "19:00"
        set(value) = prefs.edit().putString(KEY_DINNER_TIME, value).apply()

    var workHoursPerDay: Int
        get() = prefs.getInt(KEY_WORK_HOURS_PER_DAY, 8)
        set(value) = prefs.edit().putInt(KEY_WORK_HOURS_PER_DAY, value).apply()

    var mealsPerDay: Int
        get() = prefs.getInt(KEY_MEALS_PER_DAY, 3)
        set(value) = prefs.edit().putInt(KEY_MEALS_PER_DAY, value).apply()

    var waterUnit: String
        get() = prefs.getString(KEY_WATER_UNIT, "Бутылки") ?: "Бутылки"
        set(value) = prefs.edit().putString(KEY_WATER_UNIT, value).apply()

    var waterCount: Int
        get() = prefs.getInt(KEY_WATER_COUNT, 4)
        set(value) = prefs.edit().putInt(KEY_WATER_COUNT, value).apply()

    var daySchedule: String
        get() = prefs.getString(KEY_DAY_SCHEDULE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DAY_SCHEDULE, value).apply()

    var dayPlan: DayPlan
        get() {
            val raw = prefs.getString(KEY_DAY_PLAN, null) ?: return DayPlan()
            return try {
                val obj = JSONObject(raw)
                DayPlan(
                    tasks = obj.optString("tasks", ""),
                    priority = obj.optString("priority", ""),
                    dontForget = obj.optString("dontForget", ""),
                    updatedAtMillis = obj.optLong("updatedAtMillis", 0L)
                )
            } catch (_: Exception) {
                DayPlan()
            }
        }
        set(value) {
            val obj = JSONObject().apply {
                put("tasks", value.tasks)
                put("priority", value.priority)
                put("dontForget", value.dontForget)
                put("updatedAtMillis", value.updatedAtMillis)
            }
            prefs.edit().putString(KEY_DAY_PLAN, obj.toString()).apply()
        }

    var routineTasks: List<RoutineTask>
        get() {
            val raw = prefs.getString(KEY_ROUTINE_TASKS, null) ?: return DEFAULT_ROUTINE_TASKS
            return try {
                val array = JSONArray(raw)
                (0 until array.length()).map { i ->
                    val obj = array.getJSONObject(i)
                    val id = obj.getString("id")
                    RoutineTask(
                        id = id,
                        title = obj.getString("title"),
                        icon = obj.optString("icon", "✅"),
                        durationMinutes = obj.optInt("durationMinutes", 0),
                        // Tasks saved before ritual periods existed carry no field; fall back to
                        // the library entry so they land in the right section instead of all-morning.
                        period = obj.optString(
                            "period",
                            ROUTINE_TASK_LIBRARY.firstOrNull { it.id == id }?.period ?: RITUAL_MORNING
                        )
                    )
                }
            } catch (_: Exception) {
                DEFAULT_ROUTINE_TASKS
            }
        }
        set(value) {
            val array = JSONArray()
            value.forEach { task ->
                array.put(
                    JSONObject().apply {
                        put("id", task.id)
                        put("title", task.title)
                        put("icon", task.icon)
                        put("durationMinutes", task.durationMinutes)
                        put("period", task.period)
                    }
                )
            }
            prefs.edit().putString(KEY_ROUTINE_TASKS, array.toString()).apply()
        }

    private var routineCompletionsRaw: JSONObject
        get() {
            val raw = prefs.getString(KEY_ROUTINE_COMPLETIONS, null) ?: return JSONObject()
            return try { JSONObject(raw) } catch (_: Exception) { JSONObject() }
        }
        set(value) = prefs.edit().putString(KEY_ROUTINE_COMPLETIONS, value.toString()).apply()

    fun getCompletedRoutineIds(date: String): Set<String> {
        val arr = routineCompletionsRaw.optJSONArray(date) ?: return emptySet()
        return (0 until arr.length()).map { arr.getString(it) }.toSet()
    }

    fun setRoutineTaskDone(date: String, taskId: String, done: Boolean) {
        val root = routineCompletionsRaw
        val current = getCompletedRoutineIds(date).toMutableSet()
        if (done) current.add(taskId) else current.remove(taskId)
        root.put(date, JSONArray(current.toList()))
        routineCompletionsRaw = root
    }

    /**
     * Consecutive-day streak for a task, counted backward from today. If today isn't done yet
     * the streak still reflects the run ending yesterday, so it doesn't drop to zero mid-day.
     */
    fun routineStreak(taskId: String, today: String, previousDates: List<String>): Int {
        var streak = if (getCompletedRoutineIds(today).contains(taskId)) 1 else 0
        for (date in previousDates) {
            if (getCompletedRoutineIds(date).contains(taskId)) {
                streak++
            } else {
                break
            }
        }
        return streak
    }

    var daySummaries: Map<String, String>
        get() {
            val raw = prefs.getString(KEY_DAY_SUMMARIES, null) ?: return emptyMap()
            return try {
                val obj = JSONObject(raw)
                obj.keys().asSequence().associateWith { obj.getString(it) }
            } catch (_: Exception) {
                emptyMap()
            }
        }
        set(value) {
            val obj = JSONObject()
            value.forEach { (date, text) -> obj.put(date, text) }
            prefs.edit().putString(KEY_DAY_SUMMARIES, obj.toString()).apply()
        }

    fun getDaySummary(date: String): String = daySummaries[date] ?: ""

    fun setDaySummary(date: String, text: String) {
        val updated = daySummaries.toMutableMap()
        if (text.isBlank()) updated.remove(date) else updated[date] = text
        daySummaries = updated
    }

    var planItems: List<PlanItem>
        get() {
            val raw = prefs.getString(KEY_PLAN_ITEMS, null) ?: return emptyList()
            return try {
                val array = JSONArray(raw)
                (0 until array.length()).map { i ->
                    val o = array.getJSONObject(i)
                    val daysArr = o.optJSONArray("days")
                    PlanItem(
                        id = o.getString("id"),
                        title = o.getString("title"),
                        time = o.optString("time", "09:00"),
                        minutes = o.optInt("minutes", 30),
                        comment = o.optString("comment", ""),
                        // Entries saved before the week existed applied to every day.
                        days = if (daysArr == null) ALL_WEEKDAYS
                        else (0 until daysArr.length()).map { daysArr.getInt(it) }.toSet()
                    )
                }.sortedBy { it.time }
            } catch (_: Exception) {
                emptyList()
            }
        }
        set(value) {
            val array = JSONArray()
            value.sortedBy { it.time }.forEach { item ->
                array.put(
                    JSONObject().apply {
                        put("id", item.id)
                        put("title", item.title)
                        put("time", item.time)
                        put("minutes", item.minutes)
                        put("comment", item.comment)
                        put("days", JSONArray(item.days.sorted()))
                    }
                )
            }
            prefs.edit().putString(KEY_PLAN_ITEMS, array.toString()).apply()
        }

    private var planCompletionsRaw: JSONObject
        get() {
            val raw = prefs.getString(KEY_PLAN_COMPLETIONS, null) ?: return JSONObject()
            return try { JSONObject(raw) } catch (_: Exception) { JSONObject() }
        }
        set(value) = prefs.edit().putString(KEY_PLAN_COMPLETIONS, value.toString()).apply()

    fun completedPlanIds(date: String): Set<String> {
        val arr = planCompletionsRaw.optJSONArray(date) ?: return emptySet()
        return (0 until arr.length()).map { arr.getString(it) }.toSet()
    }

    fun setPlanItemDone(date: String, itemId: String, done: Boolean) {
        val root = planCompletionsRaw
        val current = completedPlanIds(date).toMutableSet()
        if (done) current.add(itemId) else current.remove(itemId)
        root.put(date, JSONArray(current.toList()))
        planCompletionsRaw = root
    }

    private var dayTasksRaw: JSONObject
        get() {
            val raw = prefs.getString(KEY_DAY_TASKS, null) ?: return JSONObject()
            return try { JSONObject(raw) } catch (_: Exception) { JSONObject() }
        }
        set(value) = prefs.edit().putString(KEY_DAY_TASKS, value.toString()).apply()

    fun dayTasks(date: String): List<DayTask> {
        val arr = dayTasksRaw.optJSONArray(date) ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            DayTask(
                id = o.optString("id", "task_$i"),
                title = o.optString("title", ""),
                done = o.optBoolean("done", false)
            )
        }.filter { it.title.isNotBlank() }
    }

    fun setDayTasks(date: String, tasks: List<DayTask>) {
        val root = dayTasksRaw
        if (tasks.isEmpty()) {
            root.remove(date)
        } else {
            val arr = JSONArray()
            tasks.take(MAX_DAY_TASKS).forEach { task ->
                arr.put(
                    JSONObject().apply {
                        put("id", task.id)
                        put("title", task.title)
                        put("done", task.done)
                    }
                )
            }
            root.put(date, arr)
        }
        // Old days are of no use once they are off the screen, and this map is never paged.
        val cutoff = dateKeyDaysAgo(120)
        root.keys().asSequence().filter { it < cutoff }.toList().forEach { root.remove(it) }
        dayTasksRaw = root
    }

    /** Pinned first, then most recently touched — the order the list is shown in. */
    var notes: List<Note>
        get() {
            val raw = prefs.getString(KEY_NOTES, null) ?: return emptyList()
            return try {
                val arr = JSONArray(raw)
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    Note(
                        id = o.optString("id", "note_$i"),
                        text = o.optString("text", ""),
                        updatedAtMillis = o.optLong("updatedAt", 0L),
                        pinned = o.optBoolean("pinned", false)
                    )
                }.filter { it.text.isNotBlank() }
                    .sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.updatedAtMillis })
            } catch (_: Exception) {
                emptyList()
            }
        }
        set(value) {
            val arr = JSONArray()
            value.take(MAX_NOTES).forEach { note ->
                arr.put(
                    JSONObject().apply {
                        put("id", note.id)
                        put("text", note.text)
                        put("updatedAt", note.updatedAtMillis)
                        put("pinned", note.pinned)
                    }
                )
            }
            prefs.edit().putString(KEY_NOTES, arr.toString()).apply()
        }

    var jiraEnabled: Boolean
        get() = prefs.getBoolean(KEY_JIRA_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_JIRA_ENABLED, value).apply()

    /** Host only, e.g. "brightcall.atlassian.net". */
    var jiraSite: String
        get() = prefs.getString(KEY_JIRA_SITE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_JIRA_SITE, value.trim().removePrefix("https://").removeSuffix("/")).apply()

    var jiraEmail: String
        get() = prefs.getString(KEY_JIRA_EMAIL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_JIRA_EMAIL, value.trim()).apply()

    /** An Atlassian API token. It grants full access to that account, so it never leaves the device. */
    var jiraToken: String
        get() = prefs.getString(KEY_JIRA_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_JIRA_TOKEN, value.trim()).apply()

    var jiraJql: String
        get() = prefs.getString(KEY_JIRA_JQL, DEFAULT_JIRA_JQL) ?: DEFAULT_JIRA_JQL
        set(value) = prefs.edit().putString(KEY_JIRA_JQL, value).apply()

    var jiraSyncedAtMillis: Long
        get() = prefs.getLong(KEY_JIRA_SYNCED_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_JIRA_SYNCED_AT, value).apply()

    /** The last answer from Jira, so the list is there before the first refresh of a session. */
    var jiraIssues: List<JiraIssue>
        get() {
            val raw = prefs.getString(KEY_JIRA_ISSUES, null) ?: return emptyList()
            return try {
                val arr = JSONArray(raw)
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    JiraIssue(
                        key = o.optString("key", ""),
                        summary = o.optString("summary", ""),
                        status = o.optString("status", "")
                    )
                }.filter { it.key.isNotBlank() }
            } catch (_: Exception) {
                emptyList()
            }
        }
        set(value) {
            val arr = JSONArray()
            value.forEach { issue ->
                arr.put(
                    JSONObject().apply {
                        put("key", issue.key)
                        put("summary", issue.summary)
                        put("status", issue.status)
                    }
                )
            }
            prefs.edit().putString(KEY_JIRA_ISSUES, arr.toString()).apply()
        }

    /** The day whose term of the day has already been dismissed. */
    var wordSeenDate: String
        get() = prefs.getString(KEY_WORD_SEEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_WORD_SEEN, value).apply()

    /**
     * Plans used to be named containers of timed tasks. Those tasks are what the schedule is made
     * of now, so the first run after the change lifts them out and drops the containers. Runs once:
     * afterwards the flag is set even when there was nothing to carry over.
     */
    fun migratePlansIfNeeded() {
        if (prefs.getBoolean(KEY_PLANS_MIGRATED, false)) return
        if (planItems.isEmpty()) {
            val flat = planTemplates
                .flatMap { it.tasks }
                .distinctBy { it.timeOfDay + it.title }
                .map { task ->
                    PlanItem(
                        id = task.id,
                        title = task.title,
                        time = task.timeOfDay.ifBlank { "09:00" },
                        minutes = task.durationMinutes,
                        comment = ""
                    )
                }
                .sortedBy { it.time }
                .take(MAX_PLAN_ITEMS)
            if (flat.isNotEmpty()) planItems = flat
        }
        prefs.edit().putBoolean(KEY_PLANS_MIGRATED, true).apply()
    }

    var planTemplates: List<PlanTemplate>
        get() {
            val raw = prefs.getString(KEY_PLAN_TEMPLATES, null) ?: return emptyList()
            return try {
                val array = JSONArray(raw)
                (0 until array.length()).map { i ->
                    val obj = array.getJSONObject(i)
                    val tasksArr = obj.getJSONArray("tasks")
                    val tasks = (0 until tasksArr.length()).map { j ->
                        val t = tasksArr.getJSONObject(j)
                        PlanTask(
                            id = t.getString("id"),
                            title = t.getString("title"),
                            durationMinutes = t.getInt("durationMinutes"),
                            // Plans saved before items carried a time fall back to the
                            // library default, then to blank.
                            timeOfDay = t.optString(
                                "timeOfDay",
                                PLAN_TASK_LIBRARY.firstOrNull { it.id == t.getString("id") }?.timeOfDay ?: ""
                            )
                        )
                    }
                    PlanTemplate(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        tasks = tasks,
                        comment = obj.optString("comment", "")
                    )
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
        set(value) {
            val array = JSONArray()
            value.forEach { template ->
                val tasksArr = JSONArray()
                template.tasks.forEach { task ->
                    tasksArr.put(
                        JSONObject().apply {
                            put("id", task.id)
                            put("title", task.title)
                            put("durationMinutes", task.durationMinutes)
                            put("timeOfDay", task.timeOfDay)
                        }
                    )
                }
                array.put(
                    JSONObject().apply {
                        put("id", template.id)
                        put("name", template.name)
                        put("comment", template.comment)
                        put("tasks", tasksArr)
                    }
                )
            }
            prefs.edit().putString(KEY_PLAN_TEMPLATES, array.toString()).apply()
        }

    var activePlanTemplateId: String?
        get() = prefs.getString(KEY_ACTIVE_PLAN_TEMPLATE, null)
        set(value) = prefs.edit().putString(KEY_ACTIVE_PLAN_TEMPLATE, value).apply()

    fun getPlanHistory(): List<PlanHistoryEntry> {
        val raw = prefs.getString(KEY_PLAN_HISTORY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                PlanHistoryEntry(
                    id = obj.getLong("id"),
                    templateName = obj.getString("templateName"),
                    completedAtMillis = obj.getLong("completedAtMillis"),
                    taskCount = obj.getInt("taskCount"),
                    totalMinutes = obj.getInt("totalMinutes"),
                    note = obj.optString("note", "")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addPlanHistoryEntry(entry: PlanHistoryEntry) {
        val updated = listOf(entry) + getPlanHistory()
        savePlanHistory(updated.take(MAX_HISTORY_ENTRIES))
    }

    fun updatePlanHistoryNote(id: Long, note: String) {
        val updated = getPlanHistory().map { if (it.id == id) it.copy(note = note) else it }
        savePlanHistory(updated)
    }

    fun deletePlanHistoryEntry(id: Long) {
        savePlanHistory(getPlanHistory().filter { it.id != id })
    }

    private fun savePlanHistory(entries: List<PlanHistoryEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject().apply {
                    put("id", entry.id)
                    put("templateName", entry.templateName)
                    put("completedAtMillis", entry.completedAtMillis)
                    put("taskCount", entry.taskCount)
                    put("totalMinutes", entry.totalMinutes)
                    put("note", entry.note)
                }
            )
        }
        prefs.edit().putString(KEY_PLAN_HISTORY, array.toString()).apply()
    }

    var lifestyleAnswers: Map<String, List<String>>
        get() {
            val raw = prefs.getString(KEY_LIFESTYLE_ANSWERS, null) ?: return emptyMap()
            return try {
                val obj = JSONObject(raw)
                obj.keys().asSequence().associateWith { key ->
                    val arr = obj.getJSONArray(key)
                    (0 until arr.length()).map { arr.getString(it) }
                }
            } catch (_: Exception) {
                emptyMap()
            }
        }
        set(value) {
            val obj = JSONObject()
            value.forEach { (key, answers) -> obj.put(key, JSONArray(answers)) }
            prefs.edit().putString(KEY_LIFESTYLE_ANSWERS, obj.toString()).apply()
        }

    var presets: List<TimerPreset>
        get() {
            val raw = prefs.getString(KEY_PRESETS, null) ?: return DEFAULT_PRESETS
            return try {
                val array = JSONArray(raw)
                (0 until array.length()).map { i ->
                    val obj = array.getJSONObject(i)
                    TimerPreset(
                        id = obj.getString("id"),
                        label = obj.getString("label"),
                        workMinutes = obj.getInt("workMinutes"),
                        restMinutes = obj.getInt("restMinutes"),
                        comment = obj.optString("comment", "")
                    )
                }
            } catch (_: Exception) {
                DEFAULT_PRESETS
            }
        }
        set(value) {
            val array = JSONArray()
            value.forEach { preset ->
                array.put(
                    JSONObject().apply {
                        put("id", preset.id)
                        put("label", preset.label)
                        put("workMinutes", preset.workMinutes)
                        put("restMinutes", preset.restMinutes)
                        put("comment", preset.comment)
                    }
                )
            }
            prefs.edit().putString(KEY_PRESETS, array.toString()).apply()
        }

    var categories: List<String>
        get() {
            val raw = prefs.getString(KEY_CATEGORIES, null) ?: return DEFAULT_CATEGORIES
            return try {
                val array = JSONArray(raw)
                (0 until array.length()).map { array.getString(it) }
            } catch (_: Exception) {
                DEFAULT_CATEGORIES
            }
        }
        set(value) {
            val array = JSONArray()
            value.forEach { array.put(it) }
            prefs.edit().putString(KEY_CATEGORIES, array.toString()).apply()
        }

    fun getHistory(): List<SessionRecord> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                SessionRecord(
                    id = obj.getLong("id"),
                    phase = obj.getString("phase"),
                    startTimeMillis = obj.getLong("startTimeMillis"),
                    durationSeconds = obj.getInt("durationSeconds"),
                    interrupted = obj.getBoolean("interrupted"),
                    comment = obj.optString("comment", ""),
                    category = obj.optString("category", ""),
                    quote = obj.optString("quote", "")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addHistoryEntry(entry: SessionRecord) {
        val updated = listOf(entry) + getHistory()
        saveHistory(updated.take(MAX_HISTORY_ENTRIES))
    }

    fun updateHistoryComment(id: Long, comment: String) {
        val updated = getHistory().map { if (it.id == id) it.copy(comment = comment) else it }
        saveHistory(updated)
    }

    private fun saveHistory(entries: List<SessionRecord>) {
        val array = JSONArray()
        entries.forEach { entry -> array.put(sessionToJson(entry)) }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    private fun sessionToJson(entry: SessionRecord): JSONObject = JSONObject().apply {
        put("id", entry.id)
        put("phase", entry.phase)
        put("startTimeMillis", entry.startTimeMillis)
        put("durationSeconds", entry.durationSeconds)
        put("interrupted", entry.interrupted)
        put("comment", entry.comment)
        put("category", entry.category)
        put("quote", entry.quote)
    }

    fun exportAllData(): String {
        val root = JSONObject()
        root.put("exportVersion", 1)
        val profile = JSONObject().apply {
            put("userName", userName)
            put("lastName", lastName)
            put("profession", profession)
            put("professionDetails", professionDetails)
            put("focusWindowOneStart", focusWindowOneStart)
            put("focusWindowOneEnd", focusWindowOneEnd)
            put("focusWindowTwoStart", focusWindowTwoStart)
            put("focusWindowTwoEnd", focusWindowTwoEnd)
            put("email", email)
            put("dataConsentGiven", dataConsentGiven)
            put("weightKg", weightKg)
            put("heightCm", heightCm)
            put("age", age)
            put("gender", gender)
            put("maritalStatus", maritalStatus)
            put("wakeTime", wakeTime)
            put("bedTime", bedTime)
            put("isWorking", isWorking)
            put("breakfastTime", breakfastTime)
            put("lunchTime", lunchTime)
            put("dinnerTime", dinnerTime)
            put("workHoursPerDay", workHoursPerDay)
            put("mealsPerDay", mealsPerDay)
            put("waterUnit", waterUnit)
            put("waterCount", waterCount)
        }
        root.put("profile", profile)
        val settings = JSONObject().apply {
            put("workMinutes", workMinutes)
            put("restMinutes", restMinutes)
            put("soundEnabled", soundEnabled)
            put("vibrationEnabled", vibrationEnabled)
            put("keepScreenOn", keepScreenOn)
            put("dynamicColorEnabled", dynamicColorEnabled)
            put("categories", JSONArray(categories))
        }
        root.put("settings", settings)
        val historyArray = JSONArray()
        getHistory().forEach { historyArray.put(sessionToJson(it)) }
        root.put("history", historyArray)
        return root.toString(2)
    }

    fun importAllData(json: String): Boolean {
        return try {
            val root = JSONObject(json)
            root.optJSONObject("profile")?.let { profile ->
                userName = profile.optString("userName", userName)
                lastName = profile.optString("lastName", lastName)
                profession = profile.optString("profession", profession)
                professionDetails = profile.optString("professionDetails", professionDetails)
                focusWindowOneStart = profile.optString("focusWindowOneStart", focusWindowOneStart)
                focusWindowOneEnd = profile.optString("focusWindowOneEnd", focusWindowOneEnd)
                focusWindowTwoStart = profile.optString("focusWindowTwoStart", focusWindowTwoStart)
                focusWindowTwoEnd = profile.optString("focusWindowTwoEnd", focusWindowTwoEnd)
                email = profile.optString("email", email)
                dataConsentGiven = profile.optBoolean("dataConsentGiven", dataConsentGiven)
                weightKg = profile.optString("weightKg", weightKg)
                heightCm = profile.optString("heightCm", heightCm)
                age = profile.optString("age", age)
                gender = profile.optString("gender", gender)
                maritalStatus = profile.optString("maritalStatus", maritalStatus)
                wakeTime = profile.optString("wakeTime", wakeTime)
                bedTime = profile.optString("bedTime", bedTime)
                isWorking = profile.optBoolean("isWorking", isWorking)
                breakfastTime = profile.optString("breakfastTime", breakfastTime)
                lunchTime = profile.optString("lunchTime", lunchTime)
                dinnerTime = profile.optString("dinnerTime", dinnerTime)
                workHoursPerDay = profile.optInt("workHoursPerDay", workHoursPerDay)
                mealsPerDay = profile.optInt("mealsPerDay", mealsPerDay)
                waterUnit = profile.optString("waterUnit", waterUnit)
                waterCount = profile.optInt("waterCount", waterCount)
            }
            root.optJSONObject("settings")?.let { settingsObj ->
                workMinutes = settingsObj.optInt("workMinutes", workMinutes)
                restMinutes = settingsObj.optInt("restMinutes", restMinutes)
                soundEnabled = settingsObj.optBoolean("soundEnabled", soundEnabled)
                vibrationEnabled = settingsObj.optBoolean("vibrationEnabled", vibrationEnabled)
                keepScreenOn = settingsObj.optBoolean("keepScreenOn", keepScreenOn)
                dynamicColorEnabled = settingsObj.optBoolean("dynamicColorEnabled", dynamicColorEnabled)
                settingsObj.optJSONArray("categories")?.let { arr ->
                    categories = (0 until arr.length()).map { arr.getString(it) }
                }
            }
            root.optJSONArray("history")?.let { arr ->
                val imported = (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    SessionRecord(
                        id = obj.getLong("id"),
                        phase = obj.getString("phase"),
                        startTimeMillis = obj.getLong("startTimeMillis"),
                        durationSeconds = obj.getInt("durationSeconds"),
                        interrupted = obj.getBoolean("interrupted"),
                        comment = obj.optString("comment", ""),
                        category = obj.optString("category", ""),
                        quote = obj.optString("quote", "")
                    )
                }
                saveHistory(imported.sortedByDescending { it.startTimeMillis })
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        private const val MAX_HISTORY_ENTRIES = 300
        private const val KEY_NAME = "user_name"
        private const val KEY_PROFESSION = "profession"
        private const val KEY_PROFESSION_DETAILS = "profession_details"
        private const val KEY_FOCUS_W1_START = "focus_w1_start"
        private const val KEY_FOCUS_W1_END = "focus_w1_end"
        private const val KEY_FOCUS_W2_START = "focus_w2_start"
        private const val KEY_FOCUS_W2_END = "focus_w2_end"
        private const val KEY_PHOTO = "photo_uri"
        private const val KEY_WORK_MIN = "work_minutes"
        private const val KEY_REST_MIN = "rest_minutes"
        private const val KEY_SOUND = "sound_enabled"
        private const val KEY_VIBRATION = "vibration_enabled"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color_enabled"
        private const val KEY_TG_ENABLED = "telegram_enabled"
        private const val KEY_TG_TOKEN = "telegram_bot_token"
        private const val KEY_TG_CHAT = "telegram_chat_id"
        private const val KEY_CALL_ENABLED = "auto_call_enabled"
        private const val KEY_CALL_NUMBER = "auto_call_number"
        private const val KEY_EMAIL = "email"
        private const val KEY_WEIGHT = "weight_kg"
        private const val KEY_HEIGHT = "height_cm"
        private const val KEY_AGE = "age"
        private const val KEY_MARITAL = "marital_status"
        private const val KEY_GENDER = "gender"
        private const val KEY_PERSONALITY = "personality_type"
        private const val KEY_NIGHT_WAKE = "night_wake_frequency"
        private const val KEY_WAKE_TIME = "wake_time"
        private const val KEY_BED_TIME = "bed_time"
        private const val KEY_IS_WORKING = "is_working"
        private const val KEY_LAST_NAME = "last_name"
        private const val KEY_DATA_CONSENT = "data_consent_given"
        private const val KEY_STEPS_ENABLED = "steps_enabled"
        private const val KEY_STEPS_BASELINE_DATE = "steps_baseline_date"
        private const val KEY_STEPS_BASELINE_COUNT = "steps_baseline_count"
        private const val KEY_VOICE_ANNOUNCE_ENABLED = "voice_announce_enabled"
        private const val KEY_VOICE_ANNOUNCE_VALUE = "voice_announce_value"
        private const val KEY_VOICE_ANNOUNCE_UNIT = "voice_announce_unit"
        private const val KEY_VOICE_LANGUAGE = "voice_language"
        private const val KEY_ELEVEN_ENABLED = "eleven_labs_enabled"
        private const val KEY_ELEVEN_API_KEY = "eleven_labs_api_key"
        private const val KEY_ELEVEN_VOICE_MALE = "eleven_labs_voice_male"
        private const val KEY_ELEVEN_VOICE_FEMALE = "eleven_labs_voice_female"
        private const val DEFAULT_ELEVEN_VOICE_MALE = "pNInz6obpgDQGcFmaJgB"
        private const val DEFAULT_ELEVEN_VOICE_FEMALE = "21m00Tcm4TlvDq8ikWAM"
        private const val KEY_HISTORY = "session_history"
        private const val KEY_CATEGORIES = "categories"
        private const val KEY_PASSWORD_HASH = "account_password_hash"
        private const val KEY_IS_REGISTERED = "is_registered"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_IS_ONBOARDED = "is_onboarded"
        private const val KEY_BREAKFAST_TIME = "breakfast_time"
        private const val KEY_LUNCH_TIME = "lunch_time"
        private const val KEY_DINNER_TIME = "dinner_time"
        private const val KEY_WORK_HOURS_PER_DAY = "work_hours_per_day"
        private const val KEY_MEALS_PER_DAY = "meals_per_day"
        private const val KEY_WATER_UNIT = "water_unit"
        private const val KEY_WATER_COUNT = "water_count"
        private const val KEY_PRESETS = "timer_presets"
        private const val KEY_DAY_SCHEDULE = "day_schedule"
        private const val KEY_DAY_PLAN = "day_plan"
        private const val KEY_LIFESTYLE_ANSWERS = "lifestyle_answers"
        private const val KEY_ROUTINE_TASKS = "routine_tasks"
        private const val KEY_ROUTINE_COMPLETIONS = "routine_completions"
        private const val KEY_DAY_SUMMARIES = "day_summaries"
        private const val KEY_PLAN_ITEMS = "plan_items"
        private const val KEY_DAY_TASKS = "day_tasks"
        private const val KEY_NOTES = "notes"
        private const val KEY_JIRA_ENABLED = "jira_enabled"
        private const val KEY_JIRA_SITE = "jira_site"
        private const val KEY_JIRA_EMAIL = "jira_email"
        private const val KEY_JIRA_TOKEN = "jira_token"
        private const val KEY_JIRA_JQL = "jira_jql"
        private const val KEY_JIRA_ISSUES = "jira_issues"
        private const val KEY_JIRA_SYNCED_AT = "jira_synced_at"
        private const val KEY_PLAN_COMPLETIONS = "plan_completions"
        private const val KEY_PLANS_MIGRATED = "plans_migrated_v2"
        private const val KEY_WORD_SEEN = "word_seen_date"
        private const val KEY_PLAN_TEMPLATES = "plan_templates"
        private const val KEY_ACTIVE_PLAN_TEMPLATE = "active_plan_template_id"
        private const val KEY_PLAN_HISTORY = "plan_history"
    }
}

/** A date key N days back, in the same "yyyy-MM-dd" shape the day-keyed maps use. */
private fun dateKeyDaysAgo(days: Int): String {
    val calendar = java.util.Calendar.getInstance()
    calendar.add(java.util.Calendar.DAY_OF_MONTH, -days)
    return "%04d-%02d-%02d".format(
        calendar.get(java.util.Calendar.YEAR),
        calendar.get(java.util.Calendar.MONTH) + 1,
        calendar.get(java.util.Calendar.DAY_OF_MONTH)
    )
}
