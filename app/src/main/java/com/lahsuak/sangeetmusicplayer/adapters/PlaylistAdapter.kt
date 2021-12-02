package com.lahsuak.sangeetmusicplayer.adapters

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lahsuak.sangeetmusicplayer.R
import com.lahsuak.sangeetmusicplayer.fragments.PlayListFragment.Companion.musicPlaylist
import com.lahsuak.sangeetmusicplayer.model.Songs
import java.lang.Exception
import java.util.ArrayList

class PlaylistAdapter constructor(
    context1: Context,
    var list: ArrayList<Songs.Playlist>?,
    listener1: PlaylistListener
) : RecyclerView.Adapter<PlaylistAdapter.MyViewHolder?>() {
    private val listener: PlaylistListener = listener1
    private var context: Context = context1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.playlist_item, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.playListTitle.text = list!![position].name
        holder.itemView.setOnLongClickListener {
            listener.onAnyItemLongClicked(position)
            return@setOnLongClickListener true
        }
        /** BUG 0001: WHEN USER SCROLL A RECYCLER VIEW IT GET DELAY OR SLOW SCROLLING
         * image load in background thread */
        Thread {
            var artUri: Uri? = null
            if (list != null)
                if (list!![position].playlist.size > 0) {
                    artUri = Uri.parse(list!![position].playlist[0].artUri)
                }
            var haveImage = true
            try {
                val stream = context.contentResolver.openInputStream(artUri!!)
            } catch (e: Exception) {
                haveImage = false
            }
            (context as Activity).runOnUiThread { //LOAD IMAGE ON UI THREAD
                if(haveImage)
                    holder.playlistImage.setImageURI(artUri)
                else
                    holder.playlistImage.setImageResource(R.drawable.image_background)
            }
        }.start()

        holder.itemView.setOnClickListener {
            listener.onItemClicked(position)
        }
        holder.deletePlaylist.setOnClickListener { v ->
            listener.onItemDelete(position)
        }
    }

    override fun getItemCount(): Int {
        return list!!.size
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var playListTitle: TextView = itemView.findViewById(R.id.playListName)
        var playlistImage: ImageView = itemView.findViewById(R.id.songImage)
        var deletePlaylist: ImageView = itemView.findViewById(R.id.deletePlaylist)
    }


    fun refreshPlaylist() {
        list = ArrayList()
        list!!.addAll(musicPlaylist.ref!!)
        notifyDataSetChanged()
    }


}

interface PlaylistListener {
    fun onItemClicked(position: Int)
    fun onAnyItemLongClicked(position: Int)
    fun onItemDelete(position: Int)
}