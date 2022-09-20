package com.auplayer.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build


class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val intent1 = Intent(context, PlaybackService::class.java)
        when(action){
            PLAY ->{
                intent1.setAction(PLAY)
                val uri : Uri? = intent.getParcelableExtra<Uri>("uri")
                intent1.putExtra("uri", uri)
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent1)
                }else{
                    context.startService(intent1)
                }
            }
            PAUSE -> {
                intent1.setAction(PAUSE)
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent1)
                }else{
                    context.startService(intent1)
                }
            }
            NEXT -> {
                intent1.setAction(NEXT)
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent1)
                }else{
                    context.startService(intent1)
                }
            }
            PREVIOUS -> {
                intent1.setAction(PREVIOUS)
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent1)
                }else{
                    context.startService(intent1)
                }
            }
        }
    }
}