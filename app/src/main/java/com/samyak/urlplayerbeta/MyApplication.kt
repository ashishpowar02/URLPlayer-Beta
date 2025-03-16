package com.samyak.urlplayerbeta

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.firebase.database.FirebaseDatabase
import com.samyak.urlplayerbeta.AdManage.Openads
import com.samyak.urlplayerbeta.AdManage.loadAdUnits
import com.samyak.urlplayerbeta.AdManage.loadInterstitialAdIfNull

class MyApplication : Application() {


    override fun onCreate() {
        super.onCreate()
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        MobileAds.initialize(this) {
            loadAdUnits {
                loadInterstitialAdIfNull(this)

            }
        }


    }
}