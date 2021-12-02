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
import com.lahsuak.sangeetmusicplayer.databinding.FragmentHistoryBinding
import com.lahsuak.sangeetmusicplayer.activity.HomeActivity.Companion.historySongList
import com.lahsuak.sangeetmusicplayer.adapters.SongAdapter
import com.lahsuak.sangeetmusicplayer.adapters.SongListener
import com.lahsuak.sangeetmusicplayer.service.SangeetMusicService.Companion.fromFavorite
import com.lahsuak.sangeetmusicplayer.service.SangeetMusicService.Companion.fromHistory
import com.lahsuak.sangeetmusicplayer.service.SangeetMusicService.Companion.fromPlaylist
import com.lahsuak.sangeetmusicplayer.service.SangeetMusicService.Companion.isInterruption2
import com.lahsuak.sangeetmusicplayer.service.SangeetMusicService.Companion.songList
import com.lahsuak.sangeetmusicplayer.util.Constants

class HistoryFragment : Fragment(R.layout.fragment_history), SongListener {

    private lateinit var binding: FragmentHistoryBinding

    private lateinit var songAdapter: SongAdapter

    companion object{
        private var tempPosition = -1
        private var isHistorySongClicked = false
        private var tempSize = 0
        var historyId = 0
    }
//        var favoriteList: ArrayList<Songs>? = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHistoryBinding.inflate(inflater, container, false)

        setHasOptionsMenu(true)

        val sharePrefLayout = requireContext().getSharedPreferences("LAYOUT", MODE_PRIVATE)
        val layout = sharePrefLayout.getBoolean("layout", false)

        if (layout) {
            binding.historyRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        } else {
            binding.historyRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        }

//       //fetch saved recently played songs list
//        val preferenceHistory = requireActivity().getSharedPreferences("HISTORY", MODE_PRIVATE)
//        val jsonStringHistory = preferenceHistory.getString("recentSongs", null)
//        val typeToken = object : TypeToken<ArrayList<Songs>>() {}.type
//        if (jsonStringHistory != null) {
//            historySongList.clear()
//            val data: ArrayList<Songs> = GsonBuilder().create().fromJson(jsonStringHistory, typeToken)
//            Log.d("H", "onCreateView: ${data.size}")
//            historySongList.addAll(data)
//        }
        songAdapter = SongAdapter(requireContext(), historySongList, this@HistoryFragment,isHistoryList = true)
        binding.historyRecyclerView.adapter = songAdapter

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.delete_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId==R.id.action_delete){
            val builder = MaterialAlertDialogBuilder(requireContext())
            builder.setTitle("Clear History")
                .setMessage("Do you want to clear history?")
                .setPositiveButton("Clear") { dialog, _ ->
                    if(fromHistory) {
                        Log.d("DATA", "onItemDelete: delete item ")
                        fromHistory = false
                        isInterruption2 = true //when playing song from history and
                                               // suddenly whole history cleared then we should playing song
                    }
                    historySongList.clear()
                    songAdapter.updateList(historySongList)
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


    override fun onPause() {
        super.onPause()
        Log.d("H", "onPause: 1 History fragment $tempPosition")
//        if(isHistorySongClicked && historySongList.size>0 && tempSize!= historySongList.size){
//            if(historySongList.size<=1){
//                tempPosition =0
//            }
//            else{
//                tempPosition = findPosition(historyId, historySongList)
//            }
//            songList = historySongList
//
//        }

        //store recently played songs list into shared preference
        val historyEditor = requireActivity().getSharedPreferences("HISTORY", MODE_PRIVATE).edit()
        val json2 = GsonBuilder().create().toJson(historySongList)
        historyEditor.putString("recentSongs", json2)
        historyEditor.putInt("historyPos", tempPosition)
        historyEditor.apply()
    }

    override fun onItemClicked(position: Int, id: Int) {
        Log.d("H", "onItem clicked fragment ${historySongList.size} and $position")
        isHistorySongClicked = true
        val editor: SharedPreferences.Editor =
            requireActivity().getSharedPreferences(Constants.MUSIC_LAST_PLAYED, MODE_PRIVATE).edit()
        editor.putBoolean(Constants.NEW_OR_RECENT, false)
       // false for new and true for recent
        editor.apply()

        songList = historySongList
        historyId = id
        tempSize = historySongList.size
        val action =
            HistoryFragmentDirections.actionHistoryFragmentToPlayerFragment(
                position, id,
                0
            )
        findNavController().navigate(action)
        fromHistory = true
        fromFavorite = false
        fromPlaylist = false
        tempPosition = position

    }

    override fun onAnyItemLongClicked(position: Int) {
        //nothing to do
    }

    override fun onItemDeleteClicked(position: Int) {

    }
}