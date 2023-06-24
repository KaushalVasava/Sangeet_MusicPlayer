package com.lahsuak.sangeetmusicplayer.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.lahsuak.sangeetmusicplayer.R
import com.lahsuak.sangeetmusicplayer.model.Playlist
import com.lahsuak.sangeetmusicplayer.ui.fragments.PlayListFragment.Companion.musicPlaylist

class PlaylistAdapter(
    private var list: ArrayList<Playlist>,
    private val listener: PlaylistListener
) : RecyclerView.Adapter<PlaylistAdapter.MyViewHolder?>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.playlist_item, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val song = list[position]
        holder.playListTitle.text = song.name
        Glide.with(holder.itemView.context).load(song.playlist[0].artUri)
            .error(R.drawable.image_background)
            .transition(DrawableTransitionOptions.withCrossFade()).into(holder.playlistImage)

        holder.itemView.setOnClickListener {
            listener.onItemClicked(position)
        }
        holder.deletePlaylist.setOnClickListener {
            listener.onItemDelete(position)
        }
        holder.itemView.setOnLongClickListener {
            listener.onAnyItemLongClicked(position)
            return@setOnLongClickListener true
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var playListTitle: TextView = itemView.findViewById(R.id.playListName)
        var playlistImage: ImageView = itemView.findViewById(R.id.songImage)
        var deletePlaylist: ImageView = itemView.findViewById(R.id.deletePlaylist)
    }

    fun refreshPlaylist() {
        list = ArrayList()
        list.addAll(musicPlaylist.ref!!)
        notifyItemRangeChanged(0, list.size)
    }
}

interface PlaylistListener {
    fun onItemClicked(position: Int)
    fun onAnyItemLongClicked(position: Int)
    fun onItemDelete(position: Int)
}