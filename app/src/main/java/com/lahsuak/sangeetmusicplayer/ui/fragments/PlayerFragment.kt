package com.lahsuak.sangeetmusicplayer.ui.fragments

import android.app.Activity
import android.content.*
import android.content.Context.AUDIO_SERVICE
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.*
import android.view.*
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.gson.GsonBuilder
import com.lahsuak.sangeetmusicplayer.*
import com.lahsuak.sangeetmusicplayer.databinding.FragmentPlayerBinding
import com.lahsuak.sangeetmusicplayer.model.*
import com.lahsuak.sangeetmusicplayer.ui.activity.HomeActivity
import com.lahsuak.sangeetmusicplayer.ui.activity.HomeActivity.Companion.favoriteList
import com.lahsuak.sangeetmusicplayer.ui.activity.HomeActivity.Companion.fromHome
import com.lahsuak.sangeetmusicplayer.ui.activity.HomeActivity.Companion.fromPlayer
import com.lahsuak.sangeetmusicplayer.ui.activity.HomeActivity.Companion.historySongList
import com.lahsuak.sangeetmusicplayer.ui.activity.HomeActivity.Companion.isFavorite
import com.lahsuak.sangeetmusicplayer.ui.fragments.HomeFragment.Companion.list
import com.lahsuak.sangeetmusicplayer.ui.fragments.PlayListFragment.Companion.musicPlaylist
import com.lahsuak.sangeetmusicplayer.util.ActionPlaying
import com.lahsuak.sangeetmusicplayer.util.AppConstants.ARTIST_NAME
import com.lahsuak.sangeetmusicplayer.util.AppConstants.LAST_CURRENT_POSITION
import com.lahsuak.sangeetmusicplayer.util.AppConstants.LAST_DURATION
import com.lahsuak.sangeetmusicplayer.util.AppConstants.LAST_SONG_ID
import com.lahsuak.sangeetmusicplayer.util.AppConstants.LAST_SONG_POSITION
import com.lahsuak.sangeetmusicplayer.util.AppConstants.MUSIC_FILE
import com.lahsuak.sangeetmusicplayer.util.AppConstants.MUSIC_LAST_PLAYED
import com.lahsuak.sangeetmusicplayer.util.AppConstants.MY_SORT_PREF
import com.lahsuak.sangeetmusicplayer.util.AppConstants.NEW_OR_RECENT
import com.lahsuak.sangeetmusicplayer.util.AppConstants.PLAY_STATUS
import com.lahsuak.sangeetmusicplayer.util.AppConstants.SONG_NAME
import com.lahsuak.sangeetmusicplayer.util.MusicApp
import com.lahsuak.sangeetmusicplayer.util.SongUtil.addToPlaylistDialog
import com.lahsuak.sangeetmusicplayer.util.SongUtil.checkHistorySong
import com.lahsuak.sangeetmusicplayer.util.SongUtil.findPosition
import com.lahsuak.sangeetmusicplayer.util.SongUtil.getFavoriteIndex
import com.lahsuak.sangeetmusicplayer.util.SongUtil.getFormattedTime
import com.lahsuak.sangeetmusicplayer.util.SongUtil.getRandom
import com.lahsuak.sangeetmusicplayer.util.SongUtil.shareMusic
import com.lahsuak.sangeetmusicplayer.util.SongUtil.showDetailsDialog
import com.lahsuak.sangeetmusicplayer.util.service.MusicService
import com.lahsuak.sangeetmusicplayer.util.service.MusicService.Companion.fromFavorite
import com.lahsuak.sangeetmusicplayer.util.service.MusicService.Companion.fromHistory
import com.lahsuak.sangeetmusicplayer.util.service.MusicService.Companion.fromPlaylist
import com.lahsuak.sangeetmusicplayer.util.service.MusicService.Companion.isFav
import com.lahsuak.sangeetmusicplayer.util.service.MusicService.Companion.isMute
import com.lahsuak.sangeetmusicplayer.util.service.MusicService.Companion.isPlay
import com.lahsuak.sangeetmusicplayer.util.service.MusicService.Companion.isRepeat
import com.lahsuak.sangeetmusicplayer.util.service.MusicService.Companion.isShuffle
import com.lahsuak.sangeetmusicplayer.util.service.MusicService.Companion.playbackSpeed
import kotlinx.coroutines.*
import java.util.*

class PlayerFragment : Fragment(R.layout.fragment_player), ActionPlaying,
    ServiceConnection, AudioManager.OnAudioFocusChangeListener {

    lateinit var binding: FragmentPlayerBinding
    private val args: PlayerFragmentArgs by navArgs()

    private val activity = HomeActivity()
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    private var playList: ArrayList<Songs>? = ArrayList()
    private var playThread: Thread? = null
    private var prevThread: Thread? = null
    private var nextThread: Thread? = null
    private val handler = Handler(Looper.getMainLooper())
    private var tempPosition = 0
    private var isOrientationChanged = false
    private lateinit var preferencesMusic: SharedPreferences
    private lateinit var preferencesSort: SharedPreferences

    companion object {
        // var countingMap = HashMap<Int, Int>() //Creating HashMap
        var service: MusicService? = null
        lateinit var uri: Uri

        // private String textContent, artistName, path;
        private var position = -1
        private var currentPos = 0
        private var muteOrNot = 0
        private var playbackOption = 0
        private var favIndex = -1

        //Timer flags
        var min15 = false
        var min30 = false
        var min60 = false
    }

    private val someActivityResultLauncher =
        registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data
                //  val result1 = data!!.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            }
        }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Inflate the layout for this fragment
        binding = FragmentPlayerBinding.bind(view)
        setHasOptionsMenu(true)
        (requireActivity() as AppCompatActivity).supportActionBar!!.hide()
        preferencesMusic =
            requireActivity().getSharedPreferences(MUSIC_LAST_PLAYED, Context.MODE_PRIVATE)
        preferencesSort =
            requireActivity().getSharedPreferences(MY_SORT_PREF, AppCompatActivity.MODE_PRIVATE)

        audioManager =
            requireActivity().applicationContext.getSystemService(AppCompatActivity.AUDIO_SERVICE) as AudioManager

        binding.btnBack.setOnClickListener {
            onBackPressed()
        }

        if (savedInstanceState != null) {
            tempPosition = savedInstanceState.getInt("last_position")
            isOrientationChanged = savedInstanceState.getBoolean("changed")
        }

        getIntentMethod()
        setClickListeners()
        // Redirect system "Back" press to our dispatcher
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            backPressedDispatcher
        )

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (service != null && fromUser) {
                    service!!.seekTo(progress * 1000)
                    currentPos = progress * 1000
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        setAudioFocusChangeListener()
        updateSeekbar()
        binding.optionPlayback.setOnClickListener {
            shuffleOrRepeatClicked()
        }

        binding.songName.isSelected = true
        //intentMethod()
        muteOrNot = if (isMute == 0) {
            binding.volumeMute.setImageResource(R.drawable.ic_volume_mute)
            R.drawable.ic_volume_mute
        } else {
            binding.volumeMute.setImageResource(R.drawable.ic_volume_up)
            R.drawable.ic_volume_up
        }

    }

    private fun setClickListeners() {
        //Music Equalizer
        binding.btnEqualizer.setOnClickListener {
            try {
                val eqIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                eqIntent.putExtra(
                    AudioEffect.EXTRA_AUDIO_SESSION,
                    service!!.mediaPlayer!!.audioSessionId
                )
                eqIntent.putExtra(
                    AudioEffect.EXTRA_PACKAGE_NAME,
                    requireActivity().baseContext.packageName
                )
                eqIntent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                someActivityResultLauncher.launch(eqIntent)
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Equalizer feature not supported!!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        //More options
        binding.btnMore.setOnClickListener { v ->
            val popupMenu = PopupMenu(context, v)
            popupMenu.menuInflater.inflate(R.menu.player_menu, popupMenu.menu)
            popupMenu.show()
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.addToPlaylist -> addToPlaylistDialog(
                        requireContext(),
                        playList!!,
                        position
                    )

                    R.id.share -> shareMusic(requireContext(), playList!!, position)
                    R.id.details -> showDetailsDialog(requireContext(), playList!!, position)
                }
                true
            }
        }

        //Sleep Timer
        binding.btnTimer.setOnClickListener {
            val timer = min15 || min30 || min60
            if (!timer) {
                setTimer(it)
            } else {
                cancelTimer()
            }
        }
        //favorite method
        favIndex = getFavoriteIndex(playList!![position].id)
        if (isFavorite) {
            binding.btnFavorite.setImageResource(R.drawable.ic_favorite)
        } else
            binding.btnFavorite.setImageResource(R.drawable.ic_favorite_empty)

        binding.btnFavorite.setOnClickListener {
            favoriteClicked()
        }
        binding.volumeMute.setOnClickListener {
            muteClicked()
        }
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putInt("last_position", position)
        savedInstanceState.putBoolean("changed", true)
    }

    private val backPressedDispatcher = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            // Redirect to our own function
            this@PlayerFragment.onBackPressed()
        }
    }

    //check audio focus changes like incoming call and other media playing
    private fun setAudioFocusChangeListener() {
        audioManager = (requireActivity().getSystemService(AUDIO_SERVICE) as AudioManager?)!!

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                //.setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(this).build()
            audioManager?.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("deprecation")
            audioManager?.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        if (service != null) {
            //Audio not pause when go to home fragment, so for that check one more condition is fromPlayer = false
            if (service!!.mediaPlayer != null && !fromPlayer) {
                service!!.mediaPlayer!!.pause()
                service!!.showNotification(
                    isPlay,
                    isFav, playbackOption, 0
                )
                binding.play.setImageResource(R.drawable.ic_play)
            }
        } else {
            //play music
            if (service != null) {
                if (service!!.mediaPlayer != null) {
                    service!!.mediaPlayer!!.start()
                    service!!.showNotification(
                        isPlay,
                        isFav,
                        playbackOption,
                        1
                    )
                    binding.play.setImageResource(R.drawable.ic_pause)
                }
            }
        }
        //   fromPlayer = false
    }

    private fun updateSeekbar() {
        activity.runOnUiThread(object : Runnable {
            override fun run() {
                if (service != null) {
                    currentPos = service!!.currentPosition / 1000
                    binding.seekBar.progress = currentPos
                    binding.startTime.text = getFormattedTime(currentPos)
                }
                handler.postDelayed({ this.run() }, 1000)
            }
        })
    }

    private fun onBackPressed() {
        position = service!!.position
        val navController = findNavController()
        navController.previousBackStackEntry?.savedStateHandle?.set("key", position)
        navController.popBackStack()

        val editor: SharedPreferences.Editor = preferencesMusic.edit()
        editor.putString(MUSIC_FILE, uri.toString())
        editor.putString(ARTIST_NAME, playList!![position].artist)
        editor.putString(SONG_NAME, playList!![position].name)
        editor.putInt(LAST_SONG_POSITION, service!!.position)
        editor.putInt(LAST_CURRENT_POSITION, service!!.currentPosition)
        editor.putInt(LAST_DURATION, service!!.duration)
        editor.putBoolean(PLAY_STATUS, service!!.isPlaying())
        editor.putBoolean(NEW_OR_RECENT, true)
        if (fromHistory)
            editor.putString("fromWhere", "history")
        else if (fromFavorite)
            editor.putString("fromWhere", "fav")
        else if (fromPlaylist)
            editor.putString("fromWhere", "playlist")
        else
            editor.putString("fromWhere", "home")
        editor.apply()
    }

    override fun onPause() {
        super.onPause()
        //make fromPlayer flag to true because after this method it will check AudioFocus
        //and we don't want to pause song when leave this fragment
        val editor = preferencesMusic.edit()
        //  editor.putBoolean(Constants.IN_BACKGROUND, true)
        //editor.apply()
        editor.putString(MUSIC_FILE, uri.toString())
        editor.putString(ARTIST_NAME, playList!![position].artist)
        editor.putString(SONG_NAME, playList!![position].name)
        editor.putInt(LAST_SONG_POSITION, position)
        editor.putInt(LAST_CURRENT_POSITION, currentPos)
        editor.putInt(LAST_DURATION, service!!.duration)
        editor.putBoolean(PLAY_STATUS, service!!.isPlaying())
        editor.putInt(LAST_SONG_ID, playList!![position].id)
        editor.putBoolean(NEW_OR_RECENT, true)
        if (fromHistory)
            editor.putString("fromWhere", "history")
        else if (fromFavorite)
            editor.putString("fromWhere", "fav")
        else if (fromPlaylist)
            editor.putString("fromWhere", "playlist")
        else
            editor.putString("fromWhere", "home")

        editor.apply()

        fromHome = false
        fromPlayer = true
        requireActivity().unbindService(this)
    }

    override fun onResume() {
        super.onResume()
        if (fromPlayer) {
            if (isOrientationChanged) {
                position = tempPosition
            }
            val editor =
                requireActivity().applicationContext.getSharedPreferences(
                    MUSIC_LAST_PLAYED,
                    Context.MODE_PRIVATE
                )
                    .edit()
            //  editor.putBoolean(Constants.IN_BACKGROUND, true)
            //editor.apply()
            editor.putString(MUSIC_FILE, uri.toString())
            editor.putString(ARTIST_NAME, playList!![position].artist)
            editor.putString(SONG_NAME, playList!![position].name)
            editor.putInt(LAST_SONG_POSITION, position)
            editor.putInt(LAST_CURRENT_POSITION, currentPos)
            editor.putInt(LAST_DURATION, service!!.duration)
            editor.putBoolean(PLAY_STATUS, service!!.isPlaying())
            editor.putInt(LAST_SONG_ID, playList!![position].id)
            editor.putBoolean(NEW_OR_RECENT, true)
            if (fromHistory)
                editor.putString("fromWhere", "history")
            else if (fromFavorite)
                editor.putString("fromWhere", "fav")
            else if (fromPlaylist)
                editor.putString("fromWhere", "playlist")
            else
                editor.putString("fromWhere", "home")

            editor.apply()
        }
        playThread()
        prevThread()
        nextThread()
        val intent = Intent(requireContext(), MusicService::class.java)
        requireActivity().bindService(intent, this, AppCompatActivity.BIND_AUTO_CREATE)
    }

    //timer methods
    private fun setTimer(view: View) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.menuInflater.inflate(R.menu.timer_menu, popupMenu.menu)
        popupMenu.show()
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.firstTimer -> {
                    binding.btnTimer.setColorFilter(R.color.purple_500)
                    min15 = true

                    Thread {
                        Thread.sleep(15 * 60000.toLong())
                        if (min15)
                            exitApplication()
                    }.start()
                    Toast.makeText(
                        requireContext(),
                        "Music will stop after 15 minute",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                R.id.secondTimer -> {
                    binding.btnTimer.setColorFilter(R.color.purple_500)
                    min30 = true
                    Thread {
                        Thread.sleep(30 * 60000.toLong())
                        if (min30)
                            exitApplication()
                    }.start()
                    Toast.makeText(
                        requireContext(),
                        "Music will stop after 30 minute",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                R.id.thirdTimer -> {
                    binding.btnTimer.setColorFilter(R.color.purple_500)
                    min60 = true
                    Thread {
                        Thread.sleep(60 * 60000.toLong())
                        if (min60)
                            exitApplication()
                    }.start()
                    Toast.makeText(
                        requireContext(),
                        "Music will stop after 60 minute",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            true
        }
    }

    private fun cancelTimer() {
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle("Stop Timer")
            .setMessage("Do you want to stop timer?")
            .setPositiveButton("Stop") { dialog, _ ->
                binding.btnTimer.setColorFilter(com.google.android.material.R.color.design_default_color_on_secondary)
                //playClicked()
                if (service!!.isPlaying()) {
                    service!!.showNotification(
                        isPlay,
                        isFav,
                        playbackOption,
                        1
                    )
                    binding.play.setImageResource(R.drawable.ic_pause)
                    service!!.start()
                    binding.seekBar.max = service!!.duration / 1000
                    updateSeekbar()
                } else {
                    service!!.pause()
                    binding.play.setImageResource(R.drawable.ic_play)
                    service!!.showNotification(
                        isPlay,
                        isFav,
                        playbackOption,
                        0
                    )
                    binding.seekBar.max = service!!.duration / 1000
                    updateSeekbar()
                }
                min15 = false
                min30 = false
                min60 = false

                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
        val deleteDialog = builder.create()
        deleteDialog.show()
    }

    //timer method for exit
    private fun exitApplication() {
        if (service!!.isPlaying()) {
            activity.runOnUiThread {
                if (service != null) {
                    binding.play.setImageResource(R.drawable.ic_play)
                    binding.btnTimer.setColorFilter(com.google.android.material.R.color.design_default_color_on_secondary)
                }
            }
            service!!.pause()
            service!!.showNotification(isPlay, isFav, playbackOption, 0)
            if (min15)
                min15 = false
            else if (min30)
                min30 = false
            else if (min60)
                min60 = false
        }
    }

    //initialization and start service
    private fun getIntentMethod() {
        playList = if (fromFavorite) {
            favoriteList
        } else if (fromHistory) {
            historySongList
        } else if (fromPlaylist) {
            val playlistPosition = args.playlistPos
            musicPlaylist.ref!![playlistPosition].playlist
        } else {
            list
        }

        position = args.position
        if (!fromFavorite && !fromPlaylist && !fromHistory) {
            if (args.songId != playList!![position].id) {
                position = findPosition(args.songId, list)
            }
        }

        //when a device orientation change then save previous position and assign to position
        if (isOrientationChanged)
            position = tempPosition
        uri = Uri.parse(playList!![position].path)

        val intent = Intent(requireContext(), MusicService::class.java)
        intent.putExtra("servicePosition", position)
        intent.putExtra("serviceCurPosition", currentPos)
        intent.putExtra("songUri", uri.toString())
        requireActivity().startService(intent)

        //add SONGS in historySongList
        if (!fromHistory) {
            checkHistorySong(playList!![position].id, playList!!, position)
        }
    }

    private fun getMetaData() {
        if (!service!!.isBackground()) {
            val refresh = preferencesSort.getBoolean("refresh", false)

            if (refresh) {
                val preference1: SharedPreferences =
                    requireActivity().getSharedPreferences(MUSIC_LAST_PLAYED, Context.MODE_PRIVATE)
                val duration = preference1.getInt(LAST_DURATION, 0)
                binding.endTime.text = getFormattedTime(duration / 1000)
            } else {
                val duration1 = playList!![position].duration!!.toInt()
                binding.endTime.text = getFormattedTime(duration1 / 1000)
            }
        } else if (service!!.isBackground()) {
            val duration1 = playList!![position].duration!!.toInt()
            binding.endTime.text = getFormattedTime(duration1 / 1000)
        }
        Glide.with(MusicApp.appContext).load(playList?.get(position)?.artUri)
            .error(R.drawable.image_background)
            .transition(DrawableTransitionOptions.withCrossFade()).into(binding.songImage)
    }

    private fun playThread() {
        playThread = object : Thread() {
            override fun run() {
                super.run()
                binding.play.setOnClickListener { playClicked() }
            }
        }
        (playThread as Thread).start()
    }

    override fun playClicked() {
        if (service!!.isPlaying()) {
            isPlay = false
            playbackSpeed = 0
            service!!.pause()
            binding.play.setImageResource(R.drawable.ic_play)
            service!!.showNotification(isPlay, isFav, playbackOption, playbackSpeed)
            binding.seekBar.max = service!!.duration / 1000
            updateSeekbar()
        } else {
            isPlay = true
            playbackSpeed = 1
            service!!.showNotification(isPlay, isFav, playbackOption, playbackSpeed)
            binding.play.setImageResource(R.drawable.ic_pause)
            service!!.start()
            binding.seekBar.max = service!!.duration / 1000
            //this is for notification when song is playing
            // then we don't want to remove notification,so again call notification method
            lifecycleScope.launch {
                delay(2)
                withContext(Dispatchers.Main) {
                    if (service!!.isPlaying()) {
                        isPlay = true
                        service!!.showNotification(isPlay, isFav, playbackOption, playbackSpeed)
                    } else {
                        isPlay = false
                        service!!.showNotification(isPlay, isFav, playbackOption, playbackSpeed)
                    }
                }

            }
            updateSeekbar()
        }
    }

//random method removed

    private fun prevThread() {
        prevThread = object : Thread() {
            override fun run() {
                super.run()
                binding.previous.setOnClickListener { prevClicked() }
            }
        }
        (prevThread as Thread).start()
    }

    override fun prevClicked() {
        if (service!!.isPlaying()) {
            service!!.stop()
            service!!.release()
            position = if (position - 1 < 0) playList!!.size - 1 else position - 1
            uri = Uri.parse(playList!![position].path)
            service!!.createMediaPlayer(position, uri.toString())
            getMetaData()
            binding.songName.text = playList!![position].name
            binding.artistName.text = playList!![position].artist
            binding.seekBar.max = service!!.duration / 1000

            updateSeekbar()

            service!!.onCompleted()
            service!!.showNotification(isPlay, isFav, playbackOption, playbackSpeed)
            binding.play.setBackgroundResource(R.drawable.ic_pause)
            service!!.start()
        } else {
            service!!.stop()
            service!!.release()
            position = if (position - 1 < 0) playList!!.size - 1 else position - 1
            uri = Uri.parse(playList!![position].path)
            service!!.createMediaPlayer(position, uri.toString())
            getMetaData()
            binding.songName.text = playList!![position].name
            binding.artistName.text = playList!![position].artist
            binding.seekBar.max = service!!.duration / 1000

            updateSeekbar()
            service!!.onCompleted()
            service!!.showNotification(isPlay, isFav, playbackOption, playbackSpeed)
            binding.play.setBackgroundResource(R.drawable.ic_play)
        }
        if (!fromHistory) {
            checkHistorySong(playList!![position].id, playList!!, position)
        }
        // favoriteClicked()
        checkFavIndex()
    }

    private fun nextThread() {
        nextThread = object : Thread() {
            override fun run() {
                super.run()
                binding.next.setOnClickListener { nextClicked() }
            }
        }
        (nextThread as Thread).start()
    }

    override fun nextClicked() {
        if (service!!.isPlaying()) {
            service!!.stop()
            service!!.release()
            if (isShuffle && !isRepeat) {
                position = getRandom(playList!!.size - 1)
            } else if (!isShuffle && !isRepeat) {
                position = (position + 1) % playList!!.size
            }
            uri = Uri.parse(playList!![position].path)
            service!!.createMediaPlayer(position, uri.toString())
            getMetaData()
            binding.songName.text = playList!![position].name
            binding.artistName.text = playList!![position].artist
            binding.seekBar.max = service!!.duration / 1000

            updateSeekbar()
            service!!.onCompleted()
            service!!.start()
            service!!.showNotification(isPlay, isFav, playbackOption, playbackSpeed)
            binding.play.setBackgroundResource(R.drawable.ic_pause)
        } else {
            service!!.stop()
            service!!.release()
            if (isShuffle && !isRepeat) {
                position = getRandom(playList!!.size - 1)
            } else if (!isShuffle && !isRepeat) {
                position = (position + 1) % playList!!.size
            }
            uri = Uri.parse(playList!![position].path)
            service!!.createMediaPlayer(position, uri.toString())
            getMetaData()
            binding.songName.text = playList!![position].name
            binding.artistName.text = playList!![position].artist
            binding.seekBar.max = service!!.duration / 1000
            updateSeekbar()
            service!!.onCompleted()
            service!!.showNotification(isPlay, isFav, playbackOption, playbackSpeed)
            binding.play.setBackgroundResource(R.drawable.ic_pause)
        }
        if (!fromHistory) {
            checkHistorySong(playList!![position].id, playList!!, position)
        }
        checkFavIndex()
    }

    private fun checkFavIndex() {
        favIndex = getFavoriteIndex(playList!![position].id)
        if (favIndex == -1) {
            binding.btnFavorite.setImageResource(R.drawable.ic_favorite_empty)
            isFav = false
            isFavorite = false
        } else {
            binding.btnFavorite.setImageResource(R.drawable.ic_favorite)
            isFav = true
            isFavorite = true
        }
        lifecycleScope.launch {
            delay(2) //delay for notification changes
            withContext(Dispatchers.Main) {
                isPlay = service!!.isPlaying()
                service!!.showNotification(isPlay, isFav, playbackOption, playbackSpeed)
            }
        }
    }

    override fun favoriteClicked() {
        if (isFavorite) {
            Snackbar.make(requireView(), "Removed from favorites", Snackbar.LENGTH_LONG)
                .show()
            binding.btnFavorite.setImageResource(R.drawable.ic_favorite_empty)
            favIndex = getFavoriteIndex(playList!![position].id)
            if (favIndex != -1) {
                favoriteList!!.removeAt(favIndex)
            }
            isFav = false
            isFavorite = false
        } else {
            Snackbar.make(requireView(), "Added to favorites", Snackbar.LENGTH_LONG)
                .show()
            isFavorite = true
            binding.btnFavorite.setImageResource(R.drawable.ic_favorite)
            favoriteList!!.add(playList!![position])
            isFav = true
        }
        if (service!!.isPlaying()) {
            service!!.showNotification(isPlay, isFav, playbackOption, playbackSpeed)
        } else {
            service!!.showNotification(isPlay, isFav, playbackOption, playbackSpeed)
        }
    }

    private fun muteClicked() {
        //service.muteClicked();
        if (isMute == 1) {
            audioManager!!.adjustVolume(AudioManager.ADJUST_MUTE, AudioManager.FLAG_PLAY_SOUND)
            binding.volumeMute.setImageResource(R.drawable.ic_volume_mute)
            muteOrNot = R.drawable.ic_volume_mute
            isMute = 0
        } else {
            audioManager!!.adjustVolume(AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_PLAY_SOUND)
            binding.volumeMute.setImageResource(R.drawable.ic_volume_up)
            muteOrNot = R.drawable.ic_volume_up
            isMute = 1
        }
    }

    override fun shuffleOrRepeatClicked() {
        playbackOption++
        playbackOption %= 3
        when (playbackOption) {
            0 -> {
                binding.optionPlayback.contentDescription = "Sequential"
                binding.optionPlayback.setImageResource(R.drawable.ic_repeat)
                isRepeat = false
                isShuffle = false
            }

            1 -> {
                if (!service!!.isBackground())
                    Toast.makeText(requireContext(), "Song repeated", Toast.LENGTH_SHORT).show()
                binding.optionPlayback.contentDescription = "Repeat music"
                binding.optionPlayback.setImageResource(R.drawable.ic_repeat_one)
                isRepeat = true
            }

            2 -> {
                if (!service!!.isBackground())
                    Toast.makeText(requireContext(), "Song shuffled", Toast.LENGTH_SHORT).show()
                binding.optionPlayback.contentDescription = "Shuffle all music"
                binding.optionPlayback.setImageResource(R.drawable.ic_shuffle)
                isRepeat = false
                isShuffle = true
            }
        }
        if (service!!.isPlaying()) {
            service!!.showNotification(isPlay, isFav, playbackOption, playbackSpeed)
        } else {
            service!!.showNotification(isPlay, isFav, playbackOption, playbackSpeed)
        }
    }

    override fun setCurrentProgress(position: Int) {
        if (service != null) {
            binding.seekBar.progress = position
        }
    }

    override fun onServiceConnected(name: ComponentName, iBinder: IBinder) {
        val binder = iBinder as MusicService.MyBinder
        service = binder.service
        service!!.setCallBack(this)
        binding.artistName.text = playList!![position].artist
        binding.songName.text = playList!![position].name
        val duration: Int = service!!.duration / 1000
        binding.seekBar.max = duration
        getMetaData() // will put in get intent Method

        service!!.onCompleted()
        //checkFavIndex()
        favIndex = getFavoriteIndex(playList!![position].id)
        if (favIndex == -1) {
            binding.btnFavorite.setImageResource(R.drawable.ic_favorite_empty)
            isFav = false
            isFavorite = false
        } else {
            binding.btnFavorite.setImageResource(R.drawable.ic_favorite)
            isFav = true
            isFavorite = true
        }
        if (service!!.isPlaying()) {
            binding.play.setImageResource(R.drawable.ic_pause)
            service!!.showNotification(isPlay, isFav, playbackOption, playbackSpeed)
        } else {
            service!!.showNotification(isPlay, isFav, playbackOption, playbackSpeed)
        }
    }

    override fun onServiceDisconnected(name: ComponentName) {
        service = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // removeAudioFocus()
        (requireActivity() as AppCompatActivity).supportActionBar!!.show()

        val editor: SharedPreferences.Editor = preferencesMusic.edit()
        editor.putString(MUSIC_FILE, uri.toString())
        editor.putString(ARTIST_NAME, playList!![position].artist)
        editor.putString(SONG_NAME, playList!![position].name)
        editor.putInt(LAST_SONG_POSITION, position)
        editor.putInt(LAST_CURRENT_POSITION, service!!.currentPosition)
        editor.putInt(LAST_DURATION, service!!.duration)
        editor.putBoolean(PLAY_STATUS, service!!.isPlaying())
        editor.putInt(LAST_SONG_ID, playList!![position].id)
        editor.putBoolean(NEW_OR_RECENT, true)

        if (fromHistory)
            editor.putString("fromWhere", "history")
        else if (fromFavorite)
            editor.putString("fromWhere", "fav")
        else if (fromPlaylist)
            editor.putString("fromWhere", "playlist")
        else
            editor.putString("fromWhere", "home")
        editor.apply()
        //store recently played songs list into shared preference
        val historyEditor =
            requireActivity().getSharedPreferences("HISTORY", Context.MODE_PRIVATE).edit()
        val json2 = GsonBuilder().create().toJson(historySongList)
        historyEditor.putString("recentSongs", json2)
        historyEditor.apply()
    }
}