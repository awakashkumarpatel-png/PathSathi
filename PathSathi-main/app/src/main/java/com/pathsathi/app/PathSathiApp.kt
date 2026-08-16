package com.pathsathi.app

import android.app.Application
import com.pathsathi.app.alerts.AlertScheduler
import com.pathsathi.app.data.db.AppDatabase
import com.pathsathi.app.data.repository.PathSathiRepository

class PathSathiApp : Application() {

    lateinit var repository: PathSathiRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getInstance(this)
        repository = PathSathiRepository.getInstance(db)
        AlertScheduler.ensureChannel(this)
    }
}
