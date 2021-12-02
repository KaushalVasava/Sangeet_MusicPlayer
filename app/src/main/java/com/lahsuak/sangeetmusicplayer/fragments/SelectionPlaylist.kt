package com.lahsuak.sangeetmusicplayer.fragments


import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.lahsuak.sangeetmusicplayer.fragments.HomeFragment.Companion.list
import com.lahsuak.sangeetmusicplayer.R
import com.lahsuak.sangeetmusicplayer.adapters.SongAdapter
import com.lahsuak.sangeetmusicplayer.adapters.SongListener
import com.lahsuak.sangeetmusicplayer.databinding.FragmentSelectionPlaylistBinding
import com.lahsuak.sangeetmusicplayer.model.Songs
import java.util.*
import kotlin.collections.ArrayList

class SelectionPlaylist : Fragment(R.layout.fragment_selection_playlist), SongListener {

    private lateinit var binding: FragmentSelectionPlaylistBinding

    lateinit var adapter: SongAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentSelectionPlaylistBinding.inflate(inflater, container, false)

        val sharePrefLayout = requireContext().getSharedPreferences("LAYOUT", Context.MODE_PRIVATE)
        val layout = sharePrefLayout.getBoolean("layout", false)

        if (layout) {
            binding.selectionRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        } else {
            binding.selectionRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        }
        adapter = SongAdapter(requireContext(),list,this@SelectionPlaylist,isSelectionList = true)
        binding.selectionRecyclerView.adapter =adapter

        (requireActivity() as AppCompatActivity).supportActionBar!!.hide()

        // Redirect system "Back" press to our dispatcher
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedDispatcher)

        binding.searchBtn.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextChange(newText: String?): Boolean {
                val input = newText!!.lowercase(Locale.getDefault())
                val myFiles = ArrayList<Songs>()
                for (item in list) {
                    if (item.name!!.lowercase(Locale.getDefault()).contains(input)) {
                        myFiles.add(item)
                    }
                }
                // isSearch = true
                // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                adapter.updateList(myFiles)
                //}
                return true
            }

            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }
        })

        binding.selectionBackBtn.setOnClickListener {
            onBackPressed()
        }

        return binding.root
    }
    private val backPressedDispatcher = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            // Redirect to our own function
            this@SelectionPlaylist.onBackPressed()
        }
    }

    private fun onBackPressed() {
        val navController = findNavController()
        navController.popBackStack()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        (requireActivity() as AppCompatActivity).supportActionBar!!.show()
    }
    override fun onItemClicked(position: Int, id: Int) {
       //all works related to select song for playlist is done in SongAdapter addSongs() method
    }

    override fun onAnyItemLongClicked(position: Int) {
      //no need to implement this functionality
    }

    override fun onItemDeleteClicked(position: Int) {
    }
}