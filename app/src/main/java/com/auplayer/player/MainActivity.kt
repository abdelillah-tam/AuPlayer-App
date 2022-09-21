package com.auplayer.player

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_screen)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controllerInsets = WindowCompat.getInsetsController(window, window.decorView)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        controllerInsets.isAppearanceLightStatusBars = true
        controllerInsets.isAppearanceLightNavigationBars = true

        Handler().postDelayed({
            setContentView(R.layout.activity_main)
            window.statusBarColor = ResourcesCompat.getColor(resources, R.color.white, null)
            WindowCompat.setDecorFitsSystemWindows(window, true)
            window.navigationBarColor = ResourcesCompat.getColor(resources, R.color.white, null)

        }, 2000)





    }
}