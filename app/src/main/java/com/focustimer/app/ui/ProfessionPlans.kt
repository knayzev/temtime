package com.focustimer.app.ui

/**
 * Profession-aware day plans plus the reasoning shown next to each block.
 *
 * Hints carry a real citation rather than a bare claim. Links point at a PubMed title search
 * instead of a hard-coded article id — the search resolves to the paper and keeps working even
 * if an id changes, and it avoids shipping a guessed URL.
 */

private fun pubmed(title: String): String =
    "https://pubmed.ncbi.nlm.nih.gov/?term=" + title.trim().replace(" ", "+")

data class PlanHint(
    val text: String,
    val source: String = "",
    val url: String = ""
)

data class ProfessionPlanItem(
    val title: String,
    val durationMinutes: Int,
    val hint: PlanHint? = null
)

data class ProfessionPreset(
    val id: String,
    val name: String,
    val emoji: String,
    val summary: String,
    val items: List<ProfessionPlanItem>
)

// ---------------------------------------------------------------------------
// Shared reasoning, reused across presets so the same claim always cites the
// same source.
// ---------------------------------------------------------------------------

val HINT_PEAK_FOCUS = PlanHint(
    "Самую трудную задачу ставьте в первые 2–3 часа после пробуждения — внимание и рабочая " +
        "память в это время обычно на суточном пике.",
    "Schmidt, Collette, Cajochen, Peigneux, Cognitive Neuropsychology, 2007",
    pubmed("A time to think circadian rhythms in human cognition")
)

val HINT_WORK_BLOCK = PlanHint(
    "Работайте блоками по 45–60 минут с короткими перерывами: даже краткое переключение " +
        "восстанавливает внимание и мешает ему «выгорать» на длинной задаче.",
    "Ariga, Lleras, Cognition, 2011",
    pubmed("Brief and rare mental breaks keep you focused")
)

val HINT_MORNING_LIGHT = PlanHint(
    "Дневной свет в первый час после подъёма подстраивает циркадные ритмы — вечером легче " +
        "уснуть, утром легче встать.",
    "Blume, Garbazza, Spitschan, Somnologie, 2019",
    pubmed("Effects of light on human circadian rhythms sleep and mood")
)

val HINT_POST_LUNCH = PlanHint(
    "После обеда закономерно проседает бодрость. Ставьте сюда рутину и встречи, а не задачи, " +
        "требующие максимальной концентрации.",
    "Monk, Clinics in Sports Medicine, 2005",
    pubmed("The post-lunch dip in performance")
)

val HINT_WALK = PlanHint(
    "Прогулка заметно повышает продуктивность мышления — эффект сохраняется и после того, как " +
        "вы сели обратно за стол.",
    "Oppezzo, Schwartz, J. Experimental Psychology: LMC, 2014",
    pubmed("Give your ideas some legs the positive effect of walking on creative thinking")
)

val HINT_CAFFEINE = PlanHint(
    "Кофеин за 6 часов до сна всё ещё сокращает сон примерно на час, даже если субъективно вы " +
        "этого не замечаете.",
    "Drake et al., Journal of Clinical Sleep Medicine, 2013",
    pubmed("Caffeine effects on sleep taken 0 3 or 6 hours before going to bed")
)

val HINT_SCREEN_NIGHT = PlanHint(
    "Экран перед сном сдвигает выработку мелатонина и делает утреннюю бодрость хуже — уберите " +
        "телефон хотя бы за час.",
    "Chang, Aeschbach, Duffy, Czeisler, PNAS, 2015",
    pubmed("Evening use of light-emitting eReaders negatively affects sleep")
)

val HINT_WARM_SHOWER = PlanHint(
    "Тёплый душ за 1–2 часа до сна ускоряет засыпание: тело активнее сбрасывает температуру, а " +
        "это сигнал ко сну.",
    "Haghayegh et al., Sleep Medicine Reviews, 2019",
    pubmed("Before-bedtime passive body heating by warm shower or bath to improve sleep")
)

val HINT_PLAN_TOMORROW = PlanHint(
    "Выписать завтрашние дела перед сном — засыпание ускоряется: чем конкретнее список, тем " +
        "сильнее эффект.",
    "Scullin et al., J. Experimental Psychology: General, 2018",
    pubmed("The effects of bedtime writing on difficulty falling asleep")
)

val HINT_REGULARITY = PlanHint(
    "Одно и то же время подъёма и отбоя важнее общей длительности сна — нерегулярный график " +
        "бьёт по результатам сильнее, чем недосып сам по себе.",
    "Phillips et al., Scientific Reports, 2017",
    pubmed("Irregular sleep wake patterns are associated with poorer academic performance")
)

val HINT_EXERCISE = PlanHint(
    "Регулярная нагрузка улучшает сон; интенсивная тренировка ближе чем за час до сна, наоборот, " +
        "мешает уснуть.",
    "Stutz, Eiholzer, Spengler, Sports Medicine, 2019",
    pubmed("Effects of evening exercise on sleep in healthy participants")
)

// ---------------------------------------------------------------------------
// Presets
// ---------------------------------------------------------------------------

private val DEEP_WORK_CORE = listOf(
    ProfessionPlanItem("Глубокая работа: главная задача дня", 90, HINT_PEAK_FOCUS),
    ProfessionPlanItem("Перерыв без экрана", 15, HINT_WORK_BLOCK),
    ProfessionPlanItem("Глубокая работа: продолжение", 60, HINT_WORK_BLOCK)
)

val PROFESSION_PRESETS = listOf(
    ProfessionPreset(
        "dev", "Разработчик", "💻",
        "Длинные непрерывные блоки кода утром, разборы и ревью — во второй половине дня.",
        DEEP_WORK_CORE + listOf(
            ProfessionPlanItem("Разбор почты, задач и сообщений", 30),
            ProfessionPlanItem("Обед", 45, HINT_POST_LUNCH),
            ProfessionPlanItem("Код-ревью и обсуждения", 60, HINT_POST_LUNCH),
            ProfessionPlanItem("Прогулка", 20, HINT_WALK),
            ProfessionPlanItem("Мелкие задачи и правки", 60),
            ProfessionPlanItem("Итоги дня и план на завтра", 15, HINT_PLAN_TOMORROW)
        )
    ),
    ProfessionPreset(
        "design", "Дизайнер", "🎨",
        "Творческие блоки без прерываний, сбор референсов и правки — ближе к вечеру.",
        listOf(
            ProfessionPlanItem("Насмотренность: референсы и идеи", 30),
            ProfessionPlanItem("Основной блок: макеты", 90, HINT_PEAK_FOCUS),
            ProfessionPlanItem("Перерыв, прогулка", 20, HINT_WALK),
            ProfessionPlanItem("Обед", 45, HINT_POST_LUNCH),
            ProfessionPlanItem("Правки по фидбеку", 60, HINT_POST_LUNCH),
            ProfessionPlanItem("Созвоны и презентация работ", 45),
            ProfessionPlanItem("Итоги дня и план на завтра", 15, HINT_PLAN_TOMORROW)
        )
    ),
    ProfessionPreset(
        "manager", "Руководитель", "🧭",
        "Встречи собраны в один блок, утро оставлено под решения, которые нельзя делегировать.",
        listOf(
            ProfessionPlanItem("Приоритеты дня, без почты", 20, HINT_PEAK_FOCUS),
            ProfessionPlanItem("Стратегическая задача", 60, HINT_PEAK_FOCUS),
            ProfessionPlanItem("Блок встреч", 120),
            ProfessionPlanItem("Обед", 45, HINT_POST_LUNCH),
            ProfessionPlanItem("Один на один с командой", 60, HINT_POST_LUNCH),
            ProfessionPlanItem("Разбор почты и решений", 45),
            ProfessionPlanItem("Прогулка", 20, HINT_WALK),
            ProfessionPlanItem("Итоги дня и план на завтра", 15, HINT_PLAN_TOMORROW)
        )
    ),
    ProfessionPreset(
        "sales", "Продажи", "📞",
        "Звонки в часы, когда клиенты доступнее всего; подготовка и CRM — вокруг них.",
        listOf(
            ProfessionPlanItem("Подготовка списка и скриптов", 30),
            ProfessionPlanItem("Блок звонков", 90, HINT_PEAK_FOCUS),
            ProfessionPlanItem("Перерыв", 15, HINT_WORK_BLOCK),
            ProfessionPlanItem("Обед", 45, HINT_POST_LUNCH),
            ProfessionPlanItem("Второй блок звонков", 90),
            ProfessionPlanItem("CRM, письма, follow-up", 45, HINT_POST_LUNCH),
            ProfessionPlanItem("Разбор результатов дня", 20, HINT_PLAN_TOMORROW)
        )
    ),
    ProfessionPreset(
        "marketing", "Маркетолог", "📈",
        "Аналитика и тексты — на пике внимания, запуски и согласования — после обеда.",
        listOf(
            ProfessionPlanItem("Метрики за вчера", 20),
            ProfessionPlanItem("Тексты и креативы", 90, HINT_PEAK_FOCUS),
            ProfessionPlanItem("Перерыв, прогулка", 20, HINT_WALK),
            ProfessionPlanItem("Обед", 45, HINT_POST_LUNCH),
            ProfessionPlanItem("Запуски и согласования", 60, HINT_POST_LUNCH),
            ProfessionPlanItem("Аналитика кампаний", 60),
            ProfessionPlanItem("Итоги дня и план на завтра", 15, HINT_PLAN_TOMORROW)
        )
    ),
    ProfessionPreset(
        "student", "Студент", "🎓",
        "Учебные блоки с активным повторением, тяжёлые предметы — в первой половине дня.",
        listOf(
            ProfessionPlanItem("Сложный предмет", 60, HINT_PEAK_FOCUS),
            ProfessionPlanItem("Перерыв", 15, HINT_WORK_BLOCK),
            ProfessionPlanItem("Второй блок учёбы", 60, HINT_WORK_BLOCK),
            ProfessionPlanItem("Обед", 45, HINT_POST_LUNCH),
            ProfessionPlanItem("Повторение пройденного", 45, HINT_POST_LUNCH),
            ProfessionPlanItem("Прогулка или спорт", 40, HINT_EXERCISE),
            ProfessionPlanItem("Домашние задания", 60),
            ProfessionPlanItem("План на завтра", 15, HINT_PLAN_TOMORROW)
        )
    ),
    ProfessionPreset(
        "founder", "Предприниматель", "🚀",
        "Одна задача, двигающая бизнес, — до всех входящих. Операционка собрана в блок.",
        listOf(
            ProfessionPlanItem("Задача, двигающая бизнес", 90, HINT_PEAK_FOCUS),
            ProfessionPlanItem("Перерыв", 15, HINT_WORK_BLOCK),
            ProfessionPlanItem("Встречи и переговоры", 90),
            ProfessionPlanItem("Обед", 45, HINT_POST_LUNCH),
            ProfessionPlanItem("Операционка и команда", 90, HINT_POST_LUNCH),
            ProfessionPlanItem("Прогулка", 20, HINT_WALK),
            ProfessionPlanItem("Цифры и итоги дня", 20, HINT_PLAN_TOMORROW)
        )
    ),
    ProfessionPreset(
        "freelance", "Фрилансер", "🧑‍💻",
        "Клиентская работа в защищённых блоках, коммуникация — в отведённые окна.",
        listOf(
            ProfessionPlanItem("Клиентская работа: проект №1", 90, HINT_PEAK_FOCUS),
            ProfessionPlanItem("Перерыв", 15, HINT_WORK_BLOCK),
            ProfessionPlanItem("Окно для сообщений клиентам", 30),
            ProfessionPlanItem("Обед", 45, HINT_POST_LUNCH),
            ProfessionPlanItem("Клиентская работа: проект №2", 90, HINT_POST_LUNCH),
            ProfessionPlanItem("Поиск заказов, счета", 45),
            ProfessionPlanItem("Итоги дня и план на завтра", 15, HINT_PLAN_TOMORROW)
        )
    ),
    ProfessionPreset(
        "other", "Другое", "🗂️",
        "Универсальный каркас: главный блок утром, рутина после обеда, разбор вечером.",
        listOf(
            ProfessionPlanItem("Главная задача дня", 90, HINT_PEAK_FOCUS),
            ProfessionPlanItem("Перерыв", 15, HINT_WORK_BLOCK),
            ProfessionPlanItem("Рабочий блок", 60, HINT_WORK_BLOCK),
            ProfessionPlanItem("Обед", 45, HINT_POST_LUNCH),
            ProfessionPlanItem("Текущие дела", 60, HINT_POST_LUNCH),
            ProfessionPlanItem("Прогулка", 20, HINT_WALK),
            ProfessionPlanItem("Итоги дня и план на завтра", 15, HINT_PLAN_TOMORROW)
        )
    )
)

fun presetForProfession(profession: String): ProfessionPreset =
    PROFESSION_PRESETS.firstOrNull { it.name.equals(profession, ignoreCase = true) }
        ?: PROFESSION_PRESETS.firstOrNull { it.id == profession }
        ?: PROFESSION_PRESETS.last()

/**
 * Extra context the user typed is appended as its own block rather than mixed into the preset,
 * so it is visible and easy to edit instead of silently changing the generated plan.
 */
fun planItemsFor(profession: String, details: String): List<ProfessionPlanItem> {
    val preset = presetForProfession(profession)
    val trimmed = details.trim()
    if (trimmed.isEmpty()) return preset.items
    return preset.items + ProfessionPlanItem(trimmed, 30)
}

/** Hints attached to the daily rituals, shown the same way as plan hints. */
val RITUAL_HINTS: Map<String, PlanHint> = mapOf(
    "routine_water" to PlanHint("Стакан воды сразу после подъёма восполняет потерю жидкости за ночь и помогает проснуться."),
    "routine_exercise" to HINT_EXERCISE,
    "routine_walk" to HINT_WALK,
    "routine_daywalk" to HINT_MORNING_LIGHT,
    "routine_deepwork" to HINT_PEAK_FOCUS,
    "routine_break" to HINT_WORK_BLOCK,
    "routine_lunch" to HINT_POST_LUNCH,
    "routine_planned" to HINT_PLAN_TOMORROW,
    "routine_brain_dump" to HINT_PLAN_TOMORROW,
    "routine_noscreen" to HINT_SCREEN_NIGHT,
    "routine_warmshower" to HINT_WARM_SHOWER,
    "routine_wake7" to HINT_REGULARITY,
    "routine_bed" to HINT_REGULARITY
)
