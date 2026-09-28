package com.focustimer.app.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.focustimer.app.PrefsManager

private val MARITAL_OPTIONS = listOf(
    "Не женат / не замужем",
    "В отношениях",
    "Женат / замужем",
    "Разведён(а)",
    "Вдовец / вдова"
)

private val GENDER_OPTIONS = listOf(
    "Мужской",
    "Женский"
)

@Composable
fun ProfileScreen(
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
                text = { Text("Профиль") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("График дня") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Рекомендации") }
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> ProfileFormTab()
                1 -> ScheduleTab(onOpenLifestyle = onOpenLifestyle, onOpenDetails = onOpenDetails)
                2 -> RecommendationsTab()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileFormTab() {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }

    var name by remember { mutableStateOf(prefs.userName) }
    var lastName by remember { mutableStateOf(prefs.lastName) }
    var photoUri by remember { mutableStateOf(prefs.photoUri?.let { Uri.parse(it) }) }
    var email by remember { mutableStateOf(prefs.email) }
    var dataConsent by remember { mutableStateOf(prefs.dataConsentGiven) }
    var weightKg by remember { mutableStateOf(prefs.weightKg) }
    var heightCm by remember { mutableStateOf(prefs.heightCm) }
    var age by remember { mutableStateOf(prefs.age) }
    var maritalStatus by remember { mutableStateOf(prefs.maritalStatus.ifBlank { MARITAL_OPTIONS[0] }) }
    var gender by remember { mutableStateOf(prefs.gender.ifBlank { GENDER_OPTIONS[0] }) }
    var wakeTime by remember { mutableStateOf(prefs.wakeTime) }
    var bedTime by remember { mutableStateOf(prefs.bedTime) }
    var isWorking by remember { mutableStateOf(prefs.isWorking) }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }
            photoUri = uri
            prefs.photoUri = uri.toString()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 24.dp)
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable {
                    pickImage.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (photoUri != null) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = "Фото профиля",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Добавить фото",
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                prefs.userName = it
            },
            label = { Text("Имя") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = lastName,
            onValueChange = {
                lastName = it
                prefs.lastName = it
            },
            label = { Text("Фамилия") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                prefs.email = it
            },
            label = { Text("Почта") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .clickable {
                    dataConsent = !dataConsent
                    prefs.dataConsentGiven = dataConsent
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = dataConsent,
                onCheckedChange = {
                    dataConsent = it
                    prefs.dataConsentGiven = it
                }
            )
            Text("Согласен(на) на обработку персональных данных")
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = weightKg,
                onValueChange = {
                    weightKg = it
                    prefs.weightKg = it
                },
                label = { Text("Вес, кг") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = heightCm,
                onValueChange = {
                    heightCm = it
                    prefs.heightCm = it
                },
                label = { Text("Рост, см") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = age,
                onValueChange = {
                    age = it
                    prefs.age = it
                },
                label = { Text("Возраст") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        DropdownField(
            label = "Пол",
            selected = gender,
            options = GENDER_OPTIONS,
            onSelected = {
                gender = it
                prefs.gender = it
            },
            modifier = Modifier.padding(top = 16.dp)
        )

        DropdownField(
            label = "Семейное положение",
            selected = maritalStatus,
            options = MARITAL_OPTIONS,
            onSelected = {
                maritalStatus = it
                prefs.maritalStatus = it
            },
            modifier = Modifier.padding(top = 16.dp)
        )

        TimePickerRow(
            label = "Обычно встаю",
            timeText = wakeTime,
            onTimeChange = {
                wakeTime = it
                prefs.wakeTime = it
            }
        )
        TimePickerRow(
            label = "Обычно ложусь",
            timeText = bedTime,
            onTimeChange = {
                bedTime = it
                prefs.bedTime = it
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Сейчас работаю")
            Switch(
                checked = isWorking,
                onCheckedChange = {
                    isWorking = it
                    prefs.isWorking = it
                }
            )
        }

    }
}

@Composable
private fun ScheduleTab(onOpenLifestyle: () -> Unit, onOpenDetails: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Ваш график дня", style = MaterialTheme.typography.headlineMedium)
        if (prefs.daySchedule.isBlank()) {
            Text(
                "График ещё не сгенерирован. Пройдите анкету образа жизни — она подберёт график под ваш сон, приёмы пищи и работу.",
                modifier = Modifier.padding(top = 12.dp)
            )
        } else {
            Text(
                prefs.daySchedule,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )
        }

        // Setup no longer walks through these, so this is where they are reached from.
        Button(
            onClick = onOpenLifestyle,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
        ) {
            Text(if (prefs.daySchedule.isBlank()) "Пройти анкету образа жизни" else "Пересобрать график")
        }
        OutlinedButton(
            onClick = onOpenDetails,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text("Подробная анкета о себе")
        }
    }
}

@Composable
private fun RecommendationsTab() {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }
    val advice = remember {
        buildAdvice(prefs.weightKg, prefs.heightCm, prefs.age, prefs.gender, prefs.personalityType)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Персональные рекомендации", style = MaterialTheme.typography.headlineMedium)
        Column(modifier = Modifier.padding(top = 12.dp)) {
            advice.forEach { tip ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("• ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(tip)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    label: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.exposedDropdownSize()
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerRow(label: String, timeText: String, onTimeChange: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { showDialog = true },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Text(timeText, style = MaterialTheme.typography.titleMedium)
    }

    if (showDialog) {
        val (initialHour, initialMinute) = parseTime(timeText)
        val state = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    onTimeChange("%02d:%02d".format(state.hour, state.minute))
                    showDialog = false
                }) { Text("ОК") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Отмена") }
            },
            text = { TimePicker(state = state) }
        )
    }
}

fun parseTime(text: String): Pair<Int, Int> {
    val parts = text.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 7
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return hour to minute
}
