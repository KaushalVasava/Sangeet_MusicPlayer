package com.lahsuak.sangeetmusicplayer.ui.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.RecoverableSecurityException
import android.content.*
import android.content.Context.MODE_PRIVATE
import android.content.pm.PackageManager
import android.media.*
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.view.*
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.gson.GsonBuilder
import com.lahsuak.sangeetmusicplayer.*
import com.lahsuak.sangeetmusicplayer.databinding.FragmentHomeBinding
import com.lahsuak.sangeetmusicplayer.model.*
import com.lahsuak.sangeetmusicplayer.ui.activity.HomeActivity
import com.lahsuak.sangeetmusicplayer.ui.activity.HomeActivity.Companion.favoriteList
import com.lahsuak.sangeetmusicplayer.ui.activity.HomeActivity.Companion.fromHome
import com.lahsuak.sangeetmusicplayer.ui.activity.HomeActivity.Companion.fromPlayer
import com.lahsuak.sangeetmusicplayer.ui.activity.HomeActivity.Companion.historySongList
import com.lahsuak.sangeetmusicplayer.ui.activity.HomeActivity.Companion.isFavorite
import com.lahsuak.sangeetmusicplayer.ui.activity.HomeActivity.Companion.position
import com.lahsuak.sangeetmusicplayer.ui.adapters.SongAdapter
import com.lahsuak.sangeetmusicplayer.ui.adapters.SongListener
import com.lahsuak.sangeetmusicplayer.ui.fragments.HistoryFragment.Companion.historyId
import com.lahsuak.sangeetmusicplayer.ui.fragments.PlayListFragment.Companion.musicPlaylist
import com.lahsuak.sangeetmusicplayer.util.ActionPlaying
import com.lahsuak.sangeetmusicplayer.util.AppConstants
import com.lahsuak.sangeetmusicplayer.util.AppConstants.DELETE_COUNT
import com.lahsuak.sangeetmusicplayer.util.AppConstants.FAVORITE
import com.lahsuak.sangeetmusicplayer.util.AppConstants.IN_BACKGROUND
import com.lahsuak.sangeetmusicplayer.util.AppConstants.LAST_SONG_ID
import com.lahsuak.sangeetmusicplayer.util.AppConstants.LAST_SONG_POSITION
import com.lahsuak.sangeetmusicplayer.util.AppConstants.MUSIC_LAST_PLAYED
import com.lahsuak.sangeetmusicplayer.util.AppConstants.MY_SORT_PREF
import com.lahsuak.sangeetmusicplayer.util.AppConstants.NEW_OR_RECENT
import com.lahsuak.sangeetmusicplayer.util.AppConstants.PLAYLIST
import com.lahsuak.sangeetmusicplayer.util.AppConstants.PLAYLIST_LAST_PLAYED
import com.lahsuak.sangeetmusicplayer.util.AppConstants.PLAYLIST_LAST_SONG_ID
import com.lahsuak.sangeetmusicplayer.util.AppConstants.PLAYLIST_LAST_SONG_POSITION
import com.lahsuak.sangeetmusicplayer.util.AppConstants.PLAY_STATUS
import com.lahsuak.sangeetmusicplayer.util.SongUtil.checkHistorySong
import com.lahsuak.sangeetmusicplayer.util.SongUtil.findPosition
import com.lahsuak.sangeetmusicplayer.util.SongUtil.getFavoriteIndex
import com.lahsuak.sangeetmusicplayer.util.SongUtil.getFormattedTime
import com.lahsuak.sangeetmusicplayer.util.SongUtil.getRandom
import com.lahsuak.sangeetmusicplayer.util.SongUtil.getSongs
import com.lahsuak.sangeetmusicplayer.util.SongUtil.shareMusicMultiple
import com.lahsuak.sangeetmusicplayer.util.service.MusicService
import com.lahsuak.sangeetmusicplayer.util.service.MusicService.Companion.fromFavorite
import com.lahsuak.sangeetmusicplayer.util.service.MusicService.Companion.fromHistory
import com.lahsuak.sangeetmusicplayer.util.service.MusicService.Companion.fromPlaylist
import com.lahsuak.sangeetmusicplayer.util.service.MusicService.Companion.isFav
import com.lahsuak.sangeetmusicplayer.util.service.MusicService.Companion.isInterruption1
import com.lahsuak.sangeetmusicplayer.util.service.MusicService.Companion.isInterruption2
import com.lahsuak.sangeetmusicplayer.util.service.MusicService.Companion.isInterruption3
import com.lahsuak.sangeetmusicplayer.util.service.MusicService.Companion.isPlay
import com.lahsuak.sangeetmusicplayer.util.service.MusicService.Companion.isShuffle
import com.lahsuak.sangeetmusicplayer.util.service.MusicService.Companion.playbackSpeed
import com.lahsuak.sangeetmusicplayer.util.service.MusicService.Companion.songFiles
import com.lahsuak.sangeetmusicplayer.util.service.MusicService.Companion.songList
import com.lahsuak.sangeetmusicplayer.util.service.MusicService.Companion.sortOrder
import kotlinx.coroutines.*
import java.util.*

class HomeFragment : Fragment(R.layout.fragment_home), SongListener, SearchView.OnQueryTextListener,
    ActionPlaying, ServiceConnection, AudioManager.OnAudioFocusChangeListener {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var navController: NavController
    private var isAllow = false
    private var actionMode: ActionMode? = null

    // private var service: MusicService? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    // private static MediaPlayer mediaPlayer;
    private var playThread: Thread? = null
    private var prevThread: Thread? = null
    private var nextThread: Thread? = null
    var service: MusicService? = null

    //old
    private var songAdapter: SongAdapter? = null

    private lateinit var intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest>
    private var deleteUri: Uri? = null
    private var deletePos: Int = -1
    private val tempList = ArrayList<Int>()
    private var deleteCount = 0

    private lateinit var preferencesMusic: SharedPreferences
    private lateinit var sharePrefLayout: SharedPreferences
    private lateinit var preferencePlaylist: SharedPreferences
    private lateinit var preferencesSort: SharedPreferences
    private val permissionResultLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle Permission granted/rejected
        permissions.entries.forEach {
            val isGranted = it.value
            isAllow = isGranted
        }
        if (isAllow) {
            checkAppPermission()
        } else {
            Snackbar.make(
                requireView(),
                getString(R.string.please_grant_permission),
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(getString(R.string.grant)) {
                    checkPermission()
                }.show()
        }
    }
    companion object {
        var fromSingleDelete = false
        var isCount = false
        var isGridView = false
        private var favIndex = -1
        var playlistPos = -1  // Use for current playlist position
        var list: ArrayList<Songs> = ArrayList() // for home fragment
        // var isPlayNow = false // current song playing or not,so we have to show animation according to this flag
        // var playPosition = -1 // songId of current song for play-pause animation

        //NEW UPDATE
        private lateinit var uri: Uri //song path
        private var songId = -1

        private var currentPos = 0 //current seek position

        // private var muteOrNot = 0
        private var playbackOption = 0 //Use for sequence, repeat and shuffle playback

        //action mode
        var selectedItem: Array<Boolean>? = null
        var counter = 0 //total selected song when action mode enable
        var is_in_action_mode = false
        var is_select_all = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentHomeBinding.bind(view)
        preferencesMusic =
            requireActivity().getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE)
        sharePrefLayout = requireContext().getSharedPreferences("LAYOUT", MODE_PRIVATE)

        //SharedPreferences for isPlayNowlist data
        preferencePlaylist =
            requireActivity().getSharedPreferences(PLAYLIST, AppCompatActivity.MODE_PRIVATE)
        preferencesSort =
            requireActivity().getSharedPreferences(MY_SORT_PREF, AppCompatActivity.MODE_PRIVATE)
        checkPermission()
        setHasOptionsMenu(true)
        navController = findNavController()
        //only one time called for multiple deletion
        setDeleteIntent()
        audioManager =
            requireActivity().applicationContext.getSystemService(AppCompatActivity.AUDIO_SERVICE) as AudioManager

        //check layout
        val layout = sharePrefLayout.getBoolean("layout", false)
        binding.recyclerView.layoutManager = if (layout) {
            //if layout is grid view then set icon of list view
            binding.btnView.setImageResource(R.drawable.ic_list_view)
            GridLayoutManager(requireContext(), 2)
        } else {
            //if layout is list view then set icon of grid view
            binding.btnView.setImageResource(R.drawable.ic_grid_view)
            LinearLayoutManager(requireContext())
        }
        //view layout
        binding.btnView.setOnClickListener {
            setLayout()
        }
        preferencesMusic.edit().apply {
            putBoolean(IN_BACKGROUND, false)
            apply()
        }

        //moved from below code

        position = preferencesMusic.getInt(LAST_SONG_POSITION, -1)
        songId = preferencesMusic.getInt(LAST_SONG_ID, -1)

        //sort items
        binding.btnSort.setOnClickListener {
            setSort(it)
        }

        //When no recent song then hide the recent player view
        if (position == -1) {
            if (!fromFavorite && !fromPlaylist && !fromHistory) {
                binding.recentLayout.visibility = View.GONE
                isCount = false
            }
        } else {
            isCount = true
        }
        setClickListeners()

        //removed permission code and placed above
        val playStatus = preferencesMusic.getBoolean(PLAY_STATUS, false)
        if (playStatus) {
            binding.play.setImageResource(R.drawable.ic_pause)
        }
        setAudioFocusChangeListener()

        //checking for null player
        if (position != -1 && songList?.isNotEmpty() == true) {
            if (!fromFavorite) {
                navController.currentBackStackEntry?.savedStateHandle?.getLiveData<Int>("key")
                    ?.observe(viewLifecycleOwner) {
                        if (!isInterruption1 && !isInterruption2 &&
                            !isInterruption3 && !fromPlaylist && !fromHistory && !fromFavorite && fromPlayer
                        ) {
                            position = it
                            songId = songList!![position].id
                        }
                    }
            }
            if (fromPlaylist) { // get position of song from playlistDetailsFragment
                position = preferencePlaylist.getInt(PLAYLIST_LAST_SONG_POSITION, -1)
                playlistPos = preferencePlaylist.getInt(PLAYLIST_LAST_PLAYED, -1)
            } else if (fromHistory) {
                val prefHistory = requireActivity().getSharedPreferences("HISTORY", MODE_PRIVATE)
                position = prefHistory.getInt("historyPos", -1)
            } else if (fromFavorite) {
                val prefFavorite = requireActivity().getSharedPreferences(FAVORITE, MODE_PRIVATE)
                position = prefFavorite.getInt("favoritePos", -1)
            }
            val inBackground = preferencesMusic.getBoolean(IN_BACKGROUND, false)
            if (inBackground) {
                position = preferencesMusic.getInt(LAST_SONG_POSITION, -1)
                currentPos =
                    preferencesMusic.getInt(AppConstants.LAST_CURRENT_POSITION, playbackSpeed)
            }
            initForPlaying()
        }
    }

    private fun checkPermission() {
        val array = if (Build.VERSION_CODES.TIRAMISU <= Build.VERSION.SDK_INT) {
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
        val permission = ContextCompat.checkSelfPermission(
            requireContext(),
            array.toString()
        )
        if (permission != PackageManager.PERMISSION_GRANTED) {
            permissionResultLauncher.launch(array)
        } else {
            isAllow = true
        }
    }
    private fun setClickListeners() {
        //recent layout of current playing song
        binding.recentLayout.setOnClickListener {
            //recent song = true & new song = false
            preferencesMusic.edit().apply {
                putBoolean(NEW_OR_RECENT, true)
                apply()
            }
            isInterruption1 = false
            isInterruption2 = false
            isInterruption3 = false
            position = service!!.position
            songId = songList!![position].id
            val action = HomeFragmentDirections.actionHomeFragmentToPlayerFragment(
                position, songId,
                playlistPos
            )
            navController.navigate(action)
        }
        //go to favorite
        binding.btnFavorite.setOnClickListener {
            isInterruption1 = false
            isInterruption2 = false
            isInterruption3 = false
            val action = HomeFragmentDirections.actionHomeFragmentToFavoriteFragment()
            navController.navigate(action)
        }
        //go to playlist
        binding.btnPlaylist.setOnClickListener {
            isInterruption1 = false
            isInterruption2 = false
            isInterruption3 = false
            val action = HomeFragmentDirections.actionHomeFragmentToPlayListFragment()
            navController.navigate(action)
        }
        //Song History or recently palyed song
        //got to recently played songs page
        binding.btnRecentlyPlayed.setOnClickListener {
            isInterruption1 = false
            isInterruption2 = false
            isInterruption3 = false

            val action = HomeFragmentDirections.actionHomeFragmentToHistoryFragment()
            navController.navigate(action)
        }
    }

    private fun setDeleteIntent() {
        intentSenderLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
                if (it.resultCode == RESULT_OK) {
                    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                        this.viewLifecycleOwner.lifecycleScope.launch {
                            if (fromSingleDelete) {
                                deleteFile(deleteUri!!)
                                songList!!.removeAt(deletePos)
                                songAdapter!!.notifyItemRemoved(deletePos)
                            } else {
                                for (song_id in tempList) {
                                    val contentUri: Uri = ContentUris.withAppendedId(
                                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                        song_id.toLong()
                                    )
                                    deleteFile(contentUri)
                                }
                            }
                        }
                        if (!fromSingleDelete)
                            refresh(sortOrder)
                        if (fromSingleDelete)
                            songAdapter!!.updateList(songList)
                        binding.txtTotal.text = String.format(
                            getString(R.string.total_songs),
                            list.size
                        )
                    }
                    Toast.makeText(requireContext(), "deleted successfully", Toast.LENGTH_SHORT)
                        .show()
                } else
                    Toast.makeText(
                        requireContext(),
                        "music couldn't be deleted",
                        Toast.LENGTH_SHORT
                    ).show()
            }
    }

    private fun checkAppPermission() {
        val sortOrder = preferencesSort.getString("sorting", "SortByName")
        list = getSongs(requireContext(), sortOrder!!)
        binding.txtTotal.text = String.format(getString(R.string.total_songs), list.size)
        songAdapter = SongAdapter(list, this@HomeFragment)
        binding.recyclerView.adapter = songAdapter
        val sort = preferencesSort.getString("sorting", "SortByName")
        binding.txtSort.text = sort
        songList?.clear()
        songList?.addAll(list)
        songFiles?.clear()
        songFiles?.addAll(list)
    }

    //update seekbar and start time of seekbar
    private fun updateSeekbar() {
        lifecycleScope.launch {
            while (true) {
                delay(1000)
                if (service != null) {
                    currentPos = service!!.currentPosition / 1000
                    binding.recentSeekbar.progress = currentPos
                    binding.startTime.text = getFormattedTime(currentPos)
                }
            }
        }
    }

    //check audio focus changes like incoming call and other media playing
    override fun onAudioFocusChange(focusChange: Int) {
        if (focusChange <= 0) {
            //pause music
            if (service != null) {
                //when go to player fragment or any other fragment then we don't want to pause song
                // so check one more condition fromHome = false for pause and fromHome = true for skip this condition
                if (service!!.mediaPlayer != null && !fromHome) {
                    service!!.mediaPlayer!!.pause()
                    service!!.showNotification(
                        isPlay,
                        isFav,
                        playbackOption, playbackSpeed
                    )
                    binding.play.setImageResource(R.drawable.ic_play)
                }
            }
        } else {
            //play music
            if (service != null) {
                if (service!!.mediaPlayer != null) {
                    service!!.mediaPlayer!!.start()
                    service!!.showNotification(
                        isPlay,
                        isFav,
                        playbackOption, playbackSpeed
                    )
                    binding.play.setImageResource(R.drawable.ic_pause)
                }
            }
        }
        fromHome = false
    }

    private fun setAudioFocusChangeListener() {

        audioManager =
            (requireActivity().getSystemService(Context.AUDIO_SERVICE) as AudioManager?)!!

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAcceptsDelayedFocusGain(true)
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

    //NEW METHOD FOR PLAY SONG
   private fun initForPlaying() {
        getIntentMethod()
        binding.songName.isSelected = true

        binding.recentSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (position != -1) {
                    if (service != null && fromUser) {
                        service!!.seekTo(progress * 1000)
                        service!!.showNotification(isPlay, isFav, playbackOption, playbackSpeed)
                        currentPos = progress * 1000
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        //for playing option ie.sequential, repeat, shuffle
        when (playbackOption) {
            0 -> { //for sequential songs
                binding.btnPlayingOption.setImageResource(R.drawable.ic_repeat)
                binding.txtPlayingOption.text = getString(R.string.sequential)
                MusicService.isRepeat = false
                isShuffle = false
            }

            1 -> { //for repeat song
                binding.btnPlayingOption.setImageResource(R.drawable.ic_repeat_one)
                MusicService.isRepeat = true
                binding.txtPlayingOption.text = getString(R.string.repeat)
            }

            2 -> { //for shuffle songs
                binding.btnPlayingOption.setImageResource(R.drawable.ic_shuffle)
                MusicService.isRepeat = false
                isShuffle = true
                binding.txtPlayingOption.text = getString(R.string.shuffle_all)
            }
        }
        binding.btnPlayingOption.setOnClickListener {
            shuffleOrRepeatClicked()
        }
    }
    //get songs method moved to Songs class

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.song_menu, menu)
        val searchItem = menu.findItem(R.id.action_search)

        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(this)

    }

    //set layout in grid or linear layout
    private fun setLayout() {
        val sharePrefLayoutEditor =
            requireActivity().getSharedPreferences("LAYOUT", MODE_PRIVATE).edit()
        if (isGridView) {
            //if already grid view then set to list view
            binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
            sharePrefLayoutEditor.putBoolean("layout", false)
            isGridView = false
            binding.btnView.setImageResource(R.drawable.ic_grid_view)
            //  item.title = "Grid View"
        } else {
            //if already list view then set to grid view
            binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
            sharePrefLayoutEditor.putBoolean("layout", true)
            isGridView = true
            binding.btnView.setImageResource(R.drawable.ic_list_view)
        }
        sharePrefLayoutEditor.apply()
        binding.recyclerView.adapter =
            SongAdapter(list, this@HomeFragment)
    }

    //set different filter
    private fun setSort(view: View) {
        val popupMenu = PopupMenu(context, view)
        popupMenu.menuInflater.inflate(R.menu.sort_menu, popupMenu.menu)
        popupMenu.show()
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.by_name -> refresh("SortByName")
                R.id.by_date -> refresh("SortByDate")
                R.id.by_size -> refresh("SortBySize")
                R.id.by_artist -> refresh("SortByArtist")
                R.id.by_album -> refresh("SortByAlbum")
                R.id.by_composer -> refresh("SortByComposer")
                else -> refresh("SortByName")
            }
            true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> {
                val action = HomeFragmentDirections.actionHomeFragmentToSettings()
                navController.navigate(action)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun refresh(sort: String) {
        val preferencesSort =
            requireActivity().getSharedPreferences(MY_SORT_PREF, AppCompatActivity.MODE_PRIVATE)
        val editorSort: SharedPreferences.Editor = preferencesSort.edit()
        list = getSongs(requireContext(), sort)
        //list = #(requireContext(),"sortBySize")
        editorSort.putString("sorting", sort)
        editorSort.apply()
        songAdapter = SongAdapter(list, this@HomeFragment)
        binding.recyclerView.adapter = songAdapter!!
        binding.txtSort.text = sort
    }

    //search item in a list
    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        val input = newText!!.lowercase(Locale.getDefault())
        val myFiles = ArrayList<Songs>()
        for (item in list) {
            if (item.name!!.lowercase(Locale.getDefault()).contains(input)) {
                myFiles.add(item)
            }
        }
        songAdapter!!.updateList(myFiles)
        return true
    }

    //this method use for initialization of arraylist and
    // check condition of different group of player ex. favorite ,playlist ,history
    private fun getIntentMethod() {
        val fromWhere = preferencesMusic.getString("fromWhere", "home")
        deleteCount = preferencesMusic.getInt(DELETE_COUNT, 0)
        if (fromWhere == "history") {
            fromHistory = true
        }
        if (fromWhere == "fav") {
            fromFavorite = true
        }
        if (fromWhere == "playlist") {
            playlistPos = preferencePlaylist.getInt(PLAYLIST_LAST_PLAYED, 0)
            fromPlaylist = true
        }

        //when deleted whole playlist of current playing song then we should play that song
        if (isInterruption1) {
            position = preferencePlaylist.getInt(PLAYLIST_LAST_SONG_POSITION, -1)
            songId = preferencePlaylist.getInt(PLAYLIST_LAST_SONG_ID, -1)
            if (songList!!.size > 0) {
                songId = songList!![position].id
            }
            position = findPosition(songId, list)
            fromPlaylist = false
        }
        //when cleared history of current playing song then we should play that song
        if (isInterruption2) {
            if (songList!!.size == 0) {
                songId = historyId
                position = findPosition(historyId, list)
            }
            fromHistory = false
        }
        if (isInterruption3) {
            val prefFavorite = requireActivity().getSharedPreferences(FAVORITE, MODE_PRIVATE)
            position = prefFavorite.getInt("favoritePos", -1)
            songId = prefFavorite.getInt("favId", -1)
            if (songList!!.size > 0) {
                songId = songList!![position].id
            }
            position = findPosition(songId, list)
            fromFavorite = false
        }


        songList = if (fromFavorite) {
            favoriteList
        } else if (fromHistory) {
            historySongList
        } else if (fromPlaylist) { //when song played from playlist so we have to get position of corresponding playlist
            musicPlaylist.ref!![playlistPos].playlist
        } else {
            list
        }

        //checking position of song when something change like sorting order,delete some songs etc
        if (songId != songList!![position].id) {
            position = findPosition(songId, songList!!)
        }
        uri = Uri.parse(songList!![position].path)

        val intent = Intent(requireContext(), MusicService::class.java)
        intent.putExtra("servicePosition", position)
        intent.putExtra("serviceCurPosition", currentPos)
        intent.putExtra("songUri", uri.toString())
        requireActivity().startService(intent)
    }

    //formatted time method removed
    private fun getMetaData(uri1: String) {
        if (!service!!.isBackground()) {
            val duration1 = songList!![position].duration!!.toInt()
            binding.endTime.text = getFormattedTime(duration1 / 1000)
        } else if (service!!.isBackground()) {
            val duration1 = songList!![position].duration!!.toInt()
            binding.endTime.text = getFormattedTime(duration1 / 1000)
        }
        val art = Uri.parse(songList!![position].artUri)
        Glide.with(requireContext()).load(art)
            .error(R.drawable.image_background)
            .transition(DrawableTransitionOptions.withCrossFade()).into(binding.songImage)
    }

    //check favorite index for notification icon favortite
    private fun checkFavIndex() {
        favIndex = getFavoriteIndex(songList!![position].id)
        if (favIndex == -1) {
            isFav = false
            isFavorite = false
        } else {
            isFav = true
            isFavorite = true
        }
        lifecycleScope.launch {
            delay(2) //delay for notification changes
            if (service!!.isPlaying()) {
                isPlay = true
                service!!.showNotification(isPlay, isFav, playbackOption, playbackSpeed)
            } else {
                isPlay = false
                service!!.showNotification(isPlay, isFav, playbackOption, playbackSpeed)
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
        service?.position = position
        if (service!!.isPlaying()) {
            service!!.pause()
            binding.play.setImageResource(R.drawable.ic_play)
            isPlay = false
            playbackSpeed = 0
            service!!.showNotification(
                isPlay,
                isFav,
                playbackOption, playbackSpeed
            )
            binding.recentSeekbar.max = service!!.duration / 1000
            updateSeekbar()
            //editorMusic.putBoolean(PLAY_STATUS,false)

        } else {
            isPlay = true
            playbackSpeed = 1
            service?.showNotification(
                isPlay,
                isFav,
                playbackOption, playbackSpeed
            )
            binding.play.setImageResource(R.drawable.ic_pause)
            service?.start()
            binding.recentSeekbar.max = service!!.duration / 1000
            updateSeekbar()
            //this is for notification when song is playing
            // then we don't want to remove notification,so again call notification method
            //editorMusic.putBoolean(PLAY_STATUS,true)
            lifecycleScope.launch {
                delay(2) //delay for notification
                withContext(Dispatchers.Main) {
                    if (service != null) {
                        if (service!!.isPlaying()) {
                            isPlay = true
                            service!!.showNotification(
                                isPlay,
                                isFav,
                                playbackOption, playbackSpeed
                            )
                        } else {
                            isPlay = false
                            service!!.showNotification(isPlay, isFav, playbackOption, playbackSpeed)
                        }
                    }
                }
            }
        }
        songId = songList!![position].id
    }

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
            position = if (position - 1 < 0) songList!!.size - 1 else position - 1
            uri = Uri.parse(songList!![position].path)
            service!!.createMediaPlayer(position, uri.toString())
            getMetaData(uri.toString())
            binding.songName.text = songList!![position].name
            binding.artistName.text = songList!![position].artist
            binding.recentSeekbar.max = service!!.duration / 1000

            updateSeekbar()

            service!!.onCompleted()
            service!!.showNotification(
                isPlay,
                isFav,
                playbackOption, playbackSpeed
            )
            binding.play.setBackgroundResource(R.drawable.ic_pause)
            service!!.start()
            songAdapter!!.notifyItemChanged(position)
        } else {

            service!!.stop()
            service!!.release()
            position = if (position - 1 < 0) songList!!.size - 1 else position - 1
            uri = Uri.parse(songList!![position].path)
            service!!.createMediaPlayer(position, uri.toString())
            getMetaData(uri.toString())
            binding.songName.text = songList!![position].name
            binding.artistName.text = songList!![position].artist

            binding.recentSeekbar.max = service!!.duration / 1000

            updateSeekbar()

            service!!.onCompleted()
            service!!.showNotification(
                isPlay,
                isFav,
                playbackOption, playbackSpeed
            )
            binding.play.setBackgroundResource(R.drawable.ic_play)
            songAdapter!!.notifyItemChanged(position)
        }
        songId = songList!![position].id

        if (!fromHistory) {
            checkHistorySong(songId, songList!!, position)
        }
        checkFavIndex()

    }

    //removed random method
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
            if (isShuffle && !MusicService.isRepeat) {
                position = getRandom(songList!!.size - 1)
            } else if (!isShuffle && !MusicService.isRepeat) {
                position = (position + 1) % songList!!.size
            }
            uri = Uri.parse(songList!![position].path)
            service!!.createMediaPlayer(position, uri.toString())
            getMetaData(uri.toString())
            binding.songName.text = songList!![position].name
            binding.artistName.text = songList!![position].artist

            binding.recentSeekbar.max = service!!.duration / 1000

            updateSeekbar()

            service!!.onCompleted()
            service!!.start()
            service!!.showNotification(
                isPlay,
                isFav,
                playbackOption, playbackSpeed
            )
            binding.play.setBackgroundResource(R.drawable.ic_pause)
        } else {
            service!!.stop()
            service!!.release()
            if (isShuffle && !MusicService.isRepeat) {
                position = getRandom(songList!!.size - 1)
            } else if (!isShuffle && !MusicService.isRepeat) {
                position = (position + 1) % songList!!.size
            }
            uri = Uri.parse(songList!![position].path)
            service!!.createMediaPlayer(position, uri.toString())
            getMetaData(uri.toString())
            binding.songName.text = songList!![position].name
            binding.artistName.text = songList!![position].artist
            binding.recentSeekbar.max = service!!.duration / 1000

            updateSeekbar()

            service!!.onCompleted()
            service!!.showNotification(
                isPlay,
                isFav,
                playbackOption, playbackSpeed
            )
            binding.play.setBackgroundResource(R.drawable.ic_pause)
        }
        songId = songList!![position].id

        if (!fromHistory) {
            checkHistorySong(songId, songList!!, position)
        }
        checkFavIndex()
    }

    override fun setCurrentProgress(position: Int) {
        if (service != null) {
            binding.recentSeekbar.progress = position
        }
    }

    override fun favoriteClicked() {
        if (isFavorite) {
            favIndex = getFavoriteIndex(songList!![position].id)
            if (favIndex != -1) {
                favoriteList!!.removeAt(favIndex)
            }
            isFav = false
            isFavorite = false
        } else {
            isFavorite = true
            favoriteList!!.add(songList!![position])
            isFav = true
        }
        if (service!!.isPlaying()) {
            service!!.showNotification(
                isPlay,
                isFav,
                playbackOption, playbackSpeed
            )
        } else {
            service!!.showNotification(
                isPlay,
                isFav,
                playbackOption, playbackSpeed
            )
        }

    }

    override fun shuffleOrRepeatClicked() {
        playbackOption++
        playbackOption %= 3
        when (playbackOption) {
            0 -> {
                binding.btnPlayingOption.setImageResource(R.drawable.ic_repeat)
                binding.txtPlayingOption.text =
                    "Sequential"//resources.getString(R.string.playbackOption)
                MusicService.isRepeat = false
                isShuffle = false
            }

            1 -> {
                if (!service!!.isBackground())
                    Toast.makeText(requireContext(), "Song repeated", Toast.LENGTH_SHORT).show()
                binding.btnPlayingOption.setImageResource(R.drawable.ic_repeat_one)
                MusicService.isRepeat = true
                binding.txtPlayingOption.text =
                    "Repeat"//resources.getString(R.string.playbackOption2)
            }

            2 -> {
                if (!service!!.isBackground())
                    Toast.makeText(requireContext(), "Song shuffled", Toast.LENGTH_SHORT).show()
                binding.btnPlayingOption.setImageResource(R.drawable.ic_shuffle)
                MusicService.isRepeat = false
                isShuffle = true
                binding.txtPlayingOption.text =
                    "Shaffle all"//resources.getString(R.string.playbackOption3)
            }
        }
        if (service!!.isPlaying()) {
            service!!.showNotification(
                isPlay,
                isFav,
                playbackOption, playbackSpeed
            )
        } else {
            service!!.showNotification(
                isPlay,
                isFav,
                playbackOption, playbackSpeed
            )
        }
    }

    override fun onServiceConnected(name: ComponentName, iBinder: IBinder) {
        val binder = iBinder as MusicService.MyBinder
        service = binder.service
        service!!.setCallBack(this)
        val duration: Int
        position = preferencesMusic.getInt(LAST_SONG_POSITION, -1)

        if (position != -1) {
            if (fromHistory || isInterruption1 || isInterruption2 || isInterruption3) {
                position = service!!.position
                isInterruption1 = false
                isInterruption2 = false
                isInterruption3 = false
            }
            if (songId != songList!![position].id) {
                position = findPosition(songId, songList!!)
            }

            //NORMAL
            duration = service?.duration?:0 / 1000
            uri = Uri.parse(songList!![position].path)
            binding.songName.text = songList!![position].name
            binding.artistName.text = songList!![position].artist

            currentPos =
                service?.currentPosition?:0 / 1000
            //when killed app and restart again so we have to resuming song

            getMetaData(uri.toString()) // will put in get intent Method
            binding.recentSeekbar.progress = currentPos
            binding.startTime.text =
                getFormattedTime(currentPos)
            binding.recentSeekbar.max = duration
            service?.onCompleted()
            // check favIndex for favorite icon in notification
            //checkFavIndex()

            favIndex = getFavoriteIndex(songList!![position].id)
            if (favIndex == -1) {
                isFav = false
                isFavorite = false
            } else {
                isFav = true
                isFavorite = true
            }

            if (service!!.isPlaying()) {
                binding.play.setImageResource(R.drawable.ic_pause)
                service!!.showNotification(
                    isPlay,
                    isFav,
                    playbackOption, playbackSpeed
                )
            }
            if (position != -1) {
                updateSeekbar()
            }
        }
    }

    override fun onServiceDisconnected(name: ComponentName) {
        service = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (position != -1) {
            val editorMusic: SharedPreferences.Editor = preferencesMusic.edit()

            editorMusic.putString(AppConstants.MUSIC_FILE, uri.toString())
            editorMusic.putString(AppConstants.ARTIST_NAME, songList!![position].artist)
            editorMusic.putString(AppConstants.SONG_NAME, songList!![position].name)
            editorMusic.putInt(LAST_SONG_POSITION, service!!.position)
            editorMusic.putInt(AppConstants.LAST_CURRENT_POSITION, service!!.currentPosition)
            editorMusic.putInt(AppConstants.LAST_DURATION, service!!.duration)
            editorMusic.putBoolean(PLAY_STATUS, service!!.isPlaying())
            editorMusic.putInt(AppConstants.LAST_SONG_ID, songId)
            editorMusic.putInt(DELETE_COUNT, deleteCount)

            if (fromHistory)
                editorMusic.putString("fromWhere", "history")
            else if (fromFavorite)
                editorMusic.putString("fromWhere", "fav")
            else if (fromPlaylist)
                editorMusic.putString("fromWhere", "playlist")
            else
                editorMusic.putString("fromWhere", "home")
            editorMusic.apply()

        }
    }

    //multi selection of list
    @SuppressLint("NotifyDataSetChanged")
    private fun onActionMode(actionModeOn: Boolean) {
        if (actionModeOn) {
            songList = list
            //initially initialize all elements to false
            selectedItem = Array(songList!!.size) { false }
            is_in_action_mode = true
            //  toolbar.visibility = View.GONE
            (requireActivity() as AppCompatActivity).supportActionBar!!.hide()
            binding.recentLayout.visibility = View.GONE
            binding.moreOptions.visibility = View.GONE
            songAdapter!!.notifyDataSetChanged()
        } else {
            is_in_action_mode = false
            is_select_all = false //select all item disable
            (requireActivity() as AppCompatActivity).supportActionBar!!.show()
            //toolbar.visibility = View.VISIBLE
            if (position == -1) {
                if (!fromFavorite && !fromPlaylist) {
                    binding.recentLayout.visibility = View.GONE
                }
            } else
                binding.recentLayout.visibility = View.VISIBLE
            binding.moreOptions.visibility = View.VISIBLE
            songAdapter!!.notifyDataSetChanged()
        }
    }

    private suspend fun deleteFile(songUri: Uri) {
        withContext(Dispatchers.IO) {
            val resolver = requireContext().contentResolver
            try {
                resolver.delete(songUri, null, null)
            } catch (e: SecurityException) {
                val intentSender = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                        MediaStore.createDeleteRequest(resolver, listOf(songUri)).intentSender
                    }

                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                        val recoverableSecurityException = e as? RecoverableSecurityException
                        recoverableSecurityException?.userAction?.actionIntent?.intentSender
                    }

                    else -> null
                }

                intentSender?.let { sender ->
                    intentSenderLauncher.launch(IntentSenderRequest.Builder(sender).build())
                }
            }
        }
    }

    override fun onAnyItemLongClicked(position: Int) {
        if (!is_in_action_mode) {
            onActionMode(true)
            counter = 1
            selectedItem!![position] = true
        } else {
            if (selectedItem!![position]) {
                selectedItem!![position] = false
                counter--
            } else {
                selectedItem!![position] = true
                counter++
            }
        }
        if (actionMode == null) {
            actionMode = (activity as AppCompatActivity).startSupportActionMode(callback)!!
        }
        actionMode!!.title = "$counter/${songList!!.size} Selected"

    }

    override fun onItemClicked(position: Int, id: Int) {
        service?.position = position
        if (is_in_action_mode) {
            if (selectedItem!![position]) {
                selectedItem!![position] = false
                counter--
                actionMode!!.title = "$counter/${songList!!.size} Selected"
            } else {
                selectedItem!![position] = true
                counter++
                actionMode!!.title = "$counter/${songList!!.size} Selected"
            }
        } else {
            val editorMusic: SharedPreferences.Editor = preferencesMusic.edit()
            editorMusic.putBoolean(NEW_OR_RECENT, false)
            editorMusic.apply()

            //false for new and true for recent
            val sort = preferencesSort.getString("sorting", "SortByName")
            songId = id
            if (sort != null) {
                sortOrder = sort
            }
            songList = list
            fromFavorite = false
            fromPlaylist = false
            fromHistory = false
            isInterruption1 = false

            val action = HomeFragmentDirections.actionHomeFragmentToPlayerFragment(
                position, id,
                playlistPos
            )
            navController.navigate(action)
        }
    }

    override fun onItemDeleteClicked(position: Int) {
        songList!!.addAll(list)
        val contentUri: Uri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            songList!![position].id.toLong()
        )
        deleteUri = contentUri
        this.viewLifecycleOwner.lifecycleScope.launch {
            deletePos = position
            fromSingleDelete = true
            deleteFile(contentUri)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            songList!!.removeAt(position)
            songAdapter!!.notifyItemRemoved(position)
        }
    }

    private val callback = object : ActionMode.Callback {
        override fun onCreateActionMode(
            mode: ActionMode?,
            menu: Menu?
        ): Boolean {
            val menuInflater = MenuInflater(requireContext())
            menuInflater.inflate(R.menu.action_menu, menu)
            return true
        }

        override fun onPrepareActionMode(
            mode: ActionMode?,
            menu: Menu?
        ): Boolean {
            return false
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun onActionItemClicked(
            mode: ActionMode?,
            item: MenuItem?
        ): Boolean {
            return when (item?.itemId) {

                R.id.action_delete -> {
                    if (counter == 0) {
                        Toast.makeText(
                            requireContext(),
                            "Please select a music",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else if (selectedItem!!.isNotEmpty()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val materialAlertDialog = MaterialAlertDialogBuilder(requireContext())
                            materialAlertDialog.setTitle("Delete Music")
                                .setMessage("Delete these musics? You have to delete one by one all music file.")
                                .setCancelable(false)
                                .setIcon(R.drawable.ic_delete)
                                .setPositiveButton("Delete") { dialog, id ->

                                    deleteInActionMode()
                                    dialog.dismiss()
                                }
                                .setNegativeButton("Cancel") { dialog, id ->
                                    dialog.cancel()
                                }
                                .show()
                        } else {
                            val materialAlertDialog = MaterialAlertDialogBuilder(requireContext())
                            materialAlertDialog.setTitle("Delete Music")
                                .setMessage("Delete these musics?")
                                .setCancelable(false)
                                .setIcon(R.drawable.ic_delete)
                                .setPositiveButton("Delete") { dialog, id ->
                                    deleteInActionMode()
                                    dialog.dismiss()
                                }
                                .setNegativeButton("Cancel") { dialog, id ->
                                    dialog.cancel()
                                }
                                .show()
                        }
                    }
                    true
                }

                R.id.action_selectAll -> {
                    if (!is_select_all) {
                        item.setIcon(R.drawable.ic_select_all)
                        for (i in 0 until songList!!.size)
                            selectedItem!![i] = true

                        counter = songList!!.size
                        actionMode!!.title = "$counter/${songList!!.size} Selected"
                        is_select_all = true
                    } else {
                        item.setIcon(R.drawable.ic_select_all_off)
                        for (i in 0 until songList!!.size)
                            selectedItem!![i] = false

                        counter = 0
                        is_select_all = false
                        actionMode!!.title = "$counter/${songList!!.size} Selected"
                    }
                    songAdapter!!.notifyDataSetChanged()
                    true
                }

                R.id.action_share -> {
                    if (counter == 0) {
                        Toast.makeText(
                            requireContext(),
                            "Please select a music",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else if (selectedItem!!.isNotEmpty()) {
                        if (counter == songList!!.size) {
                            shareMusicMultiple(requireContext(), songList!!)
                        } else {
                            val shareList = ArrayList<Songs>()
                            for (i in selectedItem!!.indices) {
                                if (selectedItem!![i]) {
                                    shareList.add(songList!![i])
                                }
                            }
                            shareMusicMultiple(requireContext(), shareList)
                        }
                    }
                    true
                }

                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            onActionMode(false)
            actionMode = null
        }
    }

    private fun deleteInActionMode() {
        if (counter == songList!!.size) {
            //delete all file
            try {
                for (i in 0 until counter) {
                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        songList!![i].id.toLong()
                    )
                    deleteUri = contentUri
                    this.viewLifecycleOwner.lifecycleScope.launch {
                        tempList.add(songList!![i].id)
                        deleteFile(contentUri)
                    }
                    songAdapter!!.updateList(songList!!)
                }
                onActionMode(false)
                actionMode!!.finish()
                binding.txtTotal.text = String.format(getString(R.string.total_songs), 0)
            } catch (e: Exception) {
                e.message
            }

        } else {
            //loop running to size but we want to reduce time using counter
            for (i in selectedItem!!.indices) {
                if (selectedItem!![i]) {
                    val contentUri: Uri =
                        ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            songList!![i].id.toLong()
                        )
                    deleteUri = contentUri
                    this.viewLifecycleOwner.lifecycleScope.launch {
                        tempList.add(songList!![i].id)
                        deleteFile(contentUri)
                    }
                }
            }
            onActionMode(false)
            actionMode!!.finish()
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                for (id in tempList) {
                    val pos = findPosition(id, songList!!)
                    songList!!.removeAt(pos)
                    songAdapter!!.notifyItemRemoved(pos)
                }
                songAdapter!!.updateList(songList!!)
                binding.txtTotal.text =
                    String.format(getString(R.string.total_songs), songList!!.size)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        fromHome = true
        fromPlayer = false
        requireActivity().unbindService(this)
        //store playlist into shared preference
        val json2 = GsonBuilder().create().toJson(musicPlaylist)
        val editorPlaylist = preferencePlaylist.edit()
        editorPlaylist.putString("playlist", json2)
        editorPlaylist.apply()

        //store recently played songs list into shared preference
        val historyEditor = requireActivity().getSharedPreferences("HISTORY", MODE_PRIVATE).edit()
        val json = GsonBuilder().create().toJson(historySongList)
        historyEditor.putString("recentSongs", json)
        historyEditor.apply()
    }

    override fun onResume() {
        super.onResume()
        if (position != -1) {
            if (service != null) {
                preferencesMusic.edit().apply {
                    putInt(AppConstants.LAST_CURRENT_POSITION, currentPos)
                    apply()
                }
            }
            playThread()
            prevThread()
            nextThread()
        }
        val intent = Intent(requireContext(), MusicService::class.java)
        requireActivity().bindService(intent, this, AppCompatActivity.BIND_AUTO_CREATE)
    }

}