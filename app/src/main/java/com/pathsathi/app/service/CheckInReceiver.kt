package com.pathsathi.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.pathsathi.app.MainActivity
import com.pathsathi.app.data.local.AppPreferences
import com.pathsathi.app.data.local.CheckInEntity
import com.pathsathi.app.data.local.PathSathiDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CheckInReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_CHECKIN = "com.pathsathi.app.action.CHECKIN_TRIGGER"
        const val ACTION_CONFIRM_SAFE = "com.pathsathi.app.action.CHECKIN_CONFIRM_SAFE"
        const val ACTION_CHECK_MISSED = "com.pathsathi.app.action.CHECKIN_CHECK_MISSED"
        const val EXTRA_SCHEDULED_AT = "scheduled_at"

        private const val CHANNEL_ID = "pathsathi_checkin"
        private const val CHANNEL_ID_ALERT = "pathsathi_checkin_missed"
        private const val NOTIFICATION_ID_PROMPT = 2001
        private const val NOTIFICATION_ID_MISSED = 2002
    }

    override fun onReceive(context: Context, intent: Intent) {
        createChannels(context)
        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                when (intent.action) {
                    ACTION_TRIGGER_CHECKIN -> handleTrigger(context)
                    ACTION_CONFIRM_SAFE -> handleConfirmSafe(context)
                    ACTION_CHECK_MISSED -> handleCheckMissed(context, intent.getLongExtra(EXTRA_SCHEDULED_AT, 0L))
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleTrigger(context: Context) {
        val settings = AppPreferences.checkInSettings(context).first()
        val enabled = settings.enabled
        val grace = settings.graceMinutes

        val scheduledAt = System.currentTimeMillis()
        val db = PathSathiDatabase.getInstance(context)
        db.checkInDao().insert(CheckInEntity(scheduledAt = scheduledAt, status = "pending"))

        showPromptNotification(context, scheduledAt)
        if (enabled) {
            CheckInScheduler.scheduleMissedCheck(context, scheduledAt, grace)
        }
    }

    private suspend fun handleConfirmSafe(context: Context) {
        CheckInActions.confirmSafe(context)
    }

    private suspend fun handleCheckMissed(context: Context, scheduledAt: Long) {
        val db = PathSathiDatabase.getInstance(context)
        val entry = db.checkInDao().getByScheduledAt(scheduledAt)
        if (entry != null && entry.status == "pending") {
            db.checkInDao().update(entry.copy(status = "missed", batteryPercent = batteryPercent(context)))
            showMissedAlertNotification(context)
        }
        // Keep the recurring cycle alive even if this one was missed.
        val intervalMin = AppPreferences.checkInSettings(context).first().intervalMinutes
        CheckInScheduler.scheduleNextTrigger(context, intervalMin)
    }

    private fun batteryPercent(context: Context): Int? {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val pct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        return if (pct in 0..100) pct else null
    }

    private fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Safety Check-in", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Periodic \"are you safe?\" prompts during a trek"
                }
            )
            manager?.createNotificationChannel(
                NotificationChannel(CHANNEL_ID_ALERT, "Missed Check-in Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Alerts when a scheduled safety check-in was missed"
                    enableVibration(true)
                }
            )
        }
    }

    private fun showPromptNotification(context: Context, scheduledAt: Long) {
        val confirmIntent = Intent(context, CheckInReceiver::class.java).apply { action = ACTION_CONFIRM_SAFE }
        val confirmPending = PendingIntent.getBroadcast(
            context, 4001, confirmIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("open_checkin", true)
        }
        val contentPending = PendingIntent.getActivity(
            context, 4002, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("Safety check-in")
            .setContentText("Are you safe? Tap \"I Am Safe\" to confirm.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPending)
            .addAction(android.R.drawable.checkbox_on_background, "I Am Safe", confirmPending)
            .build()
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID_PROMPT, notification)
    }

    private fun showMissedAlertNotification(context: Context) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("open_sos", true)
        }
        val contentPending = PendingIntent.getActivity(
            context, 4003, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ALERT)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Missed safety check-in")
            .setContentText("No response was received. Tap to open SOS if you need help.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPending)
            .build()
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID_MISSED, notification)
    }
}
