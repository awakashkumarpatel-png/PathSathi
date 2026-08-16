package com.pathsathi.app.alerts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * On boot, this is where any persisted (Room-backed) upcoming reminders would be
 * re-enqueued with WorkManager, since one-time WorkRequests are cleared on reboot
 * on some OEMs. Kept minimal for now: ensures the notification channel exists.
 */
class BootReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            AlertScheduler.ensureChannel(context)
            // Future: query Room for trips with status ACTIVE/PLANNED and re-schedule
            // their upcoming departure/budget reminders here.
        }
    }
}
