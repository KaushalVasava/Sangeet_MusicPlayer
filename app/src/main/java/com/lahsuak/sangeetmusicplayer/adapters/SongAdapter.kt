package com.lahsuak.sangeetmusicplayer.adapters


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lahsuak.sangeetmusicplayer.*
import com.lahsuak.sangeetmusicplayer.activity.HomeActivity.Companion.favoriteList
import com.lahsuak.sangeetmusicplayer.fragments.HomeFragment.Companion.counter
import com.lahsuak.sangeetmusicplayer.fragments.HomeFragment.Companion.is_in_action_mode
import com.lahsuak.sangeetmusicplayer.fragments.HomeFragment.Companion.is_select_all
import com.lahsuak.sangeetmusicplayer.fragments.HomeFragment.Companion.selectedItem
import com.lahsuak.sangeetmusicplayer.fragments.PlayListFragment.Companion.musicPlaylist
import com.lahsuak.sangeetmusicplayer.fragments.PlayerFragment.Companion.service
import com.lahsuak.sangeetmusicplayer.fragments.PlaylistDetails.Companion.currentPlaylistPos
import com.lahsuak.sangeetmusicplayer.model.*
import java.lang.Exception
import java.util.*

class SongAdapter constructor(
    private var context: Context, private var list: ArrayList<Songs>,
    listener1: SongListener, private val isPlaylistDetails: Boolean = false,
    private val isSelectionList: Boolean = false, private val isFavoriteList: Boolean = false,
    private val isHistoryList: Boolean = false
) : RecyclerView.Adapter<SongAdapter.MyViewHolder?>() {
    private val listener: SongListener = listener1
    lateinit var viewGroup: ViewGroup

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val sharePrefLayout = context.getSharedPreferences("LAYOUT", Context.MODE_PRIVATE)
        val layout = sharePrefLayout.getBoolean("layout", false)
        val view: View
        viewGroup =parent
        if (layout)
            view = LayoutInflater.from(context).inflate(R.layout.view_item_grid, parent, false)
        else
            view = LayoutInflater.from(context).inflate(R.layout.view_item, parent, false)

        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        if (isSelectionList || isHistoryList || is_in_action_mode) {
            holder.more.visibility = View.GONE
        } else {
            holder.more.visibility = View.VISIBLE
        }
        if (!is_in_action_mode) {
            holder.more.setImageResource(R.drawable.ic_more)
        } else {
            if (is_select_all) {
//                holder.more.setImageResource(R.drawable.ic_done)
//                holder.more.visibility = View.VISIBLE
                if (selectedItem != null) {
                    holder.more.setImageResource(R.drawable.ic_done)
                    holder.more.visibility = View.VISIBLE
                    selectedItem!![position] = true
                }
            } else {
                if (selectedItem != null) {
                    if (!selectedItem!![position]) {
                        holder.more.visibility = View.GONE
                        selectedItem!![position] = false
                        //  selectedItem!![position] = true
                    } else if (selectedItem!![position]) {
                        holder.more.visibility = View.VISIBLE
                        holder.more.setImageResource(R.drawable.ic_done)
                    }
                }
                if (counter == 0) {
                    holder.more.visibility = View.GONE
                    //selectedItem!![position]=false
                }
            }
        }

        holder.songTitle.text = list[position].name
        holder.artistName.text = list[position].artist
        holder.songDuration.text = formattedTime(list[position].duration!!.toInt() / 1000)

        if (!isSelectionList && !isPlaylistDetails && !isFavoriteList && !isHistoryList) {
            holder.itemView.setOnLongClickListener {
                listener.onAnyItemLongClicked(position)
                if (selectedItem == null) {
                    //do nothing
                } else {
                    if (selectedItem!![position]) {
                        holder.more.visibility = View.VISIBLE
                        holder.more.setImageResource(R.drawable.ic_done)
                        Log.d("Item", "value if long : ${selectedItem!![position]}")
                    } else {
                        Log.d("Item", "value else long : ${selectedItem!![position]}")
                        holder.more.visibility = View.GONE
                        holder.more.setImageResource(R.drawable.ic_more)
                    }
                }
                return@setOnLongClickListener true
            }
        }
        /** BUG 0001: WHEN USER SCROLL A RECYCLER VIEW IT GET DELAY OR SLOW SCROLLING
         * image load in background thread */

        Thread {
            val artUri = Uri.parse(list[position].artUri)
            var haveImage = true
            try {
                val stream = context.contentResolver.openInputStream(artUri!!)
            } catch (e: Exception) {
                haveImage = false
            }
            (context as Activity).runOnUiThread { //LOAD IMAGE ON UI THREAD
                if (haveImage)
                    holder.songImage.setImageURI(artUri)
                else
                    holder.songImage.setImageResource(R.drawable.image_background)
            }

        }.start()

        holder.itemView.setOnClickListener {
            if (is_in_action_mode) {
                if (!selectedItem!![position]) {
                    holder.more.visibility = View.VISIBLE
                    holder.more.setImageResource(R.drawable.ic_done)

                } else {
                    holder.more.visibility = View.GONE
                    holder.more.setImageResource(R.drawable.ic_more)
                }
            }

            if (isSelectionList) {
                if (addSongs(list[position])) {
                    holder.more.visibility = View.VISIBLE
                    holder.more.setImageResource(R.drawable.ic_done)
                } else {
                    holder.more.visibility = View.GONE
                    holder.more.setImageResource(R.drawable.ic_more)
                }
            } else {
                listener.onItemClicked(
                    position,
                    list[position].id
                )
            }
        }
        holder.more.setOnClickListener { v ->
            val popupMenu = PopupMenu(context, v)
            popupMenu.menuInflater.inflate(R.menu.popup_menu, popupMenu.menu)
            popupMenu.show()
            if (isPlaylistDetails) {
                popupMenu.menu.getItem(3).title = "Delete from playlist"
                popupMenu.menu.getItem(0).isVisible = false
            }
            if (isFavoriteList) {
                popupMenu.menu.getItem(3).title = "Remove from favorite"
                popupMenu.menu.getItem(0).isVisible = false
            }

            popupMenu.setOnMenuItemClickListener { item ->

                when (item.itemId) {
                    R.id.addToPlaylist -> {
                        addToPlaylistDialog(context, viewGroup, list, position)
                    }
                    R.id.details -> showDetailsDialog(context, list, position)
                    R.id.delete -> {
                        if (isFavoriteList) {
                            // item.title="Remove from favorite"
                            val index = getFavoriteIndex(list[position].id)
                            if (index != -1) {
                                if (service!!.position != position) {
                                    favoriteList!!.removeAt(index)
                                    notifyItemRemoved(position)
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Can't remove song from favorites, because song is currently playing!!" +
                                                "\nPlease change your song and try again",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        } else if (isPlaylistDetails) {
                            if ((service != null && service!!.position != position) || service == null) {
                                musicPlaylist.ref!![currentPlaylistPos].playlist.removeAt(
                                    position
                                )
//                            notifyItemRemoved(position)
                                notifyDataSetChanged()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Can't delete song from playlist, because song is currently playing!!" +
                                            "\nPlease change your song and try again",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            val builder = MaterialAlertDialogBuilder(context)
                            builder.setTitle("Delete")
                                .setMessage("Do you want to delete this song?")
                                .setPositiveButton("Delete") { dialog, _ ->
                                    listener.onItemDeleteClicked(position)
                                    dialog.dismiss()
                                }
                                .setNegativeButton("No") { dialog, _ ->
                                    dialog.dismiss()
                                }
                            val deleteDialog = builder.create()
                            deleteDialog.show()
                        }
                    }
                    R.id.share -> shareMusic(context, list, position)
                }
                true
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var songTitle: TextView = itemView.findViewById(R.id.songTitle)
        var artistName: TextView = itemView.findViewById(R.id.artistName)
        var songImage: ImageView = itemView.findViewById(R.id.songImage)
        var more: ImageView = itemView.findViewById(R.id.more_option)
        var songDuration: TextView = itemView.findViewById(R.id.songDuration)
        //        var animation: ImageView = itemView.findViewById(R.id.playAnimation)
    }

    private fun addSongs(song: Songs): Boolean {
        musicPlaylist.ref!![currentPlaylistPos].playlist.forEachIndexed { index, music ->
            if (song.id == music.id) {
                musicPlaylist.ref!![currentPlaylistPos].playlist.removeAt(index)
                return false
            }
        }
        musicPlaylist.ref!![currentPlaylistPos].playlist.add(song)
        return true
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newList: ArrayList<Songs>?) {
        list = ArrayList()
        list.addAll(newList!!)
        notifyDataSetChanged()
    }
}

interface SongListener {
    fun onItemClicked(position: Int, id: Int)
    fun onAnyItemLongClicked(position: Int)
    fun onItemDeleteClicked(position: Int)
}