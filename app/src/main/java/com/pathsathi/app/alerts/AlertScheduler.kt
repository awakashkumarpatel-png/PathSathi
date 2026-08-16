package com.pathsathi.app.alerts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/** Fully offline local reminders — departure/next-destination/schedule/budget alerts. No server needed. */
object AlertScheduler {
    const val CHANNEL_ID = "pathsathi_alerts"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Path Sathi Alerts", NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Trip reminders, schedule and budget alerts" }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun scheduleReminder(context: Context, title: String, message: String, delayMinutes: Long) {
        ensureChannel(context)
        val data = Data.Builder()
            .putString("title", title)
            .putString("message", message)
            .build()
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(data)
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }
}

class ReminderWorker(appContext: Context, params: WorkerParameters) : Worker(appContext, params) {
    override fun doWork(): Result {
        val title = inputData.getString("title") ?: "Path Sathi"
        val message = inputData.getString("message") ?: "You have a trip reminder."

        val notification = NotificationCompat.Builder(applicationContext, AlertScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager?.notify(System.currentTimeMillis().toInt(), notification)
        return Result.success()
    }
}
