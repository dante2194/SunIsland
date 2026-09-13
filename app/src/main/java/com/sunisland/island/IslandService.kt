package com.sunisland.island

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.sunisland.R
import com.sunisland.astronomy.EventChoice
import com.sunisland.astronomy.SolarCalculator
import com.sunisland.data.AppSettings
import com.sunisland.data.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import com.sunisland.data.IslandLayout
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

class IslandService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var settingsJob: Job? = null
    private var tickJob: Job? = null
    private lateinit var windowManager: WindowManager
    private var island: IslandView? = null
    private var currentTarget: ZonedDateTime? = null
    private var currentTitle: String = "SUNSET"

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, notification())
        windowManager = getSystemService(WindowManager::class.java)
        settingsJob = scope.launch {
            combine(
                PreferencesRepository.flow(this@IslandService),
                com.sunisland.accessibility.IslandAccessibilityService.connected
            ) { settings, connected -> settings to connected }.collect { (settings, a11yConnected) ->
                if (!settings.islandEnabled) { removeIsland(); return@collect }
                if (!Settings.canDrawOverlays(this@IslandService)) { removeIsland(); return@collect }
                ensureIsland(settings, a11yConnected)
                recomputeTarget(settings)
            }
        }
    }

    private fun windowType(settings: AppSettings, a11yConnected: Boolean): Int =
        if (settings.accessibilityOverlay && a11yConnected)
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        else if (Build.VERSION.SDK_INT >= 26)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE

    private fun ensureIsland(settings: AppSettings, a11yConnected: Boolean) {
        val desiredType = windowType(settings, a11yConnected)
        val fixed = settings.layout == IslandLayout.NOTCH
        val posX = if (fixed) 0 else settings.xDp
        val posY = if (fixed) 0 else settings.yDp
        val density = resources.displayMetrics.density
        val newW = (settings.widthDp * density).toInt()
        val newH = (settings.heightDp * density).toInt()

        val existing = island
        if (existing != null) {
            val lp = existing.layoutParams as? WindowManager.LayoutParams
            if (lp == null) {
                removeIsland()
            } else if (lp.type != desiredType) {
                removeIsland()
            } else {
                if (lp.width != newW || lp.height != newH || lp.x != posX || lp.y != posY) {
                    lp.width = newW; lp.height = newH; lp.x = posX; lp.y = posY
                    windowManager.updateViewLayout(existing, lp)
                }
                existing.bind(settings, currentTitle, currentTarget)
                return
            }
        }

        val view = IslandView(this) { x, y ->
            if (settings.layout != IslandLayout.NOTCH) {
                island?.let { v -> windowManager.updateViewLayout(v, v.layoutParams) }
                scope.launch { PreferencesRepository.update(this@IslandService) { p -> p.xDp = x; p.yDp = y } }
            }
        }
        island = view
        val lp = WindowManager.LayoutParams(
            newW,
            newH,
            desiredType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
            x = posX
            y = posY
        }
        view.layoutParams = lp
        view.beginDrag(lp)
        windowManager.addView(view, lp)
    }

    private fun recomputeTarget(settings: AppSettings) {
        val lat = settings.manualLat
        val lon = settings.manualLon
        if (lat == null || lon == null) {
            island?.bind(settings, "LOCATION NEEDED", null)
            return
        }
        val now = ZonedDateTime.now()
        val requested = EventChoice.valueOf(settings.eventType.name)
        val (event, target) = SolarCalculator.nextEvent(now, lat, lon, requested)
        currentTarget = target
        currentTitle = eventLabel(event.name)
        island?.bind(settings, currentTitle, target)
        if (tickJob?.isActive != true) {
            tickJob = scope.launch {
                while (isActive) {
                    island?.updateCountdown()
                    if (currentTarget != null && !ZonedDateTime.now().isBefore(currentTarget)) {
                        recomputeTarget(PreferencesRepository.flow(this@IslandService).first())
                    }
                    delay(1000)
                }
            }
        }
    }

    private fun removeIsland() {
        island?.let { runCatching { windowManager.removeView(it) } }
        island = null
    }

    override fun onDestroy() {
        tickJob?.cancel(); settingsJob?.cancel(); scope.cancel(); removeIsland(); stopForeground(STOP_FOREGROUND_REMOVE); super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun eventLabel(name: String) = when (name) {
        "SUNRISE" -> "SUNRISE"
        "SOLAR_NOON" -> "SOLAR NOON"
        "SUNSET" -> "SUNSET"
        "SOLAR_MIDNIGHT" -> "SOLAR MIDNIGHT"
        else -> "SUNSET"
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, getString(R.string.island_service_channel), NotificationManager.IMPORTANCE_LOW).apply {
            description = getString(R.string.island_service_description)
        })
    }

    private fun notification(): Notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_menu_today)
        .setContentTitle("Sun Island is active")
        .setContentText("Your solar countdown is visible as an island.")
        .setOngoing(true)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    companion object { const val CHANNEL_ID = "sun_island"; const val NOTIFICATION_ID = 7001 }
}
