package com.lahsuak.sangeetmusicplayer.util


import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.lahsuak.sangeetmusicplayer.util.AppConstants.CHANNEL_ID

class MusicApp : Application() {
    private var manager: NotificationManager? = null

    companion object{
        lateinit var appContext: Context
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onCreate() {
        super.onCreate()
        appContext = this
        manager = getSystemService(NotificationManager::class.java)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel1 = NotificationChannel(
                CHANNEL_ID,
                AppConstants.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            channel1.description = AppConstants.CHANNEL_DESC
            manager?.createNotificationChannel(channel1)
        }
    }
}