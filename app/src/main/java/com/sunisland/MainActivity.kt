package com.sunisland

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.sunisland.accessibility.IslandAccessibilityService
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.sunisland.astronomy.EventChoice
import com.sunisland.astronomy.SolarCalculator
import com.sunisland.astronomy.SolarEvent
import com.sunisland.data.AppSettings
import com.sunisland.data.SolarEventType
import com.sunisland.data.IslandFont
import com.sunisland.data.IslandLayout
import com.sunisland.data.PreferencesRepository
import com.sunisland.island.IslandService
import com.sunisland.location.LocationRepository
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SunIslandApp(this) }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SunIslandApp(activity: MainActivity) {
    val context = activity
    val settings by PreferencesRepository.flow(context).collectAsState(initial = AppSettings())
    var tab by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result[Manifest.permission.ACCESS_COARSE_LOCATION] == true || result[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            scope.launch {
                LocationRepository(context).getLastKnownLocation()?.let { (lat, lon) ->
                    PreferencesRepository.update(context) { p -> p.manualLat = lat; p.manualLon = lon }
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        if (!LocationRepository(context).hasLocationPermission()) {
            locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
        } else if (settings.useCurrentLocation) {
            LocationRepository(context).getLastKnownLocation()?.let { (lat, lon) ->
                PreferencesRepository.update(context) { p -> p.manualLat = lat; p.manualLon = lon }
            }
        }
    }
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme()) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Sun Island") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)) },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(tab == 0, { tab = 0 }, icon = { Icon(Icons.Filled.Home, null) }, label = { Text("Home") })
                    NavigationBarItem(tab == 1, { tab = 1 }, icon = { Icon(Icons.Filled.Tune, null) }, label = { Text("Island") })
                    NavigationBarItem(tab == 2, { tab = 2 }, icon = { Icon(Icons.Filled.WbSunny, null) }, label = { Text("Times") })
                    NavigationBarItem(tab == 3, { tab = 3 }, icon = { Icon(Icons.Filled.Settings, null) }, label = { Text("Settings") })
                }
            }
        ) { pad ->
            AnimatedContent(tab, modifier = Modifier.padding(pad), transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "tab") { selected ->
                when (selected) {
                    0 -> HomeScreen(context, settings) {
                        if (settings.islandEnabled) {
                            scope.launch { PreferencesRepository.update(context) { p -> p.islandEnabled = false } }
                            context.stopService(Intent(context, IslandService::class.java))
                        } else if (Settings.canDrawOverlays(context)) {
                            scope.launch { PreferencesRepository.update(context) { p -> p.islandEnabled = true } }
                            ContextCompat.startForegroundService(context, Intent(context, IslandService::class.java))
                        } else {
                            context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                        }
                    }
                    1 -> IslandScreen(context, settings)
                    2 -> TimesScreen(settings)
                    3 -> SettingsScreen(context, settings, locationLauncher)
                }
            }
        }
    }
}

private fun toggleService(context: Context, enabled: Boolean) {
    val intent = Intent(context, IslandService::class.java)
    if (enabled) {
        if (!Settings.canDrawOverlays(context)) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
            return
        }
        ContextCompat.startForegroundService(context, intent)
    } else context.stopService(intent)
}

@Composable
private fun HomeScreen(context: Context, settings: AppSettings, onToggle: () -> Unit) {
    val hasCoords = settings.manualLat != null && settings.manualLon != null
    val now = ZonedDateTime.now()
    val solar = if (hasCoords) SolarCalculator.calculate(now.toLocalDate(), settings.manualLat!!, settings.manualLon!!, now.zone) else null
    val target = if (hasCoords) SolarCalculator.nextEvent(now, settings.manualLat!!, settings.manualLon!!, EventChoice.valueOf(settings.eventType.name)).second else null
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF11141A)), shape = RoundedCornerShape(28.dp)) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (hasCoords) eventTitle(settings) else "LOCATION NEEDED", style = MaterialTheme.typography.labelLarge)
                    Text(countdownText(target), fontSize = 38.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
                    Text(target?.format(DateTimeFormatter.ofPattern("EEE · HH:mm")) ?: "Grant location or set coordinates", color = Color.LightGray)
                }
            }
        }
        item {
            Text("Today", style = MaterialTheme.typography.titleLarge)
            solar?.let { SolarTimesCard(it) } ?: Text("Solar times will appear after a location is available.", color = Color.LightGray)
        }
        item { Button(onClick = onToggle, modifier = Modifier.fillMaxWidth()) { Text(if (settings.islandEnabled) "Stop Island" else "Start Island") } }
    }
}

@Composable
private fun SolarTimesCard(s: com.sunisland.astronomy.SolarTimes) {
    Card(shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SolarRow("🌅 Sunrise", s.sunrise)
            SolarRow("☀ Solar noon", s.solarNoon)
            SolarRow("🌇 Sunset", s.sunset)
            SolarRow("🌙 Solar midnight", s.solarMidnight)
        }
    }
}

@Composable
private fun SolarRow(label: String, time: ZonedDateTime?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label); Text(time?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "—")
    }
}

@Composable
private fun IslandScreen(context: Context, settings: AppSettings) {
    val scope = rememberCoroutineScope()
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Live island", style = MaterialTheme.typography.headlineSmall) }
        item {
            Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(28.dp)).background(Color(0xFF0A0C10)), contentAlignment = Alignment.Center) {
                IslandPreview(settings)
            }
        }
        item { SliderSetting("Width", settings.widthDp.toFloat(), 140f..360f) { v -> scope.launch { PreferencesRepository.update(context) { p -> p.widthDp = v.toInt() } } } }
        item { SliderSetting("Height", settings.heightDp.toFloat(), 40f..100f) { v -> scope.launch { PreferencesRepository.update(context) { p -> p.heightDp = v.toInt() } } } }
        item { SliderSetting("Corner radius", settings.cornerRadiusDp.toFloat(), 4f..50f) { v -> scope.launch { PreferencesRepository.update(context) { p -> p.cornerRadiusDp = v.toInt() } } } }
        item { SliderSetting("Countdown size", settings.countdownSp.toFloat(), 12f..34f) { v -> scope.launch { PreferencesRepository.update(context) { p -> p.countdownSp = v.toInt() } } } }
        item { SliderSetting("Label size", settings.labelSp.toFloat(), 8f..18f) { v -> scope.launch { PreferencesRepository.update(context) { p -> p.labelSp = v.toInt() } } } }
        item { SliderSetting("Opacity", settings.opacity.toFloat(), 20f..100f) { v -> scope.launch { PreferencesRepository.update(context) { p -> p.opacity = v.toInt() } } } }
        item { ChoiceRow("Layout", IslandLayout.entries, settings.layout) { value -> scope.launch { PreferencesRepository.update(context) { p -> p.layout = value } } } }
        if (settings.layout == IslandLayout.NOTCH) {
            item { Text("The notch is pinned to the top center of the screen and cannot be dragged.", color = Color.LightGray, fontSize = 13.sp) }
        }
        item { ChoiceRow("Font", IslandFont.entries, settings.font) { value -> scope.launch { PreferencesRepository.update(context) { p -> p.font = value } } } }
        item { SwitchRow("Bold text", settings.fontBold) { v -> scope.launch { PreferencesRepository.update(context) { p -> p.fontBold = v } } } }
        item { SwitchRow("Animation", settings.animate) { v -> scope.launch { PreferencesRepository.update(context) { p -> p.animate = v } } } }
        item { SwitchRow("Progress", settings.showProgress) { v -> scope.launch { PreferencesRepository.update(context) { p -> p.showProgress = v } } } }
        item { SwitchRow("Show seconds", settings.showSeconds) { v -> scope.launch { PreferencesRepository.update(context) { p -> p.showSeconds = v } } } }
    }
}

@Composable private fun IslandPreview(settings: AppSettings) {
    val alpha = settings.opacity.coerceIn(20, 100) / 100f
    val bgColor = Color(0xFF080A0E).copy(alpha = alpha)
    val w = settings.widthDp.dp.coerceAtMost(340.dp)
    val h = settings.heightDp.dp.coerceAtMost(90.dp)
    if (settings.layout == IslandLayout.NOTCH) {
        Box(Modifier.width(w).height(h).clip(RoundedCornerShape(50)).background(bgColor), contentAlignment = Alignment.Center) {
            Box(Modifier.size(13.dp).clip(CircleShape).background(Color(0xFF3A3E46)), contentAlignment = Alignment.Center) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF0A0C10)))
            }
        }
        return
    }
    val text = when (settings.layout) {
        IslandLayout.MINIMAL -> "02:43:18"
        IslandLayout.COMPACT -> "02:43:18"
        IslandLayout.ICON -> "☀  SUNSET  02:43:18"
        else -> "SUNSET\n02:43:18\n19:42"
    }
    val family = when (settings.font) { IslandFont.SERIF -> FontFamily.Serif; IslandFont.MONO -> FontFamily.Monospace; else -> FontFamily.SansSerif }
    Box(Modifier.width(w).height(h).clip(RoundedCornerShape(settings.cornerRadiusDp.dp)).background(bgColor), contentAlignment = Alignment.Center) {
        Text(text, fontFamily = family, fontSize = settings.countdownSp.sp, fontWeight = if (settings.fontBold) FontWeight.Bold else FontWeight.Normal, lineHeight = (settings.countdownSp + 2).sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun TimesScreen(settings: AppSettings) {
    val coords = settings.manualLat to settings.manualLon
    val now = ZonedDateTime.now()
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Solar times", style = MaterialTheme.typography.headlineSmall) }
        if (coords.first != null && coords.second != null) {
            item { SolarTimesCard(SolarCalculator.calculate(now.toLocalDate(), coords.first!!, coords.second!!, now.zone)) }
            item { Text("Tomorrow", style = MaterialTheme.typography.titleLarge) }
            item { SolarTimesCard(SolarCalculator.calculate(now.toLocalDate().plusDays(1), coords.first!!, coords.second!!, now.zone)) }
        } else item { Text("No location available.", color = Color.LightGray) }
    }
}

@Composable
private fun SettingsScreen(context: Context, settings: AppSettings, locationLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>) {
    val scope = rememberCoroutineScope()
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Settings", style = MaterialTheme.typography.headlineSmall) }
        item { ChoiceRow("Countdown event", SolarEventType.entries.toList(), settings.eventType, itemLabel = { it.displayName() }) { value -> scope.launch { PreferencesRepository.update(context) { p -> p.eventType = value } } } }
        item { Text("Permissions", style = MaterialTheme.typography.titleMedium) }
        item { PermissionCard(context, locationLauncher) }
        item { Text("Xiaomi / HyperOS", style = MaterialTheme.typography.titleMedium) }
        item { XiaomiCard(context, settings, scope) }
        item { SwitchRow("Remember island after reboot", settings.autostartOnBoot) { v -> scope.launch { PreferencesRepository.update(context) { p -> p.autostartOnBoot = v } } } }
        item { Text("Accessibility overlay (lock screen)", style = MaterialTheme.typography.titleMedium) }
        item { AccessibilityCard(context, settings, scope) }
    }
}

@Composable private fun PermissionCard(context: Context, locationLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>) {
    val overlay = Settings.canDrawOverlays(context)
    Card { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Location: ${if (LocationRepository(context).hasLocationPermission()) "granted" else "needed"}")
        Text("Overlay: ${if (overlay) "granted" else "needed"}")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)) }) { Text("Location") }
            OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))) }) { Text("Overlay") }
        }
    } }
}

@Composable private fun XiaomiCard(context: Context, settings: AppSettings, scope: kotlinx.coroutines.CoroutineScope) {
    Card { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("For Xiaomi/HyperOS, enable Autostart and allow the app to run without restrictive battery limits in system settings.")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { openIntentBestEffort(context, Intent().setComponent(android.content.ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"))) }) { Text("Autostart") }
            OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))) }) { Text("Battery") }
        }
        Text("Android may still restrict background starts. The island stays user-visible via a foreground service when running.", color = Color.LightGray, fontSize = 13.sp)
    } }
}

@Composable private fun AccessibilityCard(context: Context, settings: AppSettings, scope: kotlinx.coroutines.CoroutineScope) {
    val a11yConnected by IslandAccessibilityService.connected.collectAsState()
    Card {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SwitchRow("Draw over lock screen", settings.accessibilityOverlay) { v ->
                scope.launch { PreferencesRepository.update(context) { p -> p.accessibilityOverlay = v } }
            }
            Text("When enabled, the Sun Island accessibility service must also be turned on in Android Settings for the island to appear above the lock screen.", color = Color.LightGray, fontSize = 13.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Service status", fontSize = 13.sp)
                Text(if (a11yConnected) "Connected" else "Not connected", fontSize = 13.sp, color = if (a11yConnected) Color(0xFF6CDA6C) else Color(0xFFFF6B6B))
            }
            OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) { Text("Open accessibility settings") }
        }
    }
}

private fun openIntentBestEffort(context: Context, intent: Intent) { runCatching { context.startActivity(intent) }.onFailure { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) } }

@Composable private fun SliderSetting(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) { Column { Text("$label: ${value.toInt()}"); Slider(value, onChange, valueRange = range) } }
@Composable private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(label); Switch(checked, onChange) } }
@Composable
private fun <T> ChoiceRow(
    label: String,
    values: List<T>,
    selected: T,
    itemLabel: (T) -> String = { it.toString().replace('_', ' ') },
    onSelected: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            values.forEach { value ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelected(value) },
                    label = { Text(itemLabel(value)) }
                )
            }
        }
    }
}

private fun eventTitle(settings: AppSettings): String = when (settings.eventType) { com.sunisland.data.SolarEventType.NEXT -> "NEXT EVENT"; com.sunisland.data.SolarEventType.SUNRISE -> "SUNRISE"; com.sunisland.data.SolarEventType.SOLAR_NOON -> "SOLAR NOON"; com.sunisland.data.SolarEventType.SUNSET -> "SUNSET"; com.sunisland.data.SolarEventType.SOLAR_MIDNIGHT -> "SOLAR MIDNIGHT" }
private fun countdownText(target: ZonedDateTime?): String { if (target == null) return "—"; val seconds = maxOf(0L, java.time.Duration.between(ZonedDateTime.now(), target).seconds); val h = seconds / 3600; val m = seconds % 3600 / 60; val s = seconds % 60; return String.format("%02d:%02d:%02d", h, m, s) }
private fun SolarEventType.displayName() = when (this) { SolarEventType.SUNRISE -> "Sunrise"; SolarEventType.SOLAR_NOON -> "Solar noon"; SolarEventType.SUNSET -> "Sunset"; SolarEventType.SOLAR_MIDNIGHT -> "Solar midnight"; SolarEventType.NEXT -> "Next event" }
