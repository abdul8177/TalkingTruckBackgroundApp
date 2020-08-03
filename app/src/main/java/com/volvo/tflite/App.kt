package com.volvo.tflite

import android.app.Application
import android.content.Context
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import com.microsoft.appcenter.distribute.Distribute


class App : Application() {

    companion object {
        private const val TAG = "App"
        var context: Context? = null
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        initAppCenter()
        createNotificationChannel(applicationContext)
    }

    private fun initAppCenter() {
        @Suppress("ConstantConditionIf")
        if (BuildConfig.BUILD_TYPE == "qa") {
            AppCenter.start(
                    this, "430f1545-65c2-4bc8-9834-93df7f9778f8",
                    Analytics::class.java, Crashes::class.java, Distribute::class.java
            )
        }
    }
}