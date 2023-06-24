package com.lahsuak.sangeetmusicplayer.model

data class Songs(
    val name: String?,
    val artist: String?,
    val path: String?,
    val duration: String?,
    val id: Int,
    val album: String?,
    val length: String,
    val composer: String?,
    val artUri: String?,
    val displayName: String,
    val type: String
)

class Playlist {
    lateinit var name: String
    lateinit var playlist: ArrayList<Songs>
}

class MusicPlaylist {
    var ref: ArrayList<Playlist>? = ArrayList()
}