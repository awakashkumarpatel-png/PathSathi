package com.pathsathi.app.alerts
import android.app.*
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import java.util.concurrent.TimeUnit
object AlertScheduler{const val CHANNEL_ID="pathsathi_alerts";fun ensureChannel(c:Context){if(Build.VERSION.SDK_INT>=26)c.getSystemService(NotificationManager::class.java)?.createNotificationChannel(NotificationChannel(CHANNEL_ID,"Path Sathi Alerts",NotificationManager.IMPORTANCE_DEFAULT))};fun scheduleReminder(c:Context,title:String,msg:String,delay:Long){ensureChannel(c);val d=Data.Builder().putString("title",title).putString("message",msg).build();WorkManager.getInstance(c).enqueue(OneTimeWorkRequestBuilder<ReminderWorker>().setInitialDelay(delay.coerceAtLeast(1),TimeUnit.MINUTES).setInputData(d).build())};fun scheduleTripReminders(c:Context,dest:String,start:Long){val d=start.coerceAtLeast(1);scheduleReminder(c,"Path Sathi trip reminder","Your trip to $dest is coming up.",d);scheduleReminder(c,"Path Sathi budget check","Review your budget and itinerary for $dest.",d+120);scheduleReminder(c,"Path Sathi travel check","Open Live Trip for automatic guidance and your next stop.",d+240)}}
class ReminderWorker(app:Context,p:WorkerParameters):Worker(app,p){override fun doWork():Result{val n=NotificationCompat.Builder(applicationContext,AlertScheduler.CHANNEL_ID).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(inputData.getString("title")?:"Path Sathi").setContentText(inputData.getString("message")?:"Trip reminder").setAutoCancel(true).build();applicationContext.getSystemService(NotificationManager::class.java)?.notify(System.currentTimeMillis().toInt(),n);return Result.success()}}
