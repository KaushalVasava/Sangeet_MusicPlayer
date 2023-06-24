package com.lahsuak.sangeetmusicplayer.util

interface ActionPlaying {

    fun playClicked()
    fun nextClicked()
    fun prevClicked()
    fun shuffleOrRepeatClicked()
    fun setCurrentProgress(position: Int)
    fun favoriteClicked()
}