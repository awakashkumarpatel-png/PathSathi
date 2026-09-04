package com.pathsathi.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pathsathi.app.data.local.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * AlarmManager alarms are cleared on reboot. If Safety Check-in was enabled,
 * re-arm the recurring "are you safe?" alarm so the feature keeps working
 * without the user needing to reopen Settings.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = AppPreferences.checkInSettings(context).first()
                if (settings.enabled) {
                    CheckInScheduler.scheduleNextTrigger(context, settings.intervalMinutes)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
