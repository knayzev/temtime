package com.focustimer.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.focustimer.app.PrefsManager

/** The values AdviceEngine and the voice picker already expect, so the labels map onto them. */
private const val GENDER_MALE = "Мужской"
private const val GENDER_FEMALE = "Женский"

/**
 * Step one, and the first screen a new user sees: who the plan is for. It asks only what the plan
 * and the advice engine need. Name and e-mail are optional and are filled in later, in the profile.
 */
@Composable
fun ProfileSetupScreen(onNext: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }

    var gender by remember { mutableStateOf(prefs.gender) }
    var age by remember { mutableStateOf(prefs.age) }
    var weight by remember { mutableStateOf(prefs.weightKg) }
    var height by remember { mutableStateOf(prefs.heightCm) }
    var profession by remember {
        mutableStateOf(prefs.profession.ifBlank { presetForProfession("").name })
    }
    var professionDetails by remember { mutableStateOf(prefs.professionDetails) }
    var wakeTime by remember { mutableStateOf(prefs.wakeTime) }

    val preset = remember(profession) { presetForProfession(profession) }
    val canContinue = gender.isNotBlank() && age.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // The app opens here, so this is also where it says what it is.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    Icons.Outlined.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text("EPV", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Таймер и план на день",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        StepperHeader(
            currentStep = 1,
            labels = listOf("О себе", "Расписание", "Готово"),
            modifier = Modifier.padding(bottom = 20.dp)
        )
        Text(
            "Расскажите о себе",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            "По этим данным подбираются длительность блоков и рекомендации",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GenderOption(
                label = "Я мужчина",
                emoji = "👨",
                selected = gender == GENDER_MALE,
                onClick = {
                    gender = GENDER_MALE
                    prefs.gender = GENDER_MALE
                },
                modifier = Modifier.weight(1f)
            )
            GenderOption(
                label = "Я женщина",
                emoji = "👩",
                selected = gender == GENDER_FEMALE,
                onClick = {
                    gender = GENDER_FEMALE
                    prefs.gender = GENDER_FEMALE
                },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NumberField(
                value = age,
                onValueChange = {
                    age = it.filter { ch -> ch.isDigit() }.take(3)
                    prefs.age = age
                },
                label = "Возраст",
                modifier = Modifier.weight(1f)
            )
            NumberField(
                value = weight,
                onValueChange = {
                    weight = it.filter { ch -> ch.isDigit() }.take(3)
                    prefs.weightKg = weight
                },
                label = "Вес, кг",
                modifier = Modifier.weight(1f)
            )
            NumberField(
                value = height,
                onValueChange = {
                    height = it.filter { ch -> ch.isDigit() }.take(3)
                    prefs.heightCm = height
                },
                label = "Рост, см",
                modifier = Modifier.weight(1f)
            )
        }

        DropdownField(
            label = "Профессия",
            selected = profession,
            options = PROFESSION_PRESETS.map { it.name },
            onSelected = {
                profession = it
                prefs.profession = it
            },
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            preset.summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
        OutlinedTextField(
            value = professionDetails,
            onValueChange = {
                professionDetails = it
                prefs.professionDetails = it
            },
            label = { Text("Чем именно занимаетесь (необязательно)") },
            minLines = 2,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )

        // The wake time anchors the generated schedule, so it is asked for here rather than later.
        TimePickerRow(
            label = "Во сколько встаёте",
            timeText = wakeTime,
            onTimeChange = { wakeTime = it; prefs.wakeTime = it }
        )

        Button(
            onClick = onNext,
            enabled = canContinue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .heightIn(min = 52.dp)
        ) {
            Text("Собрать расписание")
        }
        if (!canContinue) {
            Text(
                "Выберите пол и укажите возраст, чтобы продолжить",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Text(
            "Имя и почту можно указать позже в профиле — по желанию.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )
    }
}

/** A tick-box sized as a card, so each of the two options is a single obvious tap. */
@Composable
private fun GenderOption(
    label: String,
    emoji: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        // The border carries the choice: a grey hairline at rest, black once picked.
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 12.dp)
        ) {
            Text(emoji, style = MaterialTheme.typography.headlineMedium)
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
            Icon(
                imageVector = if (selected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(20.dp)
            )
        }
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}
