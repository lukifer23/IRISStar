package com.nervesparks.iris

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
@HiltAndroidApp
class IrisStarApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Always plant debug tree for now
        Timber.plant(Timber.DebugTree())
    }
}
