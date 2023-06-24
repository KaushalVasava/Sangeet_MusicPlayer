package com.lahsuak.sangeetmusicplayer.util.service

import android.app.NotificationManager
import android.media.MediaPlayer.OnCompletionListener
import android.os.IBinder
import android.media.MediaPlayer
import android.support.v4.media.session.MediaSessionCompat
import android.content.Intent
import com.lahsuak.sangeetmusicplayer.ui.activity.HomeActivity
import android.app.PendingIntent.*
import android.app.Service
import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import com.lahsuak.sangeetmusicplayer.*
import com.lahsuak.sangeetmusicplayer.util.AppConstants.ACTION_FAVORITE
import com.lahsuak.sangeetmusicplayer.util.AppConstants.ACTION_NEXT
import com.lahsuak.sangeetmusicplayer.util.AppConstants.ACTION_PLAY
import com.lahsuak.sangeetmusicplayer.util.AppConstants.ACTION_PREV
import com.lahsuak.sangeetmusicplayer.util.AppConstants.ACTION_SHUFFLE_OR_REPEAT
import com.lahsuak.sangeetmusicplayer.util.AppConstants.CHANNEL_ID
import java.util.ArrayList
import android.graphics.Bitmap
import com.lahsuak.sangeetmusicplayer.model.Songs
import com.lahsuak.sangeetmusicplayer.util.receiver.MusicNotificationReceiver
import com.lahsuak.sangeetmusicplayer.util.ActionPlaying
import com.lahsuak.sangeetmusicplayer.util.AppConstants.ARTIST_NAME
import com.lahsuak.sangeetmusicplayer.util.AppConstants.IN_BACKGROUND
import com.lahsuak.sangeetmusicplayer.util.AppConstants.LAST_CURRENT_POSITION
import com.lahsuak.sangeetmusicplayer.util.AppConstants.LAST_DURATION
import com.lahsuak.sangeetmusicplayer.util.AppConstants.LAST_SONG_ID
import com.lahsuak.sangeetmusicplayer.util.AppConstants.LAST_SONG_POSITION
import com.lahsuak.sangeetmusicplayer.util.AppConstants.MUSIC_FILE
import com.lahsuak.sangeetmusicplayer.util.AppConstants.MUSIC_LAST_PLAYED
import com.lahsuak.sangeetmusicplayer.util.AppConstants.NEW_OR_RECENT
import com.lahsuak.sangeetmusicplayer.util.AppConstants.PLAY_STATUS
import com.lahsuak.sangeetmusicplayer.util.AppConstants.SONG_NAME
import java.lang.Exception

class MusicService : Service(), OnCompletionListener {
    private val binder: IBinder = MyBinder()
    private var actionPlaying: ActionPlaying? = null
    var mediaPlayer: MediaPlayer? = null
    private var uri: Uri? = null
    var position = -1

    companion object {
        @JvmStatic
        var playlistPosition = -1
        var isPlaylistSongClicked = false
        var fromFavorite = false
        var fromPlaylist = false
        var fromHistory = false
        var isInterruption1 = false //when interruption from playlist
        var isInterruption2 = false //when interruption from favorites
        var isInterruption3 = false //when interruption from history

        //old one
        var songFiles: ArrayList<Songs>? = ArrayList()
        var sortOrder: String = "SortByName"
        var songList: ArrayList<Songs>? = ArrayList() // for global
        var isShuffle = false
        var isRepeat = false
        var isCurrent = false
        var isMute = 1 //100 for unmute & -100 for mute

        //new variables for seekbar changes
        var isFav = false //false for not favorite and true for favorite
        var isPlay = true
        var playbackSpeed = 0  //for pause it is 0 and for play it is 1
        var playbackOption = 0  //Use for sequence, repeat and shuffle playback
    }

    private var mediaSession: MediaSessionCompat? = null

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSessionCompat(baseContext, "song")
        getSystemService(NOTIFICATION_SERVICE)
        val callback = object : MediaSessionCompat.Callback() {
            override fun onSeekTo(pos: Long) {
                //super.onSeekTo(pos)
                seekTo(pos.toInt())
                showNotification(isPlay, isFav, playbackOption, playbackSpeed)
            }
        }
        mediaSession!!.setCallback(callback)
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    inner class MyBinder : Binder() {
        val service: MusicService
            get() = this@MusicService
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        // HOME

        val myPosition = intent.getIntExtra("servicePosition", -1)
        val myCurrentPos = intent.getIntExtra("serviceCurPosition", 0)
        val uri = intent.getStringExtra("songUri")
        val actionName = intent.getStringExtra("myActionName")

        if (myPosition != -1) {
            playMusic(myPosition, myCurrentPos, uri)
        }
        if (actionName != null) {
            when (actionName) {
                ACTION_PLAY -> playPauseButtonClicked()
                ACTION_PREV -> prevButtonClicked()
                ACTION_NEXT -> {
                    nextButtonClicked()
                }
                //ACTION_MUTE -> muteClicked()
                ACTION_FAVORITE -> favoriteClicked()
                ACTION_SHUFFLE_OR_REPEAT -> shuffleOrRepeatClicked()
            }
        }

        return START_REDELIVER_INTENT
    }

    fun createMediaPlayer(innerPosition: Int, uri: String?) {
        position = innerPosition
        var uri1: Uri? = null
        if (uri != null) uri1 = Uri.parse(uri)
        if (uri1 == null) uri1 = Uri.parse(songFiles!![position].path)
        val editor = getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE).edit()
        editor.putString(MUSIC_FILE, uri1.toString())
        editor.putString(ARTIST_NAME, songFiles!![position].artist)
        editor.putString(SONG_NAME, songFiles!![position].name)
        editor.putInt(LAST_SONG_POSITION, position)
        editor.putInt(LAST_CURRENT_POSITION, 0)
        editor.putInt(LAST_DURATION, 0)
        editor.putInt(
            LAST_SONG_ID,
            songFiles!![position].id
        ) //added to get current song in recent player
        editor.apply()
        mediaPlayer = MediaPlayer.create(baseContext, uri1)
    }

    private fun playMusic(startPosition: Int, currentPos: Int, uri: String?) {
        songFiles = songList!! //new MusicActivity().getDataList();
        position = startPosition
        Log.d("H", "playMusic: $position")
        if (currentPos != 0) isCurrent = true
        if (mediaPlayer != null) {
            if (isCurrent == false) {
                Log.d("KMV", "if  1= false")
                mediaPlayer!!.stop()
                mediaPlayer!!.release()
                ///when first time go to player fragment
                // and get back to home fragment then this code run
                if (songFiles != null) {
                    createMediaPlayer(position, uri)
                    mediaPlayer!!.start()
                }
            } else {
                val preferences: SharedPreferences =
                    getSharedPreferences(MUSIC_LAST_PLAYED, Context.MODE_PRIVATE)
                val isRecent = preferences.getBoolean(NEW_OR_RECENT, false)
                Log.d("KMV", "RECENT check $isRecent") //old code with recent preference
                if (isRecent) {
                    Log.d("KMV", "if  2= true") //old code with recent // preference
                    setProgress(currentPos)
                    isCurrent = true
                } else {
                    mediaPlayer!!.stop()
                    mediaPlayer!!.release()
                    if (songFiles != null) {
                        createMediaPlayer(position, uri)
                        mediaPlayer!!.start()
                    }
                }
            }
        } else {
            if (isCurrent == false) {
                val preferences: SharedPreferences =
                    getSharedPreferences(MUSIC_LAST_PLAYED, Context.MODE_PRIVATE)
                val isRecent = preferences.getBoolean(NEW_OR_RECENT, false)
                if (isRecent) {
                    Log.d("KMV", "if  2= true") //old code with recent // preference
                    val curPos = preferences.getInt(LAST_CURRENT_POSITION, 0)
                    val checkStatus = preferences.getBoolean(PLAY_STATUS, false)
                    createMediaPlayer(position, uri)
                    start()
                    seekTo(curPos)
                    setProgress(curPos)

                    if (!checkStatus) {
                        Log.d("LIFE", "pause $curPos") //old code with recent // preference
                        pause()
                    }
                    val editor = preferences.edit()
                    editor.putInt(LAST_CURRENT_POSITION, curPos)
                    editor.apply()
                    isCurrent = true
                } else {
                    createMediaPlayer(position, uri)
                    mediaPlayer!!.start()
                    isCurrent = true // changed. before it is not present here
                }
            } else {
                Log.d("KMV", "else 2  = true$currentPos")
                createMediaPlayer(position, uri)
                mediaPlayer!!.start()
                seekTo(currentPos)
                setProgress(currentPos)
                isCurrent = false
            }
        }
    }

    fun showNotification(
        isPlay: Boolean,
        isFavorite: Boolean,
        playbackOption: Int,
        playbackSpeed: Int
    ) {
        val pendingIntentFlag =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                FLAG_IMMUTABLE
            } else {
                FLAG_UPDATE_CURRENT
            }
        val intent = Intent(this, HomeActivity::class.java)
        val contentIntent = getActivity(this, 0, intent, pendingIntentFlag)

        val prevIntent =
            Intent(this, MusicNotificationReceiver::class.java).setAction(ACTION_PREV)
        val prevPendingIntent =
            getBroadcast(this, 0, prevIntent, pendingIntentFlag)

        val playIntent =
            Intent(this, MusicNotificationReceiver::class.java).setAction(ACTION_PLAY)
        val playPendingIntent =
            getBroadcast(this, 0, playIntent, pendingIntentFlag)

        val nextIntent =
            Intent(this, MusicNotificationReceiver::class.java).setAction(ACTION_NEXT)
        val nextPendingIntent =
            getBroadcast(this, 0, nextIntent, pendingIntentFlag)

        val favoriteIntent =
            Intent(this, MusicNotificationReceiver::class.java).setAction(ACTION_FAVORITE)
        val favoritePendingIntent =
            getBroadcast(this, 0, favoriteIntent, pendingIntentFlag)

        val shuffleOrRepeatIntent =
            Intent(this, MusicNotificationReceiver::class.java).setAction(ACTION_SHUFFLE_OR_REPEAT)
        val shuffleOrRepeatPendingIntent =
            getBroadcast(this, 0, shuffleOrRepeatIntent, pendingIntentFlag)

        val art = Uri.parse(songFiles!![position].artUri!!)
        val artwork: Bitmap? = try {
            val stream = contentResolver.openInputStream(art)
            BitmapFactory.decodeStream(stream)
        } catch (e: Exception) {
            BitmapFactory.decodeResource(resources, R.drawable.ic_music)
        }

        val option = when (playbackOption) {
            0 -> {
                R.drawable.ic_repeat
            }
            1 -> {
                R.drawable.ic_repeat_one
            }

            2 -> {
                R.drawable.ic_shuffle
            }

            else -> {R.drawable.ic_repeat}
        }
        var favoriteOrNot = R.drawable.ic_favorite_empty
        if (isFavorite)
            favoriteOrNot = R.drawable.ic_favorite

        var playPauseButton = R.drawable.ic_play_notification
        if (isPlay)
            playPauseButton = R.drawable.ic_pause_notification

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(playPauseButton)
            .setContentTitle(songFiles!![position].name)
            .setContentText(songFiles!![position].artist)
            .setLargeIcon(artwork)
            .addAction(favoriteOrNot, "Favorite", favoritePendingIntent)
            .addAction(R.drawable.ic_previous, "Previous", prevPendingIntent)
            .addAction(playPauseButton, "Pause", playPendingIntent)
            .addAction(R.drawable.ic_next, "Next", nextPendingIntent)
            .addAction(option, "Shuffle", shuffleOrRepeatPendingIntent)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle() //   .setShowActionsInCompactView(0, 1, 2)
                    .setMediaSession(mediaSession!!.sessionToken)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mediaSession!!.setMetadata(
                MediaMetadataCompat.Builder()
                    .putLong(
                        MediaMetadataCompat.METADATA_KEY_DURATION,
                        mediaPlayer?.duration?.toLong() ?: 0L
                    )
                    .build()
            )
            mediaSession!!.setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setState(
                        PlaybackStateCompat.STATE_PLAYING, mediaPlayer?.currentPosition?.toLong()?:0L,
                        playbackSpeed.toFloat()
                    )
                    .setActions(PlaybackStateCompat.ACTION_SEEK_TO)

                    .build()
            )
        }

        if (!isPlaying()) {
            //WHEN APP IN BACKGROUND AND MUSIC IS NOT PLAYING THEN WE CAN REMOVE NOTIFICATION
            notification.setOngoing(false)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(2)
            startForeground(2, notification.build())
            stopForeground(false)
        } else
            startForeground(2, notification.build())
        // notificationManager.notify(0, notification);
    }

    fun onCompleted() {
        mediaPlayer?.setOnCompletionListener(this)
    }

    override fun onCompletion(mp: MediaPlayer) {
        if (actionPlaying != null) {
            actionPlaying!!.nextClicked()
        }
        if (mediaPlayer != null) {
            createMediaPlayer(position, null)
            mediaPlayer!!.start()
            onCompleted()
        }
    }

    fun setCallBack(actionPlaying: ActionPlaying?) {
        this.actionPlaying = actionPlaying
    }

    fun isBackground(): Boolean {
        val preferences = getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE)
        return preferences.getBoolean(IN_BACKGROUND, false)
    }

    fun isPlaying(): Boolean {
        val editor: SharedPreferences.Editor =
            getSharedPreferences(MUSIC_LAST_PLAYED, Context.MODE_PRIVATE).edit()
        editor.putBoolean(PLAY_STATUS, mediaPlayer?.isPlaying ?: false)
        editor.apply()
        return mediaPlayer?.isPlaying ?: false
    }

    fun start() {
        mediaPlayer?.start()
    }

    fun pause() {
        mediaPlayer?.pause()
    }

    fun stop() {
        mediaPlayer!!.stop()
    }

    fun release() {
        mediaPlayer!!.release()
    }

    val currentPosition: Int
        get() = mediaPlayer?.currentPosition ?: 0
    val duration: Int
        get() = mediaPlayer?.duration ?: 0

    fun seekTo(position: Int) {
        mediaPlayer!!.seekTo(position)
    }

    private fun nextButtonClicked() {
        if (actionPlaying != null) {
            uri = Uri.parse(songFiles!![position].path)
            actionPlaying!!.nextClicked()
        }
    }

    private fun prevButtonClicked() {
        if (actionPlaying != null) {
            uri = Uri.parse(songFiles!![position].path)
            actionPlaying!!.prevClicked()
        }
    }

    private fun playPauseButtonClicked() {
        if (actionPlaying != null) {
            actionPlaying!!.playClicked()
        }
    }

    private fun favoriteClicked() {
        if (actionPlaying != null) {
            actionPlaying!!.favoriteClicked()
        }
    }

    private fun setProgress(position: Int) {
        if (actionPlaying != null) {
            actionPlaying!!.setCurrentProgress(position)
        }
    }

    private fun shuffleOrRepeatClicked() {
        if (actionPlaying != null) {
            actionPlaying!!.shuffleOrRepeatClicked()
        }
    }
}