package com.focustimer.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.focustimer.app.PrefsManager

private val MARITAL_OPTIONS_ONBOARDING = listOf(
    "Не женат / не замужем",
    "В отношениях",
    "Женат / замужем",
    "Разведён(а)",
    "Вдовец / вдова"
)

private val GENDER_OPTIONS_ONBOARDING = listOf("Мужской", "Женский")

private val PERSONALITY_OPTIONS = listOf("Интроверт", "Экстраверт", "Амбиверт")

private val NIGHT_WAKE_OPTIONS = listOf("Не просыпаюсь", "1 раз", "2–3 раза", "Часто (4+)")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }

    var userName by remember { mutableStateOf(prefs.userName) }
    var profession by remember {
        mutableStateOf(prefs.profession.ifBlank { PROFESSION_PRESETS.first().name })
    }
    var professionDetails by remember { mutableStateOf(prefs.professionDetails) }

    var weightKg by remember { mutableStateOf(prefs.weightKg) }
    var heightCm by remember { mutableStateOf(prefs.heightCm) }
    var age by remember { mutableStateOf(prefs.age) }
    var gender by remember { mutableStateOf(prefs.gender.ifBlank { GENDER_OPTIONS_ONBOARDING[0] }) }
    var maritalStatus by remember { mutableStateOf(prefs.maritalStatus.ifBlank { MARITAL_OPTIONS_ONBOARDING[0] }) }
    var personalityType by remember { mutableStateOf(prefs.personalityType.ifBlank { PERSONALITY_OPTIONS[2] }) }
    var nightWakeFrequency by remember { mutableStateOf(prefs.nightWakeFrequency.ifBlank { NIGHT_WAKE_OPTIONS[0] }) }

    var bedTime by remember { mutableStateOf(prefs.bedTime) }
    var wakeTime by remember { mutableStateOf(prefs.wakeTime) }
    var breakfastTime by remember { mutableStateOf(prefs.breakfastTime) }
    var lunchTime by remember { mutableStateOf(prefs.lunchTime) }
    var dinnerTime by remember { mutableStateOf(prefs.dinnerTime) }
    var mealsPerDay by remember { mutableStateOf(prefs.mealsPerDay) }
    var waterUnit by remember { mutableStateOf(prefs.waterUnit) }
    var waterCount by remember { mutableStateOf(prefs.waterCount) }
    var workHours by remember { mutableStateOf(prefs.workHoursPerDay) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        StepperHeader(
            currentStep = 1,
            labels = listOf("О себе", "План на день", "Готово"),
            modifier = Modifier.padding(bottom = 20.dp)
        )

        HeroGlyph(emoji = "🙋", size = 72.dp)

        Text(
            "Расскажите о себе и своём дне",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            "Это поможет предложить точный график дня и советы",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        SectionTitle("Как вас зовут")
        OutlinedTextField(
            value = userName,
            onValueChange = { userName = it; prefs.userName = it },
            label = { Text("Имя") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        )

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        SectionTitle("Чем вы занимаетесь")
        Text(
            "По профессии подберём план дня: что ставить на пик концентрации, а что — после обеда",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        DropdownField(
            label = "Специальность",
            selected = profession,
            options = PROFESSION_PRESETS.map { it.name },
            onSelected = { profession = it; prefs.profession = it }
        )
        Text(
            presetForProfession(profession).summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
        OutlinedTextField(
            value = professionDetails,
            onValueChange = { professionDetails = it; prefs.professionDetails = it },
            label = { Text("Что добавить к плану (необязательно)") },
            placeholder = { Text("Например: каждый вторник двухчасовая планёрка") },
            minLines = 2,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        SectionTitle("О себе")
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = weightKg,
                onValueChange = { weightKg = it; prefs.weightKg = it },
                label = { Text("Вес, кг") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = heightCm,
                onValueChange = { heightCm = it; prefs.heightCm = it },
                label = { Text("Рост, см") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            )
            OutlinedTextField(
                value = age,
                onValueChange = { age = it; prefs.age = it },
                label = { Text("Возраст") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            )
        }
        DropdownField(
            label = "Пол",
            selected = gender,
            options = GENDER_OPTIONS_ONBOARDING,
            onSelected = { gender = it; prefs.gender = it },
            modifier = Modifier.padding(top = 12.dp)
        )
        DropdownField(
            label = "Семейное положение",
            selected = maritalStatus,
            options = MARITAL_OPTIONS_ONBOARDING,
            onSelected = { maritalStatus = it; prefs.maritalStatus = it },
            modifier = Modifier.padding(top = 12.dp)
        )

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        SectionTitle("Психологический портрет")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PERSONALITY_OPTIONS.forEach { option ->
                ChoiceChip(
                    label = option,
                    selected = personalityType == option,
                    onClick = { personalityType = option; prefs.personalityType = option },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        SectionTitle("Сон")
        DropdownField(
            label = "Как часто просыпаетесь ночью",
            selected = nightWakeFrequency,
            options = NIGHT_WAKE_OPTIONS,
            onSelected = { nightWakeFrequency = it; prefs.nightWakeFrequency = it },
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )
        TimePickerRow(
            label = "Во сколько отбой",
            timeText = bedTime,
            onTimeChange = { bedTime = it; prefs.bedTime = it }
        )
        TimePickerRow(
            label = "Во сколько подъём",
            timeText = wakeTime,
            onTimeChange = { wakeTime = it; prefs.wakeTime = it }
        )

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        SectionTitle("Питание")
        TimePickerRow(
            label = "Во сколько завтракаете",
            timeText = breakfastTime,
            onTimeChange = { breakfastTime = it; prefs.breakfastTime = it }
        )
        TimePickerRow(
            label = "Когда обедаете",
            timeText = lunchTime,
            onTimeChange = { lunchTime = it; prefs.lunchTime = it }
        )
        TimePickerRow(
            label = "Когда ужинаете",
            timeText = dinnerTime,
            onTimeChange = { dinnerTime = it; prefs.dinnerTime = it }
        )
        StepperRow(
            label = "Приёмов пищи в день",
            value = mealsPerDay,
            range = 1..6,
            onValueChange = { mealsPerDay = it; prefs.mealsPerDay = it }
        )
        DropdownField(
            label = "Считать воду в",
            selected = waterUnit,
            options = listOf("Бутылки", "Чай/кофе"),
            onSelected = { waterUnit = it; prefs.waterUnit = it },
            modifier = Modifier.padding(top = 8.dp)
        )
        StepperRow(
            label = if (waterUnit == "Бутылки") "Бутылок воды в день" else "Чашек чая/кофе в день",
            value = waterCount,
            range = 0..15,
            onValueChange = { waterCount = it; prefs.waterCount = it }
        )

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        SectionTitle("Работа")
        StepperRow(
            label = "Часов работы в день",
            value = workHours,
            range = 1..16,
            onValueChange = { workHours = it; prefs.workHoursPerDay = it }
        )

        Button(
            onClick = { onComplete() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp)
        ) {
            Text("Далее")
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Text(
                label,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun StepperRow(label: String, value: Int, range: IntRange, onValueChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (value > range.first) onValueChange(value - 1) }) {
                Icon(Icons.Default.Remove, contentDescription = "Меньше")
            }
            Text(
                "$value",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            IconButton(onClick = { if (value < range.last) onValueChange(value + 1) }) {
                Icon(Icons.Default.Add, contentDescription = "Больше")
            }
        }
    }
}
