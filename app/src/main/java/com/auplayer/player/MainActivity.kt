package com.auplayer.player

import android.media.AudioManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val controllerInsets = WindowCompat.getInsetsController(window, window.decorView)
        window.statusBarColor = resources.getColor(R.color.white)
        controllerInsets.isAppearanceLightStatusBars = true
        window.navigationBarColor = resources.getColor(R.color.white)
        controllerInsets.isAppearanceLightNavigationBars = true

        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        AudioManager.OnAudioFocusChangeListener {
            
        }
    }
}