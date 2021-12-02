package com.lahsuak.sangeetmusicplayer.util


import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.lahsuak.sangeetmusicplayer.util.Constants.CHANNEL_1_ID

class App : Application() {
    var manager: NotificationManager? = null
    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onCreate() {
        super.onCreate()
        manager = getSystemService(NotificationManager::class.java)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel1 = NotificationChannel(
                CHANNEL_1_ID,
                "Channel Music",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel1.description = "This is Channel 1"
            manager!!.createNotificationChannel(channel1)
        }
    }
}