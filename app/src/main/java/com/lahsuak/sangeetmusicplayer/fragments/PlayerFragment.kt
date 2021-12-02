package com.lahsuak.sangeetmusicplayer.fragments


import android.content.*
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.*
import android.util.Log
import androidx.fragment.app.Fragment
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lahsuak.sangeetmusicplayer.*
import com.lahsuak.sangeetmusicplayer.util.Constants.ARTIST_NAME
import com.lahsuak.sangeetmusicplayer.util.Constants.LAST_CURRENT_POSITION
import com.lahsuak.sangeetmusicplayer.util.Constants.LAST_DURATION
import com.lahsuak.sangeetmusicplayer.util.Constants.LAST_SONG_ID
import com.lahsuak.sangeetmusicplayer.util.Constants.LAST_SONG_POSITION
import com.lahsuak.sangeetmusicplayer.util.Constants.MUSIC_FILE
import com.lahsuak.sangeetmusicplayer.util.Constants.MUSIC_LAST_PLAYED
import com.lahsuak.sangeetmusicplayer.util.Constants.MY_SORT_PREF
import com.lahsuak.sangeetmusicplayer.util.Constants.NEW_OR_RECENT
import com.lahsuak.sangeetmusicplayer.util.Constants.PLAY_STATUS
import com.lahsuak.sangeetmusicplayer.util.Constants.SONG_NAME
import com.lahsuak.sangeetmusicplayer.fragments.HomeFragment.Companion.list
import com.lahsuak.sangeetmusicplayer.fragments.PlayListFragment.Companion.musicPlaylist
import com.lahsuak.sangeetmusicplayer.service.SangeetMusicService.Companion.isMute
import com.lahsuak.sangeetmusicplayer.service.SangeetMusicService.Companion.isRepeat
import com.lahsuak.sangeetmusicplayer.service.SangeetMusicService.Companion.isShuffle
import java.util.*
import kotlin.collections.ArrayList
import android.content.Intent
import android.app.Activity
import android.content.Context.AUDIO_SERVICE
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import java.lang.Exception
import android.view.*
import com.google.android.material.snackbar.Snackbar

import com.google.gson.GsonBuilder
import com.lahsuak.sangeetmusicplayer.databinding.FragmentPlayerBinding
import com.lahsuak.sangeetmusicplayer.activity.HomeActivity.Companion.favoriteList
import com.lahsuak.sangeetmusicplayer.activity.HomeActivity.Companion.fromHome
import com.lahsuak.sangeetmusicplayer.activity.HomeActivity.Companion.fromPlayer
import com.lahsuak.sangeetmusicplayer.activity.HomeActivity.Companion.historySongList
import com.lahsuak.sangeetmusicplayer.activity.HomeActivity.Companion.isFavorite
import android.widget.ArrayAdapter
import android.widget.AdapterView.OnItemClickListener
import com.google.android.material.textfield.TextInputEditText
import com.lahsuak.sangeetmusicplayer.activity.HomeActivity
import com.lahsuak.sangeetmusicplayer.model.*
import com.lahsuak.sangeetmusicplayer.service.SangeetMusicService
import com.lahsuak.sangeetmusicplayer.util.ActionPlaying
import android.os.Build
import androidx.lifecycle.lifecycleScope
import com.lahsuak.sangeetmusicplayer.service.SangeetMusicService.Companion.fromFavorite
import com.lahsuak.sangeetmusicplayer.service.SangeetMusicService.Companion.fromHistory
import com.lahsuak.sangeetmusicplayer.service.SangeetMusicService.Companion.fromPlaylist
import com.lahsuak.sangeetmusicplayer.service.SangeetMusicService.Companion.isFav
import com.lahsuak.sangeetmusicplayer.service.SangeetMusicService.Companion.isPlay
import com.lahsuak.sangeetmusicplayer.service.SangeetMusicService.Companion.playbackSpeed
import kotlinx.coroutines.*
import java.lang.Runnable

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
    private val mainActivity = HomeActivity()
    private var tempPosition = 0
    private var isOrientationChanged = false

    companion object {
        // var countingMap = HashMap<Int, Int>() //Creating HashMap
        var service: SangeetMusicService? = null
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

    override fun onPause() {
        super.onPause()
        //make fromPlayer flag to true because after this method it will check AudioFocus
        //and we don't want to pause song when leave this fragment
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

        fromHome = false
        fromPlayer = true
        requireActivity().unbindService(this)
        Log.d("SAVED", "onPause")
    }

    override fun onResume() {
        super.onResume()
        Log.d("SAVED", "onResume")
        if (fromPlayer) {
            if (isOrientationChanged) {
                position = tempPosition
                Log.d("SAVED", "onResume: temp$tempPosition and $position")
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
        val intent = Intent(requireContext(), SangeetMusicService::class.java)
        requireActivity().bindService(intent, this, AppCompatActivity.BIND_AUTO_CREATE)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentPlayerBinding.inflate(layoutInflater, container, false)
        Log.d("SAVED", "onCreate")

        setHasOptionsMenu(true)
        (requireActivity() as AppCompatActivity).supportActionBar!!.hide()


        binding.btnBack.setOnClickListener {
            onBackPressed()
        }
        audioManager =
            requireActivity().applicationContext.getSystemService(AppCompatActivity.AUDIO_SERVICE) as AudioManager
        //audioManager = (requi.getSystemService(AUDIO_SERVICE) as AudioManager?)!!

        if (savedInstanceState != null) {
            tempPosition = savedInstanceState.getInt("last_position")
            isOrientationChanged = savedInstanceState.getBoolean("changed")
            Log.d("SAVED", "saved fetch : $position , $isOrientationChanged")
        }

        getIntentMethod()
        //More options
        binding.btnMore.setOnClickListener { v ->
            val popupMenu = PopupMenu(context, v)
            popupMenu.menuInflater.inflate(R.menu.player_menu, popupMenu.menu)
            popupMenu.show()
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.addToPlaylist -> addToPlaylistDialog(
                        requireContext(),
                        container,
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

        val someActivityResultLauncher =
            registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data
                    //  val result1 = data!!.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                }
            }

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

        //favorite method
        favIndex = getFavoriteIndex(playList!![position].id)
        if (isFavorite) {
            binding.btnFavorite.setImageResource(R.drawable.ic_favorite)
        } else
            binding.btnFavorite.setImageResource(R.drawable.ic_favorite_empty)

        binding.btnFavorite.setOnClickListener {
            favoriteClicked()
        }

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
        //   val te=  requestAudioFocus()
        /**METHOD-1 using handler.postdelay to update text and seekbar */
        updateSeekbar()
        binding.optionPlayback.setOnClickListener {
            shuffleOrRepeatClicked()
        }

        binding.songName.isSelected = true
        //intentMethod()
        if (isMute == 0) {
            binding.volumeMute.setImageResource(R.drawable.ic_volume_mute)
            muteOrNot = R.drawable.ic_volume_mute
        } else {
            binding.volumeMute.setImageResource(R.drawable.ic_volume_up)
            muteOrNot = R.drawable.ic_volume_up
        }
        binding.volumeMute.setOnClickListener {
            muteClicked()
        }
        return binding.root
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        Log.d("SAVED", "saved last : $position")
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
        Log.d("AUDIO", "onAudioFocusChange: #$focusChange")
        if (service != null) {
            //Audio not pause when go to home fragment, so for that check one more condition is fromPlayer = false
            if (service!!.mediaPlayer != null && fromPlayer == false) {
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
                    /**
                     * Problem 2 : service.getCurrentPosition()
                     * temp. solution -> currentPos = 0
                     * */
                    currentPos = service!!.currentPosition / 1000
                    binding.seekBar.progress = currentPos
                    binding.startTime.text = formattedTime(currentPos)
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

        val editor: SharedPreferences.Editor =
            requireActivity().getSharedPreferences(MUSIC_LAST_PLAYED, Context.MODE_PRIVATE).edit()
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
                binding.btnTimer.setColorFilter(R.color.design_default_color_on_secondary)
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
                    binding.btnTimer.setColorFilter(R.color.design_default_color_on_secondary)
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
        if (fromFavorite) {
            Log.d("POS", "getIntentMethod: from FAVORITE ${favoriteList!!.size}")
            playList = favoriteList
        } else if (fromHistory) {
            playList = historySongList
        } else if (fromPlaylist) {
            val playlistPosition = args.playlistPos
            playList = musicPlaylist.ref!![playlistPosition].playlist
        } else {
            playList = list
        }

        position = args.position
        if (!fromFavorite && !fromPlaylist && !fromHistory) {
            if (args.songId == playList!![position].id) {
                Log.d("POS", "FOUND $position")
            } else {
                position = findPosition(args.songId, list)
            }
        }

        //when a device orientation change then save previous position and assign to position
        if (isOrientationChanged)
            position = tempPosition
        Log.d("SAVED", "on intent method $tempPosition : $position")
        uri = Uri.parse(playList!![position].path)

        val intent = Intent(requireContext(), SangeetMusicService::class.java)
        intent.putExtra("servicePosition", position)
        intent.putExtra("serviceCurPosition", currentPos)
        intent.putExtra("songUri", uri.toString())

        Log.d("POS", "after intent : $position $uri")
        requireActivity().startService(intent)

        //add SONGS in historySongList
        if (!fromHistory) {
            checkHistorySong(playList!![position].id, playList!!, position)
        }
    }

//animation for song image

//formattedTime for seekbar time

    private fun getMetaData(uri1: String) {
        if (!service!!.isBackground()) {
            Log.d("D", "if")
            val preferences =
                requireActivity().getSharedPreferences(MY_SORT_PREF, AppCompatActivity.MODE_PRIVATE)
            val refresh = preferences.getBoolean("refresh", false)

            if (refresh) {
                val preference1: SharedPreferences =
                    requireActivity().getSharedPreferences(MUSIC_LAST_PLAYED, Context.MODE_PRIVATE)
                val duration = preference1.getInt(LAST_DURATION, 0)
                binding.endTime.text = formattedTime(duration / 1000)
            } else {
                val duration1 = playList!![position].duration!!.toInt()
                binding.endTime.text = formattedTime(duration1 / 1000)
            }
        } else if (service!!.isBackground()) {
            Log.d("D", "else")
            val duration1 = playList!![position].duration!!.toInt()
            binding.endTime.text = formattedTime(duration1 / 1000)
        }
        if (service!!.isBackground()) {
            val artUri = Uri.parse(playList!![position].artUri!!)
            var haveImage = true
            try {
                val stream =
                    requireActivity().applicationContext.contentResolver.openInputStream(artUri)
            } catch (e: Exception) {
                haveImage = false
            }
            if (haveImage)
                binding.songImage.setImageURI(artUri)
            else
                binding.songImage.setImageResource(R.drawable.image_background)
        } else {
            val artUri = Uri.parse(playList!![position].artUri!!)
            var haveImage = true
            try {
                val stream = requireContext().contentResolver.openInputStream(artUri)
            } catch (e: Exception) {
                haveImage = false
            }
            if (haveImage) {
                binding.songImage.setImageURI(artUri)
                songImageAnimation(
                    requireActivity().applicationContext, binding.songImage, artUri
                )
            } else {
                binding.songImage.setImageResource(R.drawable.image_background)
            }
        }
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
            getMetaData(uri.toString())
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
            getMetaData(uri.toString())
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
//        binding.btnFavorite.setImageResource(R.drawable.ic_favorite_empty)
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
            getMetaData(uri.toString())
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
            getMetaData(uri.toString())
            binding.songName.text = playList!![position].name
            binding.artistName.text = playList!![position].artist
            binding.seekBar.max = service!!.duration / 1000

            updateSeekbar()

            service!!.onCompleted()
//        service!!.showNotification(false, isFav, playbackOption,playbackSpeed)
//        binding.play.setBackgroundResource(R.drawable.ic_play)
            service!!.showNotification(isPlay, isFav, playbackOption, playbackSpeed)
            binding.play.setBackgroundResource(R.drawable.ic_pause)
        }
        if (!fromHistory) {
            checkHistorySong(playList!![position].id, playList!!, position)
        }
        //favoriteClicked()
        checkFavIndex()

    }

    private fun checkFavIndex() {
        favIndex = getFavoriteIndex(playList!![position].id)
        if (favIndex == -1) {
            binding.btnFavorite.setImageResource(R.drawable.ic_favorite_empty)
            isFav = false
//            favoriteList!!.add(playList!![position])
            isFavorite = false
        } else {
            binding.btnFavorite.setImageResource(R.drawable.ic_favorite)
            isFav = true
//            favoriteList!!.removeAt(favIndex)
            isFavorite = true
        }
        lifecycleScope.launch {
            delay(2) //delay for notification changes
            Log.d("CO", "checkFavIndex: ")
            withContext(Dispatchers.Main) {
                if (service!!.isPlaying()) {
                    Log.d("CO", "checkFavIndex: playing ")
                    isPlay = true
                    service!!.showNotification(isPlay, isFav, playbackOption, playbackSpeed)
                } else {
                    Log.d("CO", "checkFavIndex: stop")
                    isPlay = false
                    service!!.showNotification(isPlay, isFav, playbackOption, playbackSpeed)
                }
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

    @RequiresApi(Build.VERSION_CODES.M)
    private fun muteClicked() {
        //service.muteClicked();
        if (isMute == 1) {
            Log.d("L", "mute if $muteOrNot")
            audioManager!!.adjustVolume(AudioManager.ADJUST_MUTE, AudioManager.FLAG_PLAY_SOUND)
            binding.volumeMute.setImageResource(R.drawable.ic_volume_mute)
            muteOrNot = R.drawable.ic_volume_mute
            isMute = 0
        } else {
            Log.d("L", "mute else $muteOrNot")
            audioManager!!.adjustVolume(AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_PLAY_SOUND)
            binding.volumeMute.setImageResource(R.drawable.ic_volume_up)
            muteOrNot = R.drawable.ic_volume_up
            isMute = 1
        }
//        if (service!!.isPlaying()) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                service!!.showNotification(R.drawable.ic_pause, muteOrNot, playbackOption,playbackSpeed)
//                Log.d("L", "mute if $muteOrNot")
//            } else
//                service!!.showNotification(R.drawable.ic_pause, muteOrNot, playbackOption,playbackSpeed)
//        } else {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                Log.d("L", "mute else $muteOrNot")
//                service!!.showNotification(R.drawable.ic_play, muteOrNot, playbackOption,playbackSpeed)
//            } else
//                service!!.showNotification(R.drawable.ic_pause, muteOrNot, playbackOption,playbackSpeed)
//        }
    }

    override fun shuffleOrRepeatClicked() {
        playbackOption++
        playbackOption %= 3
        when (playbackOption) {
            0 -> {
                Log.d("P", "NORMAL : $playbackOption")
                binding.optionPlayback.contentDescription = "Sequential"
                binding.optionPlayback.setImageResource(R.drawable.ic_repeat)
                isRepeat = false
                isShuffle = false
            }
            1 -> {
                Log.d("P", "REPEAT : $playbackOption")
                if (!service!!.isBackground())
                    Toast.makeText(requireContext(), "Song repeated", Toast.LENGTH_SHORT).show()
                binding.optionPlayback.contentDescription = "Repeat music"
                binding.optionPlayback.setImageResource(R.drawable.ic_repeat_one)
                isRepeat = true
            }
            2 -> {
                Log.d("P", "SHUFFLE : $playbackOption")
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
        val binder = iBinder as SangeetMusicService.MyBinder
        service = binder.service
        service!!.setCallBack(this)

//        service!!.audioManager!!.requestAudioFocus(service!!,AudioManager.STREAM_MUSIC,
//            AudioManager.AUDIOFOCUS_GAIN)
        binding.artistName.text = playList!![position].artist
        binding.songName.text = playList!![position].name
        val duration: Int = service!!.duration / 1000
        binding.seekBar.max = duration
        getMetaData(uri.toString()) // will put in get intent Method

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

        val editor: SharedPreferences.Editor =
            requireActivity().getSharedPreferences(MUSIC_LAST_PLAYED, Context.MODE_PRIVATE).edit()
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

        Log.d("H", "onDestroyView: ${historySongList.size}")
        //store recently played songs list into shared preference
        val historyEditor =
            requireActivity().getSharedPreferences("HISTORY", Context.MODE_PRIVATE).edit()
        val json2 = GsonBuilder().create().toJson(historySongList)
        historyEditor.putString("recentSongs", json2)
        historyEditor.apply()
        //onBackPressed()
    }

}