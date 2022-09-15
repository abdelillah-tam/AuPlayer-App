package com.auplayer.player

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

const val PLAY = "PLAY"
const val STOP = "STOP"
const val NEXT = "NEXT"
const val PREVIOUS = "PREVIOUS"

@HiltAndroidApp
class Application : Application() {


    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val name = "playernotif"
            val description = "channel to control music player"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_PLAYER_ID, name, importance).apply {
                this.description = description
            }

            val notificationManager : NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object{
        const val CHANNEL_PLAYER_ID = "pospo"
    }
}