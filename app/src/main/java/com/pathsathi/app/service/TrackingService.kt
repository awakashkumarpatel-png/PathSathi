package com.pathsathi.app.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.pathsathi.app.MainActivity
import com.pathsathi.app.data.local.PathSathiDatabase
import com.pathsathi.app.data.local.TrackPointEntity
import com.pathsathi.app.data.local.TrackingSessionEntity
import com.pathsathi.app.data.model.TrackPoint
import com.pathsathi.app.data.local.AppPreferences
import com.pathsathi.app.data.local.GpsAccuracyMode
import com.pathsathi.app.data.repository.TrekRepository
import com.pathsathi.app.location.GeoUtils
import android.os.Vibrator
import android.os.VibrationEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TrackingService : Service() {

    companion object {
        const val ACTION_START = "com.pathsathi.app.action.START_TRACKING"
        const val ACTION_PAUSE = "com.pathsathi.app.action.PAUSE_TRACKING"
        const val ACTION_RESUME = "com.pathsathi.app.action.RESUME_TRACKING"
        const val ACTION_STOP = "com.pathsathi.app.action.STOP_TRACKING"
        const val EXTRA_TREK_NAME = "trek_name"

        private const val CHANNEL_ID = "pathsathi_tracking"
        private const val NOTIFICATION_ID = 1001
        private const val DEVIATION_THRESHOLD_M = 150.0

        fun start(context: Context, trekName: String) {
            val intent = Intent(context, TrackingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TREK_NAME, trekName)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // Defensive: on some OEMs/Android versions a foreground-service start can be
                // rejected (e.g. background-start restrictions). Tracking simply won't start
                // rather than crashing the app; TrackingScreen's own state will reflect that
                // nothing started, since TrackingRepository.state.isTracking stays false.
            }
        }

        fun sendAction(context: Context, action: String) {
            context.startService(Intent(context, TrackingService::class.java).apply { this.action = action })
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    // Per-session state - all explicitly reset in beginTracking() so a new
    // session never inherits data from a previous one.
    private var lastLocation: Location? = null
    private var sessionStartTime: Long = 0L
    private var pausedAccumulatedMs: Long = 0L
    private var pauseStartedAt: Long = 0L
    private var timerJob: kotlinx.coroutines.Job? = null
    private var routeWaypoints: List<Pair<Double, Double>> = emptyList()
    private var wasOffRoute: Boolean = false

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> beginTracking(intent.getStringExtra(EXTRA_TREK_NAME) ?: "Trek")
            ACTION_PAUSE -> pauseTracking()
            ACTION_RESUME -> resumeTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun beginTracking(trekName: String) {
        if (!hasLocationPermission()) {
            // Safety net: TrackingScreen is responsible for requesting permission before
            // ever calling TrackingService.start(), but if this is somehow reached without
            // it, we must still call startForeground() here. The service was launched via
            // startForegroundService() (Android 8+), and Android requires startForeground()
            // to be called shortly after onStartCommand() regardless of outcome - skipping
            // it (as a bare stopSelf()) crashes with a "did not then call
            // Service.startForeground()" ANR. So: show a brief explanatory notification,
            // satisfy that requirement, then stop cleanly.
            startForeground(
                NOTIFICATION_ID,
                buildNotification("Location permission needed", "Grant location access in PathSathi to start tracking")
            )
            stopSelf()
            return
        }

        // Full reset - guarantees no leftover state from a previous session
        // (this was previously missing for lastLocation, causing the first
        // point of a new session to be measured against the old session's
        // last point).
        lastLocation = null
        sessionStartTime = System.currentTimeMillis()
        pausedAccumulatedMs = 0L
        pauseStartedAt = 0L
        wasOffRoute = false
        routeWaypoints = TrekRepository.treks.find { it.name == trekName }?.routeWaypoints.orEmpty()

        TrackingRepository.reset()
        TrackingRepository.update {
            it.copy(trekName = trekName, isTracking = true, isPaused = false)
        }
        startForeground(NOTIFICATION_ID, buildNotification("Tracking started", "0.00 km · 00:00"))
        startLocationUpdates()
        startTimer()
    }

    private fun pauseTracking() {
        if (!TrackingRepository.state.value.isTracking || TrackingRepository.state.value.isPaused) return
        pauseStartedAt = System.currentTimeMillis()
        TrackingRepository.update { it.copy(isPaused = true) }
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        updateNotification("Tracking paused")
    }

    @SuppressLint("MissingPermission")
    private fun resumeTracking() {
        if (!TrackingRepository.state.value.isPaused) return
        if (pauseStartedAt > 0L) {
            pausedAccumulatedMs += System.currentTimeMillis() - pauseStartedAt
            pauseStartedAt = 0L
        }
        // A fresh GPS fix after resuming avoids measuring a big "jump" against
        // a stale pre-pause location.
        lastLocation = null
        TrackingRepository.update { it.copy(isPaused = false) }
        if (hasLocationPermission()) startLocationUpdates()
        updateNotification("Tracking resumed")
    }

    private fun stopTracking() {
        val finalState = TrackingRepository.state.value
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        timerJob?.cancel()

        // If stopped while paused, don't count the open pause interval.
        val effectiveElapsedMs = finalState.elapsedMs

        // Persist the completed session and its points to Room
        serviceScope.launch {
            val db = PathSathiDatabase.getInstance(applicationContext)
            val avgSpeed = if (effectiveElapsedMs > 0) {
                (finalState.distanceMeters / 1000.0) / (effectiveElapsedMs / 3_600_000.0)
            } else 0.0

            val sessionId = db.trackingDao().insertSession(
                TrackingSessionEntity(
                    trekName = finalState.trekName,
                    startTime = sessionStartTime,
                    endTime = System.currentTimeMillis(),
                    distanceMeters = finalState.distanceMeters,
                    maxSpeedKmh = finalState.maxSpeedKmh,
                    avgSpeedKmh = avgSpeed,
                    elevationGainM = finalState.elevationGainM,
                    elevationLossM = finalState.elevationLossM,
                    maxAltitudeM = finalState.maxAltitudeM
                )
            )
            db.trackingDao().insertPoints(
                finalState.points.map { p ->
                    TrackPointEntity(
                        sessionId = sessionId,
                        latitude = p.latitude,
                        longitude = p.longitude,
                        altitude = 0.0,
                        timestamp = p.timestamp
                    )
                }
            )
        }

        // Reset per-session service state so the next START begins clean
        // even if the service instance is reused by the OS.
        lastLocation = null
        pausedAccumulatedMs = 0L
        pauseStartedAt = 0L

        TrackingRepository.update { it.copy(isTracking = false, isPaused = false) }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (!hasLocationPermission()) return

        // Respect the user's GPS accuracy / battery saver settings: lower
        // accuracy modes trade update frequency for battery life, which
        // matters a lot on a multi-day trek with no charging.
        val gpsMode = AppPreferences.gpsModeBlocking(applicationContext)
        val batterySaver = AppPreferences.batterySaverBlocking(applicationContext)
        val priority = if (batterySaver || gpsMode == GpsAccuracyMode.BATTERY_SAVER) {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        } else if (gpsMode == GpsAccuracyMode.BALANCED) {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        } else {
            Priority.PRIORITY_HIGH_ACCURACY
        }
        val intervalMs = when {
            batterySaver || gpsMode == GpsAccuracyMode.BATTERY_SAVER -> 15_000L
            gpsMode == GpsAccuracyMode.BALANCED -> 8_000L
            else -> 4_000L
        }

        val request = LocationRequest.Builder(priority, intervalMs)
            .setMinUpdateDistanceMeters(5f)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { onNewLocation(it) }
            }
        }
        locationCallback = callback
        fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
    }

    private fun onNewLocation(location: Location) {
        val prev = lastLocation
        val current = TrackingRepository.state.value

        var newDistance = current.distanceMeters
        var speedKmh = (location.speed * 3.6).let { if (it.isFinite()) it else 0.0 }
        var elevationGain = current.elevationGainM
        var elevationLoss = current.elevationLossM

        if (prev != null) {
            newDistance += prev.distanceTo(location)
            val altDiff = location.altitude - prev.altitude
            if (altDiff > 0) elevationGain += altDiff else elevationLoss += -altDiff
        }

        lastLocation = location

        // Route Deviation: compare the live fix against the trek's planned
        // route polyline. Fires a one-time vibration + banner when crossing
        // the threshold, and auto-clears once back within range.
        var isOffRoute = false
        var deviationDistance = 0.0
        var bearingToRoute = 0f
        if (routeWaypoints.size >= 1) {
            val deviation = GeoUtils.distanceToRoute(
                Pair(location.latitude, location.longitude),
                routeWaypoints
            )
            if (deviation != null) {
                deviationDistance = deviation.distanceMeters
                isOffRoute = deviationDistance > DEVIATION_THRESHOLD_M
                bearingToRoute = GeoUtils.bearingDegrees(
                    location.latitude, location.longitude,
                    deviation.nearestPoint.first, deviation.nearestPoint.second
                )
                if (isOffRoute && !wasOffRoute) {
                    vibrateOnce()
                }
                wasOffRoute = isOffRoute
            }
        }

        TrackingRepository.update {
            it.copy(
                points = it.points + TrackPoint(location.latitude, location.longitude, System.currentTimeMillis()),
                distanceMeters = newDistance,
                currentSpeedKmh = speedKmh,
                maxSpeedKmh = maxOf(it.maxSpeedKmh, speedKmh),
                currentAltitudeM = location.altitude,
                maxAltitudeM = maxOf(it.maxAltitudeM, location.altitude),
                elevationGainM = elevationGain,
                elevationLossM = elevationLoss,
                gpsAccuracyM = location.accuracy,
                isOffRoute = isOffRoute,
                deviationDistanceM = deviationDistance,
                bearingToRouteDeg = bearingToRoute
            )
        }

        val notifText = if (isOffRoute) {
            String.format("⚠ Off route by %.0fm · %.2f km · %s", deviationDistance, newDistance / 1000.0, formatElapsed())
        } else {
            String.format("%.2f km · %s", newDistance / 1000.0, formatElapsed())
        }
        updateNotification(notifText)
    }

    private fun vibrateOnce() {
        val vibrator = getSystemService(Vibrator::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(400)
        }
    }

    /** Elapsed active tracking time = wall clock since start, minus all paused time. */
    private fun computeElapsedMs(): Long {
        val now = System.currentTimeMillis()
        val openPause = if (pauseStartedAt > 0L) now - pauseStartedAt else 0L
        return (now - sessionStartTime) - pausedAccumulatedMs - openPause
    }

    private fun startTimer() {
        timerJob = serviceScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                if (!TrackingRepository.state.value.isPaused) {
                    TrackingRepository.update { it.copy(elapsedMs = computeElapsedMs()) }
                }
            }
        }
    }

    private fun formatElapsed(): String {
        val ms = TrackingRepository.state.value.elapsedMs
        val minutes = (ms / 60000) % 60
        val hours = ms / 3600000
        return String.format("%02d:%02d", hours, minutes)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Trek Tracking", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Shows live trek tracking progress" }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification("PathSathi Tracking", text))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
    }
}
