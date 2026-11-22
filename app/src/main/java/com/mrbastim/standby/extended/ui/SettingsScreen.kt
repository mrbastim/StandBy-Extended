package com.mrbastim.standby.extended.ui

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.RadioButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.runtime.saveable.rememberSaveable
import com.mrbastim.standby.extended.data.settings.DisplayProtectionSettings
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.mrbastim.standby.extended.ui.theme.StandByTheme
import com.mrbastim.standby.extended.data.settings.StandBySettings
import com.mrbastim.standby.extended.system.ChargingState
import com.mrbastim.standby.extended.BuildConfig
import com.mrbastim.standby.extended.R
import com.mrbastim.standby.extended.data.settings.ClockColor
import com.mrbastim.standby.extended.data.settings.ClockFace
import com.mrbastim.standby.extended.data.settings.BackgroundStyle
import com.mrbastim.standby.extended.data.settings.BatteryWidgetStyle
import com.mrbastim.standby.extended.data.settings.ClockWidgetStyle
import com.mrbastim.standby.extended.data.settings.DateWidgetStyle
import com.mrbastim.standby.extended.data.settings.ScreenTimeout
import com.mrbastim.standby.extended.data.settings.StandByThemeId
import com.mrbastim.standby.extended.data.settings.TypographyStyle
import com.mrbastim.standby.extended.data.settings.WorldClockZone
import com.mrbastim.standby.extended.system.ChargingSource

@Composable
fun SettingsScreen(
    state: StandByUiState,
    onDisplayProtectionChanged: (DisplayProtectionSettings) -> Unit = {},
    onAutomaticModeChanged: (Boolean) -> Unit,
    on24HourChanged: (Boolean) -> Unit,
    onShowSecondsChanged: (Boolean) -> Unit,
    onScreenTimeoutChanged: (ScreenTimeout) -> Unit,
    onKeepAwakeChanged: (Boolean) -> Unit,
    onClockFaceChanged: (ClockFace) -> Unit,
    onClockColorChanged: (ClockColor) -> Unit,
    onShowDateChanged: (Boolean) -> Unit,
    onNightModeChanged: (Boolean) -> Unit,
    onBurnInProtectionChanged: (Boolean) -> Unit,
    onDayThemeChanged: (StandByThemeId) -> Unit,
    onNightThemeChanged: (StandByThemeId) -> Unit,
    onBackgroundStyleChanged: (BackgroundStyle) -> Unit,
    onTypographyStyleChanged: (TypographyStyle) -> Unit,
    onClockWidgetStyleChanged: (ClockWidgetStyle) -> Unit,
    onDateWidgetStyleChanged: (DateWidgetStyle) -> Unit,
    onBatteryWidgetStyleChanged: (BatteryWidgetStyle) -> Unit,
    onWorldClockZoneChanged: (WorldClockZone) -> Unit,
    onWeatherEnabledChanged: (Boolean) -> Unit,
    onPreview: () -> Unit,
    onOpenScreenSaverSettings: () -> Unit,
) {
    val context = LocalContext.current
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            Text(stringResource(R.string.app_name), fontSize = 36.sp, fontWeight = FontWeight.Light)
            Text(
                statusText(state),
                modifier = Modifier.padding(top = 6.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
            )
            Button(onClick = onPreview, modifier = Modifier.padding(top = 18.dp)) {
                Text(stringResource(R.string.preview_standby))
            }

            SettingsSection(stringResource(R.string.section_standby))
            SettingSwitch(
                stringResource(R.string.automatic_activation),
                stringResource(R.string.automatic_activation_description),
                state.settings.automaticModeEnabled,
                onAutomaticModeChanged,
            )
            SettingsSection(stringResource(R.string.protection_section))
            var protectionExpanded by rememberSaveable { mutableStateOf(false) }
            TextButton(onClick = { protectionExpanded = !protectionExpanded }) {
                Text(stringResource(if (protectionExpanded) R.string.protection_collapse else R.string.protection_expand))
            }
            if (protectionExpanded) {
                val protection = state.settings.displayProtection
                val lightAvailable = remember(context) {
                    context.getSystemService(SensorManager::class.java)?.getDefaultSensor(Sensor.TYPE_LIGHT) != null
                }

                // The main switch determines which brightness/dimming controls are available.
                SettingSwitch(
                    stringResource(R.string.protection_enabled),
                    stringResource(R.string.protection_description),
                    protection.enabled,
                    { onDisplayProtectionChanged(protection.copy(enabled = it)) },
                )

                if (protection.enabled) {
                    DisplaySettingsGroup(stringResource(R.string.display_group_brightness))
                    BrightnessControls(protection, onDisplayProtectionChanged)
                    SettingSwitch(
                        stringResource(R.string.protection_light),
                        stringResource(
                            if (lightAvailable) R.string.protection_light_description
                            else R.string.protection_no_sensor
                        ),
                        protection.ambientLight && lightAvailable,
                        { onDisplayProtectionChanged(protection.copy(ambientLight = it)) },
                        enabled = lightAvailable,
                    )
                }

                DisplaySettingsGroup(stringResource(R.string.display_group_behavior))
                if (protection.enabled) {
                    DimmingControls(protection, onDisplayProtectionChanged)
                }
                SettingSwitch(
                    stringResource(R.string.keep_screen_awake),
                    stringResource(R.string.keep_screen_awake_description),
                    state.settings.keepScreenAwake,
                    onKeepAwakeChanged,
                )
                ChoiceSetting(
                    title = stringResource(R.string.screen_timeout),
                    value = context.localizedLabel(state.settings.screenTimeout),
                    enabled = !state.settings.keepScreenAwake,
                    choices = ScreenTimeout.entries,
                    label = context::localizedLabel,
                    onSelected = onScreenTimeoutChanged,
                )
                if (protection.enabled) {
                    ChoiceSetting(
                        title = stringResource(R.string.protection_release),
                        value = if (protection.releaseAfterMinutes == 0)
                            stringResource(R.string.protection_disabled)
                        else stringResource(R.string.protection_minutes, protection.releaseAfterMinutes),
                        choices = listOf(0, 15, 30, 60, 120, 240, 480, 720),
                        label = {
                            if (it == 0) context.getString(R.string.protection_disabled)
                            else context.getString(R.string.protection_minutes, it)
                        },
                        onSelected = {
                            onDisplayProtectionChanged(protection.copy(releaseAfterMinutes = it))
                        },
                        description = stringResource(R.string.protection_release_description),
                    )
                }

                DisplaySettingsGroup(stringResource(R.string.display_group_anti_burn_in))
                SettingSwitch(
                    stringResource(R.string.burn_in_protection),
                    stringResource(R.string.burn_in_protection_description),
                    state.settings.burnInProtection,
                    onBurnInProtectionChanged,
                )
            }

            SettingsSection(stringResource(R.string.section_appearance))
            ChoiceSetting(stringResource(R.string.day_theme), context.localizedLabel(state.settings.dayTheme),
                choices = StandByThemeId.entries, label = context::localizedLabel, onSelected = onDayThemeChanged)
            ChoiceSetting(stringResource(R.string.night_theme), context.localizedLabel(state.settings.nightTheme),
                choices = StandByThemeId.entries.filterNot { it == StandByThemeId.MONO_LIGHT },
                label = context::localizedLabel, onSelected = onNightThemeChanged)
            ChoiceSetting(stringResource(R.string.background), context.localizedLabel(state.settings.backgroundStyle),
                choices = BackgroundStyle.entries, label = context::localizedLabel, onSelected = onBackgroundStyleChanged)
            ChoiceSetting(stringResource(R.string.clock_face), context.localizedLabel(state.settings.clockFace),
                choices = ClockFace.entries, label = context::localizedLabel, onSelected = onClockFaceChanged)
            ChoiceSetting(stringResource(R.string.clock_color), context.localizedLabel(state.settings.clockColor),
                choices = ClockColor.entries, label = context::localizedLabel, onSelected = onClockColorChanged)
            SettingSwitch(stringResource(R.string.time_24_hour), null, state.settings.use24HourClock, on24HourChanged)
            SettingSwitch(stringResource(R.string.show_seconds), null, state.settings.showSeconds, onShowSecondsChanged)
            SettingSwitch(stringResource(R.string.show_date), null, state.settings.showDate, onShowDateChanged)
            ChoiceSetting(stringResource(R.string.typography), context.localizedLabel(state.settings.typographyStyle),
                choices = TypographyStyle.entries, label = context::localizedLabel, onSelected = onTypographyStyleChanged)
            SettingSwitch(
                stringResource(R.string.night_mode),
                stringResource(R.string.night_mode_description),
                state.settings.nightMode,
                onNightModeChanged,
            )

            SettingsSection(stringResource(R.string.section_widgets))
            InfoRow(stringResource(R.string.left_stack), state.settings.leftWidgets.joinToString { context.localizedLabel(it) })
            InfoRow(stringResource(R.string.right_stack), state.settings.rightWidgets.joinToString { context.localizedLabel(it) })
            ChoiceSetting(stringResource(R.string.clock_widget_style), context.localizedLabel(state.settings.clockWidgetStyle),
                choices = ClockWidgetStyle.entries, label = context::localizedLabel, onSelected = onClockWidgetStyleChanged)
            ChoiceSetting(stringResource(R.string.date_widget_style), context.localizedLabel(state.settings.dateWidgetStyle),
                choices = DateWidgetStyle.entries, label = context::localizedLabel, onSelected = onDateWidgetStyleChanged)
            ChoiceSetting(stringResource(R.string.battery_widget_style), context.localizedLabel(state.settings.batteryWidgetStyle),
                choices = BatteryWidgetStyle.entries, label = context::localizedLabel, onSelected = onBatteryWidgetStyleChanged)
            CityPickerSetting(
                selected = state.settings.worldClockZone,
                onSelected = onWorldClockZoneChanged,
            )
            SettingSwitch(
                stringResource(R.string.weather_enabled),
                stringResource(R.string.weather_enabled_description),
                state.settings.weatherEnabled,
                onWeatherEnabledChanged,
            )
            Text(
                stringResource(R.string.widget_edit_hint),
                modifier = Modifier.padding(top = 10.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )

            SettingsSection(stringResource(R.string.section_system))
            TextButton(onClick = onOpenScreenSaverSettings) {
                Text(stringResource(R.string.open_screen_saver_settings))
            }

            SettingsSection(stringResource(R.string.section_about))
            InfoRow(stringResource(R.string.version), BuildConfig.VERSION_NAME)
            Text(
                stringResource(R.string.background_launch_explanation),
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DisplaySettingsGroup(title: String) {
    Text(
        title,
        modifier = Modifier.padding(top = 20.dp, bottom = 6.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun BrightnessControls(
    protection: DisplayProtectionSettings,
    onChanged: (DisplayProtectionSettings) -> Unit,
) {
    ProtectionSlider(
        R.string.protection_initial, R.string.protection_initial_description,
        protection.initialPercent, protection.minimumPercent..100,
        formatValue = { stringResource(R.string.protection_percent, it) },
    ) { onChanged(protection.copy(initialPercent = it)) }

    ProtectionSlider(
        R.string.protection_minimum, R.string.protection_minimum_description,
        protection.minimumPercent, 1..protection.initialPercent,
        formatValue = { stringResource(R.string.protection_percent, it) },
    ) { onChanged(protection.copy(minimumPercent = it)) }
}

@Composable
private fun DimmingControls(
    protection: DisplayProtectionSettings,
    onChanged: (DisplayProtectionSettings) -> Unit,
) {
    ProtectionDelaySetting(protection.dimDelaySeconds) {
        onChanged(protection.copy(dimDelaySeconds = it))
    }

    ProtectionSlider(
        R.string.protection_transition, R.string.protection_transition_description,
        protection.transitionSeconds, 1..60,
        formatValue = { protectionTime(it) },
    ) { onChanged(protection.copy(transitionSeconds = it)) }
}

@Composable
private fun ProtectionSlider(
    title: Int,
    description: Int,
    value: Int,
    range: IntRange,
    formatValue: @Composable (Int) -> String,
    onValue: (Int) -> Unit,
) {
    var draft by remember(value, range) { mutableStateOf(value.toFloat()) }
    val titleText = stringResource(title)
    val descriptionText = stringResource(description)
    val current = draft.roundToInt().coerceIn(range)
    val formattedValue = formatValue(current)
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        ProtectionSettingLabel(titleText, formattedValue, descriptionText)
        Slider(
            modifier = Modifier.semantics {
                contentDescription = listOf(titleText, descriptionText).joinToString(". ")
                stateDescription = formattedValue
            },
            value = current.toFloat(),
            onValueChange = { draft = it },
            onValueChangeFinished = {
                val selected = draft.roundToInt().coerceIn(range)
                if (selected != value) onValue(selected)
            },
            valueRange = range.first.toFloat()..range.last.coerceAtLeast(range.first + 1).toFloat(),
            enabled = range.last > range.first,
        )
        if (range.first != range.last) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatValue(range.first), style = MaterialTheme.typography.bodySmall)
                Text(formatValue(range.last), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ProtectionSettingLabel(title: String, value: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(
            value,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.End,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    Text(
        description,
        modifier = Modifier.padding(top = 4.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private val protectionDelayPresets = listOf(15, 30, 60, 120, 300, 600)
private val protectionDelayRange = 10..3600

@Composable
private fun ProtectionDelaySetting(delaySeconds: Int, onValue: (Int) -> Unit) {
    val title = stringResource(R.string.protection_delay)
    val context = LocalContext.current
    var customSeconds by rememberSaveable { mutableStateOf<String?>(null) }
    ChoiceSetting(
        title = title,
        value = protectionTime(delaySeconds),
        description = stringResource(R.string.protection_delay_description),
        choices = protectionDelayPresets + listOf(null),
        label = { seconds ->
            if (seconds == null) context.getString(R.string.protection_custom_delay)
            else protectionTime(context, seconds)
        },
        onSelected = { seconds ->
            if (seconds == null) customSeconds = delaySeconds.toString()
            else onValue(seconds)
        },
    )
    customSeconds?.let { input ->
        val seconds = input.toIntOrNull()?.takeIf { it in protectionDelayRange }
        AlertDialog(
            onDismissRequest = { customSeconds = null },
            title = { Text(stringResource(R.string.protection_custom_delay)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { customSeconds = it.filter(Char::isDigit).take(4) },
                        label = { Text(stringResource(R.string.protection_delay_input)) },
                        supportingText = {
                            Text(stringResource(
                                R.string.protection_delay_range,
                                protectionDelayRange.first,
                                protectionDelayRange.last,
                            ))
                        },
                        isError = input.isNotEmpty() && seconds == null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = seconds != null,
                    onClick = {
                        seconds?.let(onValue)
                        customSeconds = null
                    },
                ) { Text(stringResource(R.string.protection_apply)) }
            },
            dismissButton = {
                TextButton(onClick = { customSeconds = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun protectionTime(seconds: Int): String = protectionTime(LocalContext.current, seconds)

private fun protectionTime(context: Context, seconds: Int): String {
    val minutes = seconds / 60
    val remaining = seconds % 60
    return when {
        minutes == 0 -> context.getString(R.string.protection_seconds, remaining)
        remaining == 0 -> context.getString(R.string.protection_minutes, minutes)
        else -> context.getString(R.string.protection_minutes_seconds, minutes, remaining)
    }
}

@Preview(name = "Protection 2.0", locale = "ru", widthDp = 400, heightDp = 620)
@Preview(name = "Protection 2.0 • Large text", locale = "tr", widthDp = 560, heightDp = 300, fontScale = 2f)
@Composable
private fun ProtectionControlsPreview() {
    StandByTheme {
        Surface {
            var protection by remember { mutableStateOf(DisplayProtectionSettings(dimDelaySeconds = 90)) }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                DisplaySettingsGroup(stringResource(R.string.display_group_brightness))
                BrightnessControls(protection) { protection = it }
                DisplaySettingsGroup(stringResource(R.string.display_group_behavior))
                DimmingControls(protection) { protection = it }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        title,
        modifier = Modifier.padding(top = 30.dp, bottom = 8.dp),
        color = MaterialTheme.colorScheme.primary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.5.sp,
    )
}

@Composable
private fun SettingSwitch(
    title: String,
    description: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            description?.let {
                Text(it, modifier = Modifier.padding(top = 3.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}

@Composable
private fun <T> ChoiceSetting(
    title: String,
    value: String,
    enabled: Boolean = true,
    choices: List<T>,
    label: (T) -> String,
    onSelected: (T) -> Unit,
    description: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { expanded = true }
            .padding(vertical = 13.dp),
    ) {
        if (description == null) {
            Text(title, fontSize = 16.sp)
            Text(value,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.4f),
                fontSize = 13.sp)
        } else {
            ProtectionSettingLabel(title, value, description)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            choices.forEach { choice ->
                DropdownMenuItem(
                    text = { Text(label(choice)) },
                    onClick = { expanded = false; onSelected(choice) },
                )
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}

@Composable
private fun CityPickerSetting(
    selected: WorldClockZone,
    onSelected: (WorldClockZone) -> Unit,
) {
    val context = LocalContext.current
    var dialogOpen by rememberSaveable { mutableStateOf(false) }
    val title = stringResource(R.string.world_clock_city)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { dialogOpen = true }
            .padding(vertical = 13.dp),
    ) {
        Text(title, fontSize = 16.sp)
        Text(
            context.localizedLabel(selected),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

    if (dialogOpen) {
        AlertDialog(
            onDismissRequest = { dialogOpen = false },
            title = { Text(title) },
            text = {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                    items(WorldClockZone.entries, key = { it.name }) { zone ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelected(zone)
                                    dialogOpen = false
                                }
                                .padding(vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                context.localizedLabel(zone),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            RadioButton(
                                selected = zone == selected,
                                onClick = {
                                    onSelected(zone)
                                    dialogOpen = false
                                },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { dialogOpen = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, fontSize = 15.sp)
        Text(value, modifier = Modifier.weight(1f).padding(start = 24.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
    }
}

@Composable
private fun statusText(state: StandByUiState): String = when {
    !state.settings.automaticModeEnabled -> stringResource(R.string.status_automatic_off)
    !state.charging.isCharging -> stringResource(R.string.status_connect_power)
    !state.isLandscape -> stringResource(R.string.status_rotate_landscape)
    state.charging.source == ChargingSource.WIRELESS -> stringResource(R.string.status_wireless_ready)
    else -> stringResource(R.string.status_charging_ready)
}

@Preview(name = "Настройки", showBackground = true, locale = "ru", widthDp = 412, heightDp = 915,
    showSystemUi = true
)
@Composable
private fun SettingsScreenPreview() {
    StandByTheme {
        SettingsScreen(
            state = StandByUiState(
                settings = StandBySettings(),
                charging = ChargingState(isCharging = true, batteryPercent = 82),
                isLandscape = true,
            ),
            onAutomaticModeChanged = {},
            on24HourChanged = {},
            onShowSecondsChanged = {},
            onScreenTimeoutChanged = {},
            onKeepAwakeChanged = {},
            onClockFaceChanged = {},
            onClockColorChanged = {},
            onShowDateChanged = {},
            onNightModeChanged = {},
            onBurnInProtectionChanged = {},
            onDayThemeChanged = {},
            onNightThemeChanged = {},
            onBackgroundStyleChanged = {},
            onTypographyStyleChanged = {},
            onClockWidgetStyleChanged = {},
            onDateWidgetStyleChanged = {},
            onBatteryWidgetStyleChanged = {},
            onWorldClockZoneChanged = {},
            onWeatherEnabledChanged = {},
            onPreview = {},
            onOpenScreenSaverSettings = {},
        )
    }
}
