package com.pathsathi.app.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Schedules the recurring "Are you safe?" safety check-in alarm and the
 * follow-up "missed check-in" alarm. Uses AlarmManager (exact, wake-device)
 * rather than WorkManager so a check-in still fires promptly even in deep
 * doze, which matters for a trekking-safety feature.
 */
object CheckInScheduler {

    private const val REQUEST_TRIGGER = 3001
    private const val REQUEST_MISSED = 3002

    private fun triggerIntent(context: Context): PendingIntent {
        val intent = Intent(context, CheckInReceiver::class.java).apply {
            action = CheckInReceiver.ACTION_TRIGGER_CHECKIN
        }
        return PendingIntent.getBroadcast(
            context, REQUEST_TRIGGER, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun missedIntent(context: Context, scheduledAt: Long): PendingIntent {
        val intent = Intent(context, CheckInReceiver::class.java).apply {
            action = CheckInReceiver.ACTION_CHECK_MISSED
            putExtra(CheckInReceiver.EXTRA_SCHEDULED_AT, scheduledAt)
        }
        return PendingIntent.getBroadcast(
            context, REQUEST_MISSED, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun alarmManager(context: Context) =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** Schedules the next "are you safe?" prompt [intervalMinutes] from now. */
    fun scheduleNextTrigger(context: Context, intervalMinutes: Int) {
        val triggerAt = System.currentTimeMillis() + intervalMinutes * 60_000L
        setExact(context, triggerAt, triggerIntent(context))
    }

    /** Schedules the missed-check alarm [graceMinutes] after a trigger fired. */
    fun scheduleMissedCheck(context: Context, scheduledAt: Long, graceMinutes: Int) {
        val missedAt = scheduledAt + graceMinutes * 60_000L
        setExact(context, missedAt, missedIntent(context, scheduledAt))
    }

    fun cancelMissedCheck(context: Context) {
        alarmManager(context).cancel(missedIntent(context, 0L))
    }

    fun cancelAll(context: Context) {
        alarmManager(context).cancel(triggerIntent(context))
        cancelMissedCheck(context)
    }

    private fun setExact(context: Context, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        val am = alarmManager(context)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                // No exact-alarm permission granted - fall back to an inexact
                // alarm so the feature still works, just with less precise timing.
                am.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                return
            }
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } catch (_: SecurityException) {
            am.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }
}
