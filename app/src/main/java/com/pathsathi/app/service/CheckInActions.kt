package com.pathsathi.app.service

import android.app.NotificationManager
import android.content.Context
import android.os.BatteryManager
import com.pathsathi.app.data.local.AppPreferences
import com.pathsathi.app.data.local.PathSathiDatabase
import com.pathsathi.app.location.LocationHelper
import kotlinx.coroutines.flow.first

/** Confirm-safe logic shared between the notification action button and the in-app Check-in screen. */
object CheckInActions {

    suspend fun confirmSafe(context: Context) {
        val db = PathSathiDatabase.getInstance(context)
        val latest = db.checkInDao().getLatest()
        if (latest != null && latest.status == "pending") {
            val location = try { LocationHelper(context).getCurrentLocation() } catch (_: Exception) { null }
            db.checkInDao().update(
                latest.copy(
                    status = "confirmed",
                    respondedAt = System.currentTimeMillis(),
                    latitude = location?.first,
                    longitude = location?.second,
                    batteryPercent = batteryPercent(context)
                )
            )
        }
        CheckInScheduler.cancelMissedCheck(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.cancel(2001)

        val intervalMin = AppPreferences.checkInSettings(context).first().intervalMinutes
        CheckInScheduler.scheduleNextTrigger(context, intervalMin)
    }

    private fun batteryPercent(context: Context): Int? {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val pct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        return if (pct in 0..100) pct else null
    }
}
