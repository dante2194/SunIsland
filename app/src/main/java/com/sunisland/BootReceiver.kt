package com.sunisland

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.sunisland.data.PreferencesRepository
import com.sunisland.island.IslandService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = PreferencesRepository.flow(context).first()
                // Android 15+ requires a visible overlay before a background FGS start for this pattern.
                // On older Android releases we can restore the user-visible foreground service at boot.
                if (settings.autostartOnBoot && settings.islandEnabled &&
                    Build.VERSION.SDK_INT < 35 && Settings.canDrawOverlays(context)) {
                    ContextCompat.startForegroundService(context, Intent(context, IslandService::class.java))
                }
            } catch (_: Exception) {
                // OEM boot behavior is best-effort; the app does not bypass platform restrictions.
            } finally {
                pending.finish()
            }
        }
    }
}
