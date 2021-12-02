package com.lahsuak.sangeetmusicplayer.fragments


import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.lahsuak.sangeetmusicplayer.*
import com.lahsuak.sangeetmusicplayer.util.Constants.PLAYLIST
import com.lahsuak.sangeetmusicplayer.databinding.FragmentPlaylistBinding
import com.lahsuak.sangeetmusicplayer.adapters.PlaylistAdapter
import com.lahsuak.sangeetmusicplayer.adapters.PlaylistListener
import com.lahsuak.sangeetmusicplayer.model.Songs
import com.lahsuak.sangeetmusicplayer.service.SangeetMusicService.Companion.fromPlaylist
import com.lahsuak.sangeetmusicplayer.service.SangeetMusicService.Companion.isInterruption1
import com.lahsuak.sangeetmusicplayer.service.SangeetMusicService.Companion.isPlaylistSongClicked
import com.lahsuak.sangeetmusicplayer.util.Constants
import java.util.*
import kotlin.collections.ArrayList

class PlayListFragment : Fragment(R.layout.fragment_playlist) , PlaylistListener {

    private lateinit var binding:FragmentPlaylistBinding
    private var playlistAdapter: PlaylistAdapter? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnPlaylist: FloatingActionButton

    companion object {
        private var tempPos = -1
        var tempCheck = false
        var musicPlaylist: Songs.MusicPlaylist = Songs.MusicPlaylist()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlaylistBinding.inflate(inflater, container, false)

        binding.playlistRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        playlistAdapter =
            PlaylistAdapter(requireContext(), musicPlaylist.ref!!, this@PlayListFragment)
        binding.playlistRecyclerView.adapter = playlistAdapter

        val preferencePlaylist =
            requireActivity().getSharedPreferences(PLAYLIST, AppCompatActivity.MODE_PRIVATE)
         val result = preferencePlaylist.getBoolean("isClicked",false)

        Log.d("DATA", "onCreateView: backstack $result")
        if(result){
            val editor1: SharedPreferences.Editor =
                requireActivity().getSharedPreferences(PLAYLIST, Context.MODE_PRIVATE)
                    .edit()
            editor1.putInt(Constants.PLAYLIST_LAST_PLAYED, tempPos)
            editor1.apply()
        }

        binding.btnPlaylist.setOnClickListener {
            val playlistDialog = LayoutInflater.from(requireContext())
                .inflate(R.layout.add_playlist_dialog, container, false)
            val builder = MaterialAlertDialogBuilder(requireContext())
            val input = playlistDialog.findViewById<TextInputLayout>(R.id.input)
            input.requestFocus()
            val textPlaylistName =
                playlistDialog.findViewById<TextInputEditText>(R.id.editPlaylistName)

            textPlaylistName.requestFocus()
            textPlaylistName.setText("New Playlist ${musicPlaylist!!.ref!!.size+1}")
            builder.setView(playlistDialog)
                .setTitle("Create Playlist")
                .setMessage("Enter Playlist name")
                .setPositiveButton("Add") { dialog, _ ->
                    val playlistName = textPlaylistName.text
                    if (playlistName != null) {
                        if (playlistName.isNotEmpty()) {
                            addPlaylist(playlistName.toString())
                        }
                    }
                    //when playlist empty so we have to go to detail playlist
                    onItemClicked(musicPlaylist.ref!!.size-1)
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }.show()
        }
        return binding.root
    }


    private fun addPlaylist(playlistName: String) {
        var playlistExists = false
        for (item in musicPlaylist.ref!!) {
            if (playlistName == item.name) {
                playlistExists = true
                break
            }
        }
        if (playlistExists)
            Toast.makeText(
                requireContext(), "Playlist Exists!!", Toast.LENGTH_SHORT
            ).show()
        else {
            val tempPlaylist = Songs.Playlist()
            tempPlaylist.name = playlistName
            tempPlaylist.playlist = ArrayList()
            musicPlaylist.ref!!.add(tempPlaylist)
            playlistAdapter!!.refreshPlaylist()
        }
    }

    override fun onPause() {
        super.onPause()
        //isPlaylistSongClicked = false
        //This flag is true when user clicked on item of this playlist
        //So we have to send position of current playlist and clicked song of this playlist
        Log.d("DATA","Position of playlist in pause  $tempPos and $tempCheck")
        if(tempCheck) {
            val editor1: SharedPreferences.Editor =
                requireActivity().getSharedPreferences(PLAYLIST, Context.MODE_PRIVATE)
                    .edit()
            editor1.putInt(Constants.PLAYLIST_LAST_PLAYED, tempPos)
            editor1.apply()
        }
    }
    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        val preferencePlaylist =
            requireActivity().getSharedPreferences(PLAYLIST, AppCompatActivity.MODE_PRIVATE)
        val result = preferencePlaylist.getBoolean("isClicked",false)

        Log.d("DATA", "onCreateView: backstack $result and temppos $tempPos")
        if(result){
            val editor1: SharedPreferences.Editor =
                requireActivity().getSharedPreferences(PLAYLIST, Context.MODE_PRIVATE)
                    .edit()
            editor1.putInt(Constants.PLAYLIST_LAST_PLAYED, tempPos)
            editor1.apply()
        }
        playlistAdapter!!.notifyDataSetChanged()
    }

    override fun onItemClicked(position: Int) {
        //playlist position
        tempPos = position
        isPlaylistSongClicked = false
        Log.d("DATA","Position of playlist $tempPos and #$isPlaylistSongClicked")
        val navController = findNavController()
        val action = PlayListFragmentDirections.actionPlayListFragmentToPlaylistDetails(
                position
            )
        navController.navigate(action)
    }

    override fun onAnyItemLongClicked(position: Int) {
          //nothing to do
    }

    override fun onItemDelete(position: Int) {
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle(musicPlaylist.ref!![position].name)
            .setMessage("Do you want to delete playlist?")
            .setPositiveButton("Yes") { dialog, _ ->
                if(fromPlaylist) {
                    Log.d("DATA", "onItemDelete: delete item ")
                    fromPlaylist = false
                    isInterruption1 = true
                }
                musicPlaylist.ref!!.removeAt(position)
                playlistAdapter!!.refreshPlaylist()
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
        val deleteDialog = builder.create()
        deleteDialog.show()
    }
}