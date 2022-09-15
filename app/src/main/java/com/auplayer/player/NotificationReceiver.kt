package com.auplayer.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log


class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val intent1 = Intent(context, PlaybackService::class.java)
        when(action){
            PLAY ->{
                intent1.setAction(PLAY)
                val uri : Uri? = intent.getParcelableExtra<Uri>("uri")
                intent1.putExtra("uri", uri)
                context.startService(intent1)
            }
            STOP -> {
                intent1.setAction(STOP)
                context.startService(intent1)
            }
            NEXT -> {
                intent1.setAction(NEXT)
                context.startService(intent1)
            }
            PREVIOUS -> {
                intent1.setAction(PREVIOUS)
                context.startService(intent1)
            }
        }
    }
}