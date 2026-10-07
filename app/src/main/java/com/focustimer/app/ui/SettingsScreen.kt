package com.focustimer.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.focustimer.app.JiraClient
import com.focustimer.app.JiraResult
import com.focustimer.app.PrefsManager
import com.focustimer.app.ui.theme.ThemeState
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Основные настройки") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Активность") }
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> GeneralSettingsTab()
                1 -> ActivitySettingsTab()
            }
        }
    }
}

@Composable
private fun GeneralSettingsTab() {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }

    var soundEnabled by remember { mutableStateOf(prefs.soundEnabled) }
    var vibrationEnabled by remember { mutableStateOf(prefs.vibrationEnabled) }
    var keepScreenOn by remember { mutableStateOf(prefs.keepScreenOn) }
    var jiraEnabled by remember { mutableStateOf(prefs.jiraEnabled) }
    var jiraSite by remember { mutableStateOf(prefs.jiraSite) }
    var jiraEmail by remember { mutableStateOf(prefs.jiraEmail) }
    var jiraToken by remember { mutableStateOf(prefs.jiraToken) }
    var jiraJql by remember { mutableStateOf(prefs.jiraJql) }
    var jiraChecking by remember { mutableStateOf(false) }
    var jiraCheck by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var dynamicColor by remember { mutableStateOf(prefs.dynamicColorEnabled) }

    var telegramEnabled by remember { mutableStateOf(prefs.telegramEnabled) }
    var telegramToken by remember { mutableStateOf(prefs.telegramBotToken) }
    var telegramChatId by remember { mutableStateOf(prefs.telegramChatId) }

    var autoCallEnabled by remember { mutableStateOf(prefs.autoCallEnabled) }
    var autoCallNumber by remember { mutableStateOf(prefs.autoCallNumber) }

    var voiceAnnounceEnabled by remember { mutableStateOf(prefs.voiceAnnounceEnabled) }
    var voiceAnnounceValue by remember { mutableStateOf(prefs.voiceAnnounceLeadValue.toString()) }
    var voiceAnnounceUnit by remember { mutableStateOf(prefs.voiceAnnounceUnit) }
    var voiceLanguage by remember { mutableStateOf(prefs.voiceLanguage) }

    var elevenLabsEnabled by remember { mutableStateOf(prefs.elevenLabsEnabled) }
    var elevenLabsApiKey by remember { mutableStateOf(prefs.elevenLabsApiKey) }

    var importMessage by remember { mutableStateOf<String?>(null) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(prefs.exportAllData().toByteArray())
                }
                importMessage = "Данные экспортированы"
            } catch (_: Exception) {
                importMessage = "Не удалось сохранить файл"
            }
        }
    }

    val importPickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
        }
    }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        autoCallEnabled = granted
        prefs.autoCallEnabled = granted
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        SettingRow("Звук по окончании этапа", soundEnabled) {
            soundEnabled = it
            prefs.soundEnabled = it
        }
        SettingRow("Вибрация по окончании этапа", vibrationEnabled) {
            vibrationEnabled = it
            prefs.vibrationEnabled = it
        }
        SettingRow("Не выключать экран во время таймера", keepScreenOn) {
            keepScreenOn = it
            prefs.keepScreenOn = it
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SettingRow("Цвета из обоев системы", dynamicColor) {
                dynamicColor = it
                prefs.dynamicColorEnabled = it
                // ThemeState is what the theme observes, so the repaint is immediate.
                ThemeState.dynamicColor = it
            }
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))
        Text(
            "Если пропустили окно таймера",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            "Если через 1 минуту после смены этапа вы не открыли приложение — сработает выбранное ниже",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        SettingRow("Сообщение в Telegram", telegramEnabled) {
            telegramEnabled = it
            prefs.telegramEnabled = it
        }
        if (telegramEnabled) {
            OutlinedTextField(
                value = telegramToken,
                onValueChange = {
                    telegramToken = it
                    prefs.telegramBotToken = it
                },
                label = { Text("Токен бота (от @BotFather)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
            OutlinedTextField(
                value = telegramChatId,
                onValueChange = {
                    telegramChatId = it
                    prefs.telegramChatId = it
                },
                label = { Text("Chat ID") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp)
            )
        }

        SettingRow("Автозвонок", autoCallEnabled) { checked ->
            if (checked) {
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.CALL_PHONE
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    autoCallEnabled = true
                    prefs.autoCallEnabled = true
                } else {
                    callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                }
            } else {
                autoCallEnabled = false
                prefs.autoCallEnabled = false
            }
        }
        if (autoCallEnabled) {
            OutlinedTextField(
                value = autoCallNumber,
                onValueChange = {
                    autoCallNumber = it
                    prefs.autoCallNumber = it
                },
                label = { Text("Номер телефона") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))
        Text("Голосовое предупреждение", style = MaterialTheme.typography.titleMedium)
        Text(
            "Голосом предупредит о приближении смены этапа заранее",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
        SettingRow("Озвучивать приближение конца этапа", voiceAnnounceEnabled) {
            voiceAnnounceEnabled = it
            prefs.voiceAnnounceEnabled = it
        }
        if (voiceAnnounceEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = voiceAnnounceValue,
                    onValueChange = { input ->
                        voiceAnnounceValue = input
                        input.toIntOrNull()?.let { value ->
                            if (value in 1..600) prefs.voiceAnnounceLeadValue = value
                        }
                    },
                    label = { Text("За сколько") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                DropdownField(
                    label = "Единицы",
                    selected = voiceAnnounceUnit,
                    options = listOf("Секунды", "Минуты"),
                    onSelected = {
                        voiceAnnounceUnit = it
                        prefs.voiceAnnounceUnit = it
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            DropdownField(
                label = "Язык озвучки",
                selected = voiceLanguage,
                options = listOf("Русский", "English"),
                onSelected = {
                    voiceLanguage = it
                    prefs.voiceLanguage = it
                }
            )

            SettingRow("Реалистичный голос (ElevenLabs)", elevenLabsEnabled) {
                elevenLabsEnabled = it
                prefs.elevenLabsEnabled = it
            }
            Text(
                "Нужен интернет и свой API-ключ с elevenlabs.io. Без ключа или без сети озвучка " +
                    "автоматически вернётся к обычному голосу устройства.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (elevenLabsEnabled) {
                OutlinedTextField(
                    value = elevenLabsApiKey,
                    onValueChange = {
                        elevenLabsApiKey = it
                        prefs.elevenLabsApiKey = it
                    },
                    label = { Text("API-ключ ElevenLabs") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
            }
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))
        Text("Jira", style = MaterialTheme.typography.titleMedium)
        SettingRow("Показывать задачи из Jira", jiraEnabled) {
            jiraEnabled = it
            prefs.jiraEnabled = it
        }
        if (jiraEnabled) {
            Text(
                "Нужен API-токен Atlassian: id.atlassian.net → Security → Create API token. " +
                    "Токен даёт полный доступ к вашему Jira, хранится только на этом устройстве " +
                    "и в резервную копию не попадает.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = jiraSite,
                onValueChange = { jiraSite = it; prefs.jiraSite = it },
                label = { Text("Адрес сайта") },
                placeholder = { Text("example.atlassian.net") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = jiraEmail,
                onValueChange = { jiraEmail = it; prefs.jiraEmail = it },
                label = { Text("Почта аккаунта") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
            OutlinedTextField(
                value = jiraToken,
                onValueChange = { jiraToken = it; prefs.jiraToken = it },
                label = { Text("API-токен") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
            OutlinedTextField(
                value = jiraJql,
                onValueChange = { jiraJql = it; prefs.jiraJql = it },
                label = { Text("Запрос JQL") },
                minLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
            Text(
                "Фильтр по категории статуса захватывает всё «в процессе», включая отложенное, " +
                    "поэтому по умолчанию стоит точный статус.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Button(
                onClick = {
                    jiraChecking = true
                    jiraCheck = null
                    scope.launch {
                        jiraCheck = when (
                            val r = JiraClient.fetch(prefs.jiraSite, prefs.jiraEmail, prefs.jiraToken, prefs.jiraJql)
                        ) {
                            is JiraResult.Ok -> {
                                prefs.jiraIssues = r.issues
                                prefs.jiraSyncedAtMillis = System.currentTimeMillis()
                                "Связь есть: задач — ${r.issues.size}"
                            }
                            is JiraResult.Failed -> r.message
                        }
                        jiraChecking = false
                    }
                },
                enabled = !jiraChecking,
                modifier = Modifier.padding(top = 10.dp)
            ) {
                Text(if (jiraChecking) "Проверяю…" else "Проверить связь")
            }
            jiraCheck?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))
        Text("Экспорт и бэкап", style = MaterialTheme.typography.titleMedium)
        Text(
            "Все данные хранятся только на этом устройстве — сохраните файл, чтобы не потерять историю",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { exportLauncher.launch("focus-timer-backup.json") }) {
                Text("Экспортировать")
            }
            OutlinedButton(onClick = { importPickLauncher.launch("application/json") }) {
                Text("Импортировать")
            }
        }
        importMessage?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }

    if (pendingImportUri != null) {
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("Импортировать данные?") },
            text = { Text("Текущий профиль, настройки и история будут заменены содержимым файла.") },
            confirmButton = {
                TextButton(onClick = {
                    val uri = pendingImportUri
                    pendingImportUri = null
                    if (uri != null) {
                        val text = try {
                            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        } catch (_: Exception) {
                            null
                        }
                        val success = text != null && prefs.importAllData(text)
                        importMessage = if (success) "Данные импортированы" else "Не удалось прочитать файл"
                    }
                }) { Text("Импортировать") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun ActivitySettingsTab() {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }

    var stepsEnabled by remember { mutableStateOf(prefs.stepsEnabled) }
    var categories by remember { mutableStateOf(prefs.categories) }
    var newCategory by remember { mutableStateOf("") }

    val activityRecognitionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        stepsEnabled = granted
        prefs.stepsEnabled = granted
    }

    val liveSteps = rememberLiveStepCount(stepsEnabled)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        SettingRow("Счётчик шагов", stepsEnabled) { checked ->
            if (checked) {
                val needsRuntimePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                val granted = !needsRuntimePermission || ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    stepsEnabled = true
                    prefs.stepsEnabled = true
                } else {
                    activityRecognitionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                }
            } else {
                stepsEnabled = false
                prefs.stepsEnabled = false
            }
        }
        if (stepsEnabled) {
            Text(
                liveSteps?.let { "Шагов сегодня: $it" } ?: "Считаем шаги…",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))
        Text("Категории активности", style = MaterialTheme.typography.titleMedium)
        Text(
            "Выбираются на экране таймера и видны в истории",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
        categories.forEach { category ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(category)
                IconButton(onClick = {
                    categories = categories - category
                    prefs.categories = categories
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Удалить $category")
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newCategory,
                onValueChange = { newCategory = it },
                label = { Text("Новая категория") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = {
                    val trimmed = newCategory.trim()
                    if (trimmed.isNotEmpty() && trimmed !in categories) {
                        categories = categories + trimmed
                        prefs.categories = categories
                    }
                    newCategory = ""
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Добавить")
            }
        }
    }
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.padding(end = 16.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
