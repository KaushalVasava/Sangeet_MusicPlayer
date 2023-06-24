package com.lahsuak.sangeetmusicplayer.ui.activity

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.lahsuak.sangeetmusicplayer.R
import com.lahsuak.sangeetmusicplayer.databinding.ActivityHomeBinding
import com.lahsuak.sangeetmusicplayer.model.MusicPlaylist
import com.lahsuak.sangeetmusicplayer.ui.fragments.PlayListFragment.Companion.musicPlaylist
import com.lahsuak.sangeetmusicplayer.model.Songs
import com.lahsuak.sangeetmusicplayer.util.AppConstants.FAVORITE
import com.lahsuak.sangeetmusicplayer.util.AppConstants.IN_BACKGROUND
import com.lahsuak.sangeetmusicplayer.util.AppConstants.MUSIC_LAST_PLAYED
import com.lahsuak.sangeetmusicplayer.util.AppConstants.PLAYLIST


class HomeActivity : AppCompatActivity() {

    private lateinit var binding : ActivityHomeBinding
    private lateinit var navController: NavController
    private lateinit var gestureDetector: GestureDetectorCompat

    companion object {
        var historySongList :ArrayList<Songs> = ArrayList()
        var favoriteList: ArrayList<Songs>? = ArrayList()
        @JvmStatic
        var position = -1
        var isFavorite = false

        //These two flags use for audio focus changes
        var fromHome = false
        var fromPlayer = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setTheme(R.style.Theme_MusicPlayer)
        fromHome = true
        fromPlayer = true
        val themePref = getSharedPreferences("THEME", Context.MODE_PRIVATE)
        val theme = themePref.getInt("theme", R.style.Theme_MusicPlayer)
        val color1 = themePref.getInt("colorNo", 0)
        when (color1) {
            0 -> {
                setTheme(theme)
                //window.navigationBarColor = ContextCompat.getColor(this, R.color.purple_500)
                //window.statusBarColor = ContextCompat.getColor(this, R.color.purple_700)
//               supportActionBar!!.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(
//                   this@HomeActivity,R.color.purple_500)))
            }
            1 -> {
                setTheme(theme)
                //window.navigationBarColor = ContextCompat.getColor(this, R.color.blue_500)
                //window.statusBarColor = ContextCompat.getColor(this, R.color.blue_700)

                // Set BackgroundDrawable
//               supportActionBar!!.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(
//                   this@HomeActivity,R.color.yellow_500)))
            }
            2 -> {
                setTheme(theme)
                //window.navigationBarColor = ContextCompat.getColor(this, R.color.red_500)
                //window.statusBarColor = ContextCompat.getColor(this, R.color.red_700)
//               supportActionBar!!.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(
//                   this@HomeActivity,R.color.red_500)))
            }
            3 -> {
                setTheme(theme)
                //window.navigationBarColor = ContextCompat.getColor(this, R.color.pink_500)
               // window.statusBarColor = ContextCompat.getColor(this, R.color.pink_700)
//               supportActionBar!!.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(
//                   this@HomeActivity,R.color.pink_500)))
            }
            4 -> {
                setTheme(theme)
               // window.navigationBarColor = ContextCompat.getColor(this, R.color.green_500)
              //  window.statusBarColor = ContextCompat.getColor(this, R.color.green_700)
//               supportActionBar!!.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(
//                   this@HomeActivity,R.color.green_500)))
            }
        }
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        WindowInsetsControllerCompat(window,window.decorView).isAppearanceLightNavigationBars =true

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        //set navigation graph
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_container) as NavHostFragment

        //set controller for navigation between fragments
        navController = navHostFragment.navController
        setupActionBarWithNavController(navController)//,appBarConfiguration)

        //fetch saved playlist
        val preference = getSharedPreferences(PLAYLIST, Context.MODE_PRIVATE)
        val jsonString = preference.getString("playlist", null)
        if (jsonString != null) {
            val data: MusicPlaylist =
                GsonBuilder().create().fromJson(jsonString, MusicPlaylist::class.java)
            musicPlaylist = data
        }
        //fetch saved recently played songs list
        val preferenceHistory = getSharedPreferences("HISTORY", MODE_PRIVATE)
        val jsonStringHistory = preferenceHistory.getString("recentSongs", null)
        val typeToken1 = object : TypeToken<ArrayList<Songs>>() {}.type
        if (jsonStringHistory != null) {
            historySongList.clear()
            val data: ArrayList<Songs> = GsonBuilder().create().fromJson(jsonStringHistory, typeToken1)
            Log.d("H", "onCreateView: history ${data.size}")
            historySongList.addAll(data)
        }
        //fetch saved favorites
        val editor1 = getSharedPreferences(FAVORITE, MODE_PRIVATE)
        val jsonString1 = editor1.getString("favorite", null)
        val typeToken = object : TypeToken<ArrayList<Songs>>() {}.type
        if (jsonString1 != null) {
            val temp: ArrayList<Songs> = ArrayList()
            temp.addAll(favoriteList!!)
            val data: ArrayList<Songs> = GsonBuilder().create().fromJson(jsonString1, typeToken)
            favoriteList!!.clear()
            if (data.size < temp.size)
                favoriteList!!.addAll(temp)
            else
                favoriteList!!.addAll(data)
        }
    }
    override fun onSupportNavigateUp(): Boolean {
        //Pass argument appBarConfiguration in navigateUp() method
        // for hamburger icon respond to click events
        //navConfiguration
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("DATA", "onDestroy: ")
        //when app close then change state to 'background=true'
        // for remove notification when player is pause or stop
        val editor =
            getSharedPreferences(MUSIC_LAST_PLAYED, Context.MODE_PRIVATE).edit()
        editor.putBoolean(IN_BACKGROUND, true)
        editor.apply()

        //store favorite song list into shared preference
        val editor1 = getSharedPreferences(FAVORITE, MODE_PRIVATE).edit()
        val json = GsonBuilder().create().toJson(favoriteList)
        editor1.putString("favorite", json)
        editor1.apply()
        //store playlist into shared preference
        val editor2 = getSharedPreferences(PLAYLIST, MODE_PRIVATE).edit()
        val json2 = GsonBuilder().create().toJson(musicPlaylist)
        editor2.putString("playlist", json2)
        editor2.apply()

        //store recently played songs list into shared preference
        val historyEditor = getSharedPreferences("HISTORY", MODE_PRIVATE).edit()
        val json3 = GsonBuilder().create().toJson(historySongList)
        historyEditor.putString("recentSongs", json3)
        historyEditor.apply()

        fromHome = false
        fromPlayer = false
    }

//    override fun onSupportNavigateUp(): Boolean {
//        return navController.navigateUp() || super.onSupportNavigateUp()
//    }
}
