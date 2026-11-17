package com.spendly.app

import android.app.Application
import com.spendly.app.detection.work.SessionReconciliationWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SpendlyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionReconciliationWorker.schedulePeriodicWork(this)
    }
}
