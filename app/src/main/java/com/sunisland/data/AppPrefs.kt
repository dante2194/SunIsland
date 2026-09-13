package com.sunisland.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class SolarEventType { NEXT, SUNRISE, SOLAR_NOON, SUNSET, SOLAR_MIDNIGHT }
enum class IslandLayout { COMPACT, DETAILED, ICON, PROGRESS, MINIMAL, NOTCH }
enum class IslandFont { SYSTEM, SERIF, MONO, ROUNDED, DISPLAY }

data class AppSettings(
    val eventType: SolarEventType = SolarEventType.NEXT,
    val islandEnabled: Boolean = false,
    val widthDp: Int = 220,
    val heightDp: Int = 52,
    val xDp: Int = 0,
    val yDp: Int = 24,
    val cornerRadiusDp: Int = 26,
    val opacity: Int = 100,
    val countdownSp: Int = 22,
    val labelSp: Int = 11,
    val layout: IslandLayout = IslandLayout.DETAILED,
    val font: IslandFont = IslandFont.SYSTEM,
    val fontBold: Boolean = false,
    val animate: Boolean = true,
    val useCurrentLocation: Boolean = true,
    val manualLat: Double? = null,
    val manualLon: Double? = null,
    val autostartOnBoot: Boolean = false,
    val showProgress: Boolean = true,
    val showSeconds: Boolean = true,
    val accessibilityOverlay: Boolean = false
)

private val Context.dataStore by preferencesDataStore(name = "sun_island")

object PreferencesRepository {
    private val EVENT = stringPreferencesKey("event")
    private val ENABLED = booleanPreferencesKey("enabled")
    private val WIDTH = intPreferencesKey("width")
    private val HEIGHT = intPreferencesKey("height")
    private val X = intPreferencesKey("x")
    private val Y = intPreferencesKey("y")
    private val RADIUS = intPreferencesKey("radius")
    private val OPACITY = intPreferencesKey("opacity")
    private val COUNTDOWN_SP = intPreferencesKey("countdown_sp")
    private val LABEL_SP = intPreferencesKey("label_sp")
    private val LAYOUT = stringPreferencesKey("layout")
    private val FONT = stringPreferencesKey("font")
    private val BOLD = booleanPreferencesKey("bold")
    private val ANIMATE = booleanPreferencesKey("animate")
    private val CURRENT_LOCATION = booleanPreferencesKey("current_location")
    private val LAT = doublePreferencesKey("lat")
    private val LON = doublePreferencesKey("lon")
    private val AUTOSTART = booleanPreferencesKey("autostart")
    private val SHOW_PROGRESS = booleanPreferencesKey("show_progress")
    private val SHOW_SECONDS = booleanPreferencesKey("show_seconds")
    private val ACCESSIBILITY = booleanPreferencesKey("accessibility")

    fun flow(context: Context): Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            eventType = p[EVENT]?.let { runCatching { SolarEventType.valueOf(it) }.getOrNull() } ?: SolarEventType.NEXT,
            islandEnabled = p[ENABLED] ?: false,
            widthDp = p[WIDTH] ?: 220,
            heightDp = p[HEIGHT] ?: 52,
            xDp = p[X] ?: 0,
            yDp = p[Y] ?: 24,
            cornerRadiusDp = p[RADIUS] ?: 26,
            opacity = p[OPACITY] ?: 100,
            countdownSp = p[COUNTDOWN_SP] ?: 22,
            labelSp = p[LABEL_SP] ?: 11,
            layout = p[LAYOUT]?.let { runCatching { IslandLayout.valueOf(it) }.getOrNull() } ?: IslandLayout.DETAILED,
            font = p[FONT]?.let { runCatching { IslandFont.valueOf(it) }.getOrNull() } ?: IslandFont.SYSTEM,
            fontBold = p[BOLD] ?: false,
            animate = p[ANIMATE] ?: true,
            useCurrentLocation = p[CURRENT_LOCATION] ?: true,
            manualLat = p[LAT], manualLon = p[LON],
            autostartOnBoot = p[AUTOSTART] ?: false,
            showProgress = p[SHOW_PROGRESS] ?: true,
            showSeconds = p[SHOW_SECONDS] ?: true,
            accessibilityOverlay = p[ACCESSIBILITY] ?: false
        )
    }

    suspend fun update(context: Context, block: (MutablePrefs) -> Unit) {
        context.dataStore.edit { p -> block(MutablePrefs(p)) }
    }

    class MutablePrefs(private val p: androidx.datastore.preferences.core.MutablePreferences) {
        var eventType: SolarEventType
            get() = SolarEventType.valueOf(p[EVENT] ?: SolarEventType.NEXT.name)
            set(v) { p[EVENT] = v.name }
        var islandEnabled: Boolean get() = p[ENABLED] ?: false; set(v) { p[ENABLED] = v }
        var widthDp: Int get() = p[WIDTH] ?: 220; set(v) { p[WIDTH] = v }
        var heightDp: Int get() = p[HEIGHT] ?: 52; set(v) { p[HEIGHT] = v }
        var xDp: Int get() = p[X] ?: 0; set(v) { p[X] = v }
        var yDp: Int get() = p[Y] ?: 24; set(v) { p[Y] = v }
        var cornerRadiusDp: Int get() = p[RADIUS] ?: 26; set(v) { p[RADIUS] = v }
        var opacity: Int get() = p[OPACITY] ?: 100; set(v) { p[OPACITY] = v }
        var countdownSp: Int get() = p[COUNTDOWN_SP] ?: 22; set(v) { p[COUNTDOWN_SP] = v }
        var labelSp: Int get() = p[LABEL_SP] ?: 11; set(v) { p[LABEL_SP] = v }
        var layout: IslandLayout get() = IslandLayout.valueOf(p[LAYOUT] ?: IslandLayout.DETAILED.name); set(v) { p[LAYOUT] = v.name }
        var font: IslandFont get() = IslandFont.valueOf(p[FONT] ?: IslandFont.SYSTEM.name); set(v) { p[FONT] = v.name }
        var fontBold: Boolean get() = p[BOLD] ?: false; set(v) { p[BOLD] = v }
        var animate: Boolean get() = p[ANIMATE] ?: true; set(v) { p[ANIMATE] = v }
        var useCurrentLocation: Boolean get() = p[CURRENT_LOCATION] ?: true; set(v) { p[CURRENT_LOCATION] = v }
        var manualLat: Double? get() = p[LAT]; set(v) { if (v == null) p.remove(LAT) else p[LAT] = v }
        var manualLon: Double? get() = p[LON]; set(v) { if (v == null) p.remove(LON) else p[LON] = v }
        var autostartOnBoot: Boolean get() = p[AUTOSTART] ?: false; set(v) { p[AUTOSTART] = v }
        var showProgress: Boolean get() = p[SHOW_PROGRESS] ?: true; set(v) { p[SHOW_PROGRESS] = v }
        var showSeconds: Boolean get() = p[SHOW_SECONDS] ?: true; set(v) { p[SHOW_SECONDS] = v }
        var accessibilityOverlay: Boolean get() = p[ACCESSIBILITY] ?: false; set(v) { p[ACCESSIBILITY] = v }
    }
}
