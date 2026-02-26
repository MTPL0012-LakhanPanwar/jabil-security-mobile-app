package com.jabil.securityapp

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner

class CameraSecurity: Application() {
    override fun onCreate() {
        super.onCreate()
//        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver(this))
    }
}