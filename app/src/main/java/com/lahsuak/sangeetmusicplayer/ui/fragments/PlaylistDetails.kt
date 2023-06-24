package com.lahsuak.sangeetmusicplayer.ui.fragments

import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.GsonBuilder
import com.lahsuak.sangeetmusicplayer.R
import com.lahsuak.sangeetmusicplayer.databinding.FragmentPlaylistDetailsBinding
import com.lahsuak.sangeetmusicplayer.ui.adapters.SongAdapter
import com.lahsuak.sangeetmusicplayer.ui.adapters.SongListener
import com.lahsuak.sangeetmusicplayer.ui.fragments.PlayListFragment.Companion.musicPlaylist
import com.lahsuak.sangeetmusicplayer.util.AppConstants
import com.lahsuak.sangeetmusicplayer.util.AppConstants.MUSIC_LAST_PLAYED
import com.lahsuak.sangeetmusicplayer.util.AppConstants.PLAYLIST
import com.lahsuak.sangeetmusicplayer.util.AppConstants.PLAYLIST_LAST_SONG_POSITION
import com.lahsuak.sangeetmusicplayer.util.SongUtil.findPosition
import com.lahsuak.sangeetmusicplayer.util.service.MusicService.Companion.fromFavorite
import com.lahsuak.sangeetmusicplayer.util.service.MusicService.Companion.fromHistory
import com.lahsuak.sangeetmusicplayer.util.service.MusicService.Companion.fromPlaylist
import com.lahsuak.sangeetmusicplayer.util.service.MusicService.Companion.isInterruption1
import com.lahsuak.sangeetmusicplayer.util.service.MusicService.Companion.isPlaylistSongClicked
import com.lahsuak.sangeetmusicplayer.util.service.MusicService.Companion.songList

class PlaylistDetails : Fragment(R.layout.fragment_playlist_details), SongListener {
    //PROBLEM WHEN ONE SONG PLAYING IN PLAYLIST AND AND CLICK ON ANOTHER SONG IT WILL SHOW WRONG NOTIFICATION
    private lateinit var binding: FragmentPlaylistDetailsBinding
    private val args: PlaylistDetailsArgs by navArgs()
    private lateinit var adapter: SongAdapter
    private lateinit var navController: NavController
    private var playlistSongPos = -1

    companion object {
        private var tempSize = 0
        var playlistSongId = -1
        var currentPlaylistPos: Int = -1
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlaylistDetailsBinding.inflate(inflater, container, false)

        currentPlaylistPos = args.playlistPosition
        val sharePrefLayout = requireContext().getSharedPreferences("LAYOUT", MODE_PRIVATE)
        val layout = sharePrefLayout.getBoolean("layout", false)

        if (layout) {
            binding.detailRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        } else {
            binding.detailRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        }

        adapter = SongAdapter(
            musicPlaylist.ref!![currentPlaylistPos].playlist,
            this@PlaylistDetails,
            isPlaylistDetails = true
        )
        binding.detailRecyclerView.adapter = adapter
        if (isPlaylistSongClicked && musicPlaylist.ref!![currentPlaylistPos].playlist.size > 0)
            songList = musicPlaylist.ref!![currentPlaylistPos].playlist
        //add songs to playlist
        binding.addToPlaylist.setOnClickListener {
            val action =
                PlaylistDetailsDirections.actionPlaylistDetailsToSelectionPlaylist()
            navController = findNavController()
            navController.navigate(action)
        }
        //removeSongs from playlist
        binding.removeFromPlaylist.setOnClickListener {
            val builder = MaterialAlertDialogBuilder(requireContext())
            builder.setTitle("Remove Songs from Playlist")
                .setMessage("Do you want to remove all songs?")
                .setPositiveButton("Remove") { dialog, _ ->
                    if (fromPlaylist) {
                        fromPlaylist = false
                        isInterruption1 = true

                    }
                    musicPlaylist.ref!![currentPlaylistPos].playlist.clear()
                    adapter.updateList(musicPlaylist.ref!![currentPlaylistPos].playlist)
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
            val deleteDialog = builder.create()
            deleteDialog.show()
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()

        if (isPlaylistSongClicked && musicPlaylist.ref!![currentPlaylistPos].playlist.size > 0)
            songList = musicPlaylist.ref!![currentPlaylistPos].playlist

        (requireActivity() as AppCompatActivity).supportActionBar!!.title =
            musicPlaylist.ref!![currentPlaylistPos].name

        if (adapter.itemCount > 0) {
            val artUri = Uri.parse(musicPlaylist.ref!![currentPlaylistPos].playlist[0].artUri)
            Glide.with(requireContext()).load(artUri)
                .error(R.drawable.image_background)
                .transition(DrawableTransitionOptions.withCrossFade()).into(binding.titleImage)
        }

        //store playlist into shared preference
        val editor2 = requireActivity().getSharedPreferences(PLAYLIST, MODE_PRIVATE).edit()
        val json2 = GsonBuilder().create().toJson(musicPlaylist)
        editor2.putString("playlist", json2)
        editor2.apply()

    }

    override fun onPause() {
        super.onPause()
        if (isPlaylistSongClicked && musicPlaylist.ref!![currentPlaylistPos].playlist.size > 0
            && musicPlaylist.ref!![currentPlaylistPos].playlist.size != tempSize
        ) {
            //when deleted some song from playlist then we should find current playing song position
            // and update song list
            playlistSongPos = if (musicPlaylist.ref!![currentPlaylistPos].playlist.size <= 1) {
                0
            } else {
                findPosition(playlistSongId, musicPlaylist.ref!![currentPlaylistPos].playlist)
            }
            songList = musicPlaylist.ref!![currentPlaylistPos].playlist
            val editor1: SharedPreferences.Editor =
                requireActivity().getSharedPreferences(PLAYLIST, MODE_PRIVATE).edit()
            editor1.putInt(PLAYLIST_LAST_SONG_POSITION, playlistSongPos)
            editor1.putInt(AppConstants.PLAYLIST_LAST_SONG_ID, playlistSongId)

            editor1.apply()
        }
    }

    override fun onStop() {
        super.onStop()
        val editor: SharedPreferences.Editor =
            requireActivity().getSharedPreferences(PLAYLIST, MODE_PRIVATE)
                .edit()
        if (isPlaylistSongClicked) {
            editor.putBoolean("isClicked", true)
        } else {
            isPlaylistSongClicked = false
            editor.putBoolean("isClicked", false)
        }
        editor.apply()
    }

    override fun onItemClicked(position: Int, id: Int) {
        playlistSongPos = position
        //This flag is true when user clicked on item of this playlist
        //So we have to send position of this song and playlistPosition from playlistFragment
        isPlaylistSongClicked = true
        if (isPlaylistSongClicked && musicPlaylist.ref!![currentPlaylistPos].playlist.size > 0) {
            songList = musicPlaylist.ref!![currentPlaylistPos].playlist
            playlistSongId = id
            tempSize = songList!!.size
        }
        val editor: SharedPreferences.Editor =
            requireActivity().getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE).edit()
        // false for new and true for recent
        editor.putBoolean(AppConstants.NEW_OR_RECENT, false)
        editor.apply()

        val editor1: SharedPreferences.Editor =
            requireActivity().getSharedPreferences(PLAYLIST, MODE_PRIVATE).edit()
        editor1.putInt(PLAYLIST_LAST_SONG_POSITION, position)
        editor1.putInt(AppConstants.PLAYLIST_LAST_SONG_ID, id)
        editor1.apply()

        val action = PlaylistDetailsDirections.actionPlaylistDetailsToPlayerFragment(
            position,
            id,
            currentPlaylistPos
        )
        //Third argument in the player fragment is optional for other fragments but necessary for this fragment
        navController = findNavController()
        navController.navigate(action)
        fromPlaylist = true
        fromHistory = false
        fromFavorite = false
    }

    override fun onAnyItemLongClicked(position: Int) {
        /* no-op */
    }

    override fun onItemDeleteClicked(position: Int) {
        /* no-op */
    }
}