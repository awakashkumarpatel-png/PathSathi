package com.pathsathi.app.alerts
import android.content.*
import androidx.work.*
import com.pathsathi.app.PathSathiApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit
class BootReminderReceiver:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent){if(i.action!=Intent.ACTION_BOOT_COMPLETED)return;AlertScheduler.ensureChannel(c);WorkManager.getInstance(c).enqueue(OneTimeWorkRequestBuilder<BootCheckWorker>().setInitialDelay(2,TimeUnit.MINUTES).build())}}
class BootCheckWorker(app:Context,p:WorkerParameters):Worker(app,p){override fun doWork():Result{val a=applicationContext as? PathSathiApp?:return Result.success();runBlocking{a.repository.observeActiveTrip().first()?.let{AlertScheduler.scheduleReminder(applicationContext,"Path Sathi","Your active trip to ${it.destination} is ready.",1)}};return Result.success()}}
