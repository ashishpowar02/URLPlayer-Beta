package com.samyak.urlplayerbeta

import android.app.Application
import com.samyak.urlplayerbeta.AdManage.Openads

class MyApplication : Application() {

    private lateinit var appOpenAdManager: Openads

    override fun onCreate() {
        super.onCreate()
        appOpenAdManager = Openads(this)
    }
}