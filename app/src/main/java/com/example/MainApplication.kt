package com.example

import android.app.Application
import com.example.util.CrashReportManager

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize global uncaught exception crash handler on launch
        CrashReportManager.init(this)
    }
}
