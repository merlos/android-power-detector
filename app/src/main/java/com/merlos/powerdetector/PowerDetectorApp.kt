package com.merlos.powerdetector

import android.app.Application
import androidx.work.Configuration

class PowerDetectorApp : Application(), Configuration.Provider {
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setMinimumLoggingLevel(android.util.Log.DEBUG).build()
}
