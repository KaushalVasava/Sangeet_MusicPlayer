package com.lahsuak.sangeetmusicplayer.fragments

import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.GsonBuilder
import com.lahsuak.sangeetmusicplayer.*
import com.lahsuak.sangeetmusicplayer.util.Constants.FAVORITE
import com.lahsuak.sangeetmusicplayer.databinding.FragmentFavoriteBinding
import com.lahsuak.sangeetmusicplayer.activity.HomeActivity.Companion.favoriteList
import com.lahsuak.sangeetmusicplayer.adapters.SongAdapter
import com.lahsuak.sangeetmusicplayer.adapters.SongListener
import com.lahsuak.sangeetmusicplayer.model.findPosition
import com.lahsuak.sangeetmusicplayer.service.SangeetMusicService.Companion.fromFavorite
import com.lahsuak.sangeetmusicplayer.service.SangeetMusicService.Companion.fromHistory
import com.lahsuak.sangeetmusicplayer.service.SangeetMusicService.Companion.fromPlaylist
import com.lahsuak.sangeetmusicplayer.service.SangeetMusicService.Companion.isInterruption3
import com.lahsuak.sangeetmusicplayer.service.SangeetMusicService.Companion.playlistPosition
import com.lahsuak.sangeetmusicplayer.service.SangeetMusicService.Companion.songList
import com.lahsuak.sangeetmusicplayer.util.Constants

class FavoriteFragment : Fragment(R.layout.fragment_favorite), SongListener {

    private lateinit var binding: FragmentFavoriteBinding
    private lateinit var songAdapter: SongAdapter

    companion object {
        private var tempPosition = -1
        private var isFavoriteSongClicked = false
        var favoriteSongId = -1
        private var tempSize = 0
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFavoriteBinding.inflate(inflater, container, false)

        setHasOptionsMenu(true)

        val sharePrefLayout = requireContext().getSharedPreferences("LAYOUT", MODE_PRIVATE)
        val layout = sharePrefLayout.getBoolean("layout", false)

        if (layout) {
            binding.favRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        } else {
            binding.favRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        }

//        val editor1 = requireActivity().getSharedPreferences(FAVORITE, MODE_PRIVATE)
//        val jsonString = editor1.getString("favorite", null)
//        val typeToken = object : TypeToken<ArrayList<Songs>>() {}.type
//        if (jsonString != null) {
//            favoriteList!!.clear()
//            val data: ArrayList<Songs> = GsonBuilder().create().fromJson(jsonString, typeToken)
//            favoriteList!!.addAll(data)
//        }
        songAdapter = SongAdapter(
            requireContext(),
            favoriteList!!,
            this@FavoriteFragment,
            isFavoriteList = true
        )
        binding.favRecyclerView.adapter = songAdapter


        return binding.root
    }

    override fun onPause() {
        super.onPause()
        Log.d("H", "onPause: 1 History fragment $tempPosition")
        if (isFavoriteSongClicked && favoriteList!!.size > 0 && tempSize != favoriteList!!.size) {
            if (favoriteList!!.size <= 1) {
                tempPosition = 0
            } else {
                tempPosition = findPosition(favoriteSongId, favoriteList!!)
            }
            songList = favoriteList
            //store recently played songs list into shared preference
        }
        val favoriteEditor =
            requireActivity().getSharedPreferences(FAVORITE, MODE_PRIVATE).edit()
        val json2 = GsonBuilder().create().toJson(favoriteList)
        favoriteEditor.putString("favorite", json2)
        favoriteEditor.putInt("favoritePos", tempPosition)
        favoriteEditor.putInt("favId", favoriteSongId)
        favoriteEditor.apply()

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.delete_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_delete) {
            val builder = MaterialAlertDialogBuilder(requireContext())
            builder.setTitle("Remove songs from favorites")
                .setMessage("Do you want to remove all favorites songs?")
                .setPositiveButton("Remove") { dialog, _ ->
                    if (fromFavorite) {
                        Log.d("DATA", "onItemDelete: delete item ")
                        fromFavorite = false
                        isInterruption3 = true //when playing song from history and
                        // suddenly whole history cleared then we should playing song
                    }
                    favoriteList!!.clear()
                    songAdapter.updateList(favoriteList!!)
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
            val deleteDialog = builder.create()
            deleteDialog.show()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onItemClicked(position: Int, id: Int) {
        val editor: SharedPreferences.Editor =
            requireActivity().getSharedPreferences(Constants.MUSIC_LAST_PLAYED, MODE_PRIVATE).edit()
        editor.putBoolean(Constants.NEW_OR_RECENT, false)
        // false for new and true for recent
        editor.apply()
        val action =
            FavoriteFragmentDirections.actionFavoriteFragmentToPlayerFragment(
                position, id,
                playlistPosition
            )
        songList = favoriteList
        //new
        isFavoriteSongClicked = true
        tempPosition = position
        favoriteSongId = id
        tempSize = songList!!.size

        findNavController().navigate(action)
        fromFavorite = true
        fromPlaylist = false
        fromHistory = false
    }

    override fun onAnyItemLongClicked(position: Int) {
        //nothing to do
    }

    override fun onItemDeleteClicked(position: Int) {
    }
}