package com.lahsuak.sangeetmusicplayer.util

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.lahsuak.sangeetmusicplayer.R
import com.lahsuak.sangeetmusicplayer.model.Playlist
import com.lahsuak.sangeetmusicplayer.model.Songs
import com.lahsuak.sangeetmusicplayer.ui.activity.HomeActivity
import com.lahsuak.sangeetmusicplayer.ui.fragments.PlayListFragment
import java.io.File
import java.io.IOException
import java.util.Random

object SongUtil {
    //Add to playlist dialog
    fun addToPlaylistDialog(
        context: Context,
        lists: ArrayList<Songs>,
        position: Int
    ) {
        val playlistDialog = LayoutInflater.from(context)
            .inflate(R.layout.playlist_dialog, null)
        val builder = MaterialAlertDialogBuilder(context)
        builder.setView(playlistDialog)
            .setTitle("Add Playlist")
        val dialogShow = builder.create()
        dialogShow.show()
        val getList: ArrayList<String> = ArrayList<String>()
        for (i in PlayListFragment.musicPlaylist.ref!!) {
            getList.add(i.name)
        }
        getList.add("Create New Playlist")

        val listView = playlistDialog.findViewById(R.id.listView) as ListView
        listView.adapter = ArrayAdapter(
            context, android.R.layout.simple_list_item_1, getList
        )
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ ->
            if (pos == getList.size - 1) {
                //when playlists are not available
                val playlistDialog1 = LayoutInflater.from(context)
                    .inflate(R.layout.add_playlist_dialog, null)
                val builder1 = MaterialAlertDialogBuilder(context)
                val textPlaylistName =
                    playlistDialog1.findViewById<TextInputEditText>(R.id.editPlaylistName)

                val introText = "New Playlist ${PlayListFragment.musicPlaylist.ref!!.size + 1}"
                textPlaylistName.setText(introText)
                builder1.setView(playlistDialog1)
                    .setTitle("Create Playlist")
                    .setMessage("Enter Playlist name")
                    .setPositiveButton("Create") { dialog, _ ->
                        val playlistName = textPlaylistName.text
                        if (playlistName != null) {
                            if (playlistName.isNotEmpty()) {
                                val available = addToPlaylist(context, playlistName.toString())
                                if (available) {
                                    PlayListFragment.musicPlaylist.ref!![pos].playlist.add(lists[position])
                                    Toast.makeText(
                                        context,
                                        "Song added to playlist ${PlayListFragment.musicPlaylist.ref!![pos].name}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }

                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }.show()
            } else { // when playlists are available
                var isExist = false
                for (i in PlayListFragment.musicPlaylist.ref!![pos].playlist) {
                    //item already exist
                    if (i.id == lists[position].id) {
                        Toast.makeText(
                            context,
                            "Song already in the playlist ${PlayListFragment.musicPlaylist.ref!![pos].name}",
                            Toast.LENGTH_SHORT
                        ).show()
                        isExist = true
                        break
                    }
                }
                //item not exist
                if (!isExist) {
                    PlayListFragment.musicPlaylist.ref!![pos].playlist.add(lists[position])
                    Toast.makeText(
                        context,
                        "Song added to playlist ${PlayListFragment.musicPlaylist.ref!![pos].name}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            dialogShow.dismiss()
        }
    }

    private fun addToPlaylist(context: Context, playlistName: String): Boolean {
        var playlistExists = false
        for (item in PlayListFragment.musicPlaylist.ref!!) {
            if (playlistName == item.name) {
                playlistExists = true
                break
            }
        }
        if (playlistExists) {
            Toast.makeText(
                context, "Playlist Exists!!", Toast.LENGTH_SHORT
            ).show()
            playlistExists = false
        } else {
            val tempPlaylist = Playlist()
            tempPlaylist.name = playlistName
            tempPlaylist.playlist = ArrayList()
            PlayListFragment.musicPlaylist.ref!!.add(tempPlaylist)
            //playlistAdapter!!.refreshPlaylist()
            playlistExists = true
        }
        return playlistExists
    }

    //music information dialog
    @SuppressLint("SetTextI18n")
    fun showDetailsDialog(context: Context, lists: ArrayList<Songs>, position: Int) {
        val detailsDialog = Dialog(context)
        detailsDialog.setContentView(R.layout.details_bottomsheet_dialog)
        val textPath = detailsDialog.findViewById<TextView>(R.id.filePath)
        val textName = detailsDialog.findViewById<TextView>(R.id.fileName)
        val textArtist = detailsDialog.findViewById<TextView>(R.id.fileArtist)
        val textFormat = detailsDialog.findViewById<TextView>(R.id.fileFormat)
        val textLength = detailsDialog.findViewById<TextView>(R.id.fileLength)
        val textAlbum = detailsDialog.findViewById<TextView>(R.id.fileAlbum)
        val textComposer = detailsDialog.findViewById<TextView>(R.id.fileComposer)
        val btnOk = detailsDialog.findViewById<MaterialButton>(R.id.okBtn)

        val path = getRealPath(Uri.parse(lists[position].path!!), context)
        ("File Path: $path").also { textPath!!.text = it }
        ("Song Name: " + lists[position].name).also { textName!!.text = it }
        ("Artist Name: " + lists[position].artist).also { textArtist!!.text = it }
        val fileFormat = "File Format: " + lists[position].type
        textFormat!!.text = fileFormat
        val length =
            "File Length : " + "%.2f".format(lists[position].length!!.toFloat() / (1024 * 1024)) + " mb"
        textLength!!.text = length
        textAlbum!!.text = "Album Name : " + lists[position].album
        textComposer!!.text = "Composer Name : " + lists[position].composer
        //builder.show()
        btnOk!!.setOnClickListener {
            detailsDialog.dismiss()
        }
        detailsDialog.show()
    }

    //get storage path of media file for android Q and above
    private fun getRealPath(uri: Uri, context: Context): String {
        var realPath: String? = null
        try {
            if (uri.scheme.equals("content", true)) {
                val projection = arrayOf("_data")
                val cursor = context.contentResolver.query(
                    uri,
                    projection, null, null, null
                )
                if (cursor != null) {
                    val id = cursor.getColumnIndexOrThrow("_data")
                    cursor.moveToNext()
                    realPath = cursor.getString(id)
                    cursor.close()
                } else if (uri.scheme.equals("file", true)) {
                    realPath = uri.path!!
                }
            }
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
        return realPath!!
    }

    //share music files
    fun shareMusic(context: Context, lists: ArrayList<Songs>, position: Int) {
        var path: Uri? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //val path1  = EXTERNAL_CONTENT_URI.toString() +"/"+lists[position].displayName
            try {
                val file = File(
                    getRealPath(
                        Uri.parse(lists[position].path!!),
                        context
                    )
                )//lists[position].path!!)
                path = FileProvider.getUriForFile(
                    context,
                    context.getString(R.string.file_provider), file
                )
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            try {
                path = FileProvider.getUriForFile(
                    context,
                    context.getString(R.string.file_provider),
                    File(lists[position].path!!)
                )
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        val sendIntent = Intent().apply {
            // Put the Uri and MIME type in the result Intent
            action = Intent.ACTION_SEND
            type = "audio/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_STREAM, path!!)//Uri.parse(list[position].path))
        }
        context.startActivity(
            Intent.createChooser(
                sendIntent,
                "Sharing ${lists[position].name}"
            )
        )
    }

    fun shareMusicMultiple(context: Context, lists: ArrayList<Songs>) {
        val paths = ArrayList<Uri>()
        for (i in 0 until lists.size) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    paths.add(
                        FileProvider.getUriForFile(
                            context,
                            context.getString(R.string.file_provider),
                            File(getRealPath(Uri.parse(lists[i].path!!), context))
                        )
                    )
                } else {
                    paths.add(
                        FileProvider.getUriForFile(
                            context,
                            context.getString(R.string.file_provider),
                            File(lists[i].path!!)
                        )
                    )
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, paths)//Uri.parse(list[position].path))
        }
        context.startActivity(
            Intent.createChooser(
                sendIntent,
                "Sharing ${paths.size} files"
            )
        )
    }

    //find position of history song
    fun checkHistorySong(id: Int, list: List<Songs>, position: Int) {
        var isFound = false
        for (i in 0 until HomeActivity.historySongList.size) {
            if (id == HomeActivity.historySongList[i].id) {
                HomeActivity.historySongList.removeAt(i)
                HomeActivity.historySongList.add(0, list[position])
                isFound = true
                break
            }
        }
        if (!isFound) {
            HomeActivity.historySongList.add(0, list[position])
        }
    }

    //find position of favorite song
    fun getFavoriteIndex(id: Int): Int {
        HomeActivity.isFavorite = false
        HomeActivity.favoriteList!!.forEachIndexed { index, song ->
            if (id == song.id) {
                HomeActivity.isFavorite = true
                return index
            }
        }
        return -1
    }

    //find position for song when from search or from recent song
    fun findPosition(id: Int, list: ArrayList<Songs>): Int {
        var position = -1
        Log.d("DATA", "list ${list.size} and id : $id")
        for (i in 0 until list.size) {
            if (id == list[i].id) {
                position = i
                break
            }
        }
        return position
    }

    //animation for song image
    fun songImageAnimation(context: Context?, imageView: ImageView, bitmap: Uri?) {
        val animOut = AnimationUtils.loadAnimation(context, R.anim.fade_out)
        val animIn = AnimationUtils.loadAnimation(context, R.anim.fade_in)
        animOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                imageView.setImageURI(bitmap)
                animIn.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation) {}
                    override fun onAnimationEnd(animation: Animation) {}
                    override fun onAnimationRepeat(animation: Animation) {}
                })
                imageView.startAnimation(animIn)
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
        imageView.startAnimation(animOut)
    }

    //formatted time format for seek bar time
    fun getFormattedTime(currentPosition: Int): String {
        val totalOld: String
        var totalNew: String
        val sec = (currentPosition % 60).toString()
        val min = (currentPosition / 60).toString()
        totalOld = "$min:$sec"
        totalNew = "$min:0$sec"
        totalNew = if (sec.length == 1) {
            return totalNew
        } else {
            totalOld
        }
        return totalNew
    }

    //generate random number for shuffle playback song
    fun getRandom(number: Int): Int {
        val random = Random()
        return random.nextInt(number + 1)
    }

    //Fetch Songs from Internal storage
    @SuppressLint("Recycle")
    fun getSongs(context: Context, sortOrder: String): ArrayList<Songs> {
        val list = ArrayList<Songs>()
        var order: String? = null
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL_PRIMARY
            )
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        when (sortOrder) {
            "SortByName" -> {
                Log.d("S", sortOrder + 0)
                order = MediaStore.Audio.AudioColumns.TITLE + " ASC"
            }

            "SortByDate" -> {
                Log.d("S", sortOrder + 1)
                order = MediaStore.Audio.AudioColumns.DATE_ADDED + " DESC"
            }

            "SortBySize" -> {
                Log.d("S", sortOrder + 2)
                order = MediaStore.Audio.AudioColumns.SIZE + " DESC"
            }

            "SortByArtist" -> {
                order = MediaStore.Audio.AudioColumns.ARTIST + " ASC"
            }

            "SortByAlbum" -> {
                order = MediaStore.Audio.AudioColumns.ALBUM + " ASC"
            }

            "SortByComposer" -> {
                order = MediaStore.Audio.AudioColumns.COMPOSER + " ASC"
            }

        }

        val projection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(
                MediaStore.Audio.AudioColumns.TITLE,
                MediaStore.Audio.AudioColumns.ARTIST,
                MediaStore.Audio.Media.RELATIVE_PATH,
                MediaStore.Audio.AudioColumns.DURATION,
                MediaStore.Audio.AudioColumns._ID,
                MediaStore.Audio.AudioColumns.ALBUM,
                MediaStore.Audio.AudioColumns.SIZE,
                MediaStore.Audio.AudioColumns.COMPOSER,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.MIME_TYPE
            )
        } else {
            arrayOf(
                MediaStore.Audio.AudioColumns.TITLE,
                MediaStore.Audio.AudioColumns.ARTIST,
                MediaStore.Audio.AudioColumns.DATA,
                MediaStore.Audio.AudioColumns.DURATION,
                MediaStore.Audio.AudioColumns._ID,
                MediaStore.Audio.AudioColumns.ALBUM,
                MediaStore.Audio.AudioColumns.SIZE,
                MediaStore.Audio.AudioColumns.COMPOSER,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.MIME_TYPE
            )
        }

        val cursor = context.contentResolver.query(uri, projection, null, null, order)

        if (cursor != null) {
            while (cursor.moveToNext()) {
                if (cursor.getString(3).toLong() > 10000) {

                    val path: String
                    val artUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        cursor.getLong(8)
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        path =
                            ContentUris.withAppendedId(uri, cursor.getLong(4)).toString()
                        Log.d("PATH", "getSongs:  Q and + #$path and ${cursor.getString(10)}")
                    } else {
                        path = cursor.getString(2)
                        //Log.d("PATH", "getSongs: below Q #$path")
                    }
                    var composer = cursor.getString(7)

                    if (composer == null) {
                        Log.d("PATH", "getSongs: composer #${composer}")
                        composer = "<unknown>"
                    }
                    list.add(
                        Songs(
                            cursor.getString(0),
                            cursor.getString(1),
                            path,
                            cursor.getString(3),
                            cursor.getInt(4),
                            cursor.getString(5),
                            cursor.getString(6),
                            composer,
                            artUri.toString(),
                            cursor.getString(9),
                            cursor.getString(10)
                        )
                    )
                }
            }
        }
        cursor!!.close()
        return list
    }
}