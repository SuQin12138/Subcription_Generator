package com.example.regionswitcher

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RegionSwitcherApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Initialize any global application components here
    }
}
