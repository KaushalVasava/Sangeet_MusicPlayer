package com.lahsuak.sangeetmusicplayer.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lahsuak.sangeetmusicplayer.service.SangeetMusicService
import com.lahsuak.sangeetmusicplayer.util.Constants.ACTION_FAVORITE
import com.lahsuak.sangeetmusicplayer.util.Constants.ACTION_NEXT
import com.lahsuak.sangeetmusicplayer.util.Constants.ACTION_PLAY
import com.lahsuak.sangeetmusicplayer.util.Constants.ACTION_PREV
import com.lahsuak.sangeetmusicplayer.util.Constants.ACTION_SHUFFLE_OR_REPEAT

class MusicNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, SangeetMusicService::class.java)
        if (intent.action != null) {
            when (intent.action) {
                ACTION_PLAY -> {
                    serviceIntent.putExtra("myActionName", ACTION_PLAY)
                    context.startService(serviceIntent)
                }
                ACTION_PREV -> {
                    serviceIntent.putExtra("myActionName", ACTION_PREV)
                    context.startService(serviceIntent)
                }
                ACTION_NEXT -> {
                    serviceIntent.putExtra("myActionName", ACTION_NEXT) //,intent.getAction();
                    context.startService(serviceIntent)
                }
                ACTION_FAVORITE -> {
                    serviceIntent.putExtra("myActionName", ACTION_FAVORITE) //,intent.getAction();
                    context.startService(serviceIntent)
                }
                ACTION_SHUFFLE_OR_REPEAT -> {
                    serviceIntent.putExtra("myActionName", ACTION_SHUFFLE_OR_REPEAT) //,intent.getAction();
                    context.startService(serviceIntent)
                }
            }
        }
    }
}