package com.auplayer.player

import android.app.Service
import android.content.ContentUris
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.media.AudioFocusRequestCompat
import com.auplayer.player.domain.model.SoundItem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val TAG = "PlaybackService"

class PlaybackService : LifecycleService(),
    MediaPlayer.OnPreparedListener,
    MediaPlayer.OnInfoListener,
    MediaPlayer.OnCompletionListener,
    MediaPlayer.OnBufferingUpdateListener,
    MediaPlayer.OnErrorListener,
    MediaPlayer.OnSeekCompleteListener
{

    private var mediaPlayer: MediaPlayer? = null

    private var isPlaying : MutableStateFlow<Boolean> = MutableStateFlow(false)
    var playingState : StateFlow<Boolean> = isPlaying.asStateFlow()
    private val binder = LocalBinder()

    private lateinit var soundItem : SoundItem

    private lateinit var playerController: PlayerController

    override fun onCreate() {
        super.onCreate()
        isPlaying = MutableStateFlow(false)
        playingState = isPlaying.asStateFlow()
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setOnPreparedListener(this@PlaybackService)
            setOnInfoListener(this@PlaybackService)
            setOnCompletionListener(this@PlaybackService)
            setOnBufferingUpdateListener(this@PlaybackService)
            setOnErrorListener(this@PlaybackService)
            setOnSeekCompleteListener(this@PlaybackService)
        }
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        val requestListener = AudioManager.OnAudioFocusChangeListener { focus ->
            when(focus){
                AudioManager.AUDIOFOCUS_GAIN ->{
                    if(!mediaPlayer!!.isPlaying){
                        mediaPlayer!!.start()
                    }
                }

                AudioManager.AUDIOFOCUS_LOSS ->{
                    if (mediaPlayer != null) {
                        mediaPlayer!!.stop()
                        mediaPlayer!!.release()
                        mediaPlayer = null
                    }
                }

                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ->{
                    if(mediaPlayer != null && mediaPlayer!!.isPlaying){
                        mediaPlayer!!.pause()
                    }
                }
            }
        }


        audioManager.requestAudioFocus(requestListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val action = intent?.action
        if (action != null) {
            when (action) {
                PLAY -> {
                    soundItem = intent.getParcelableExtra<SoundItem>("soundItem")!!
                    playerController.playSound(soundItem)
                    isPlaying.value = false
                }
                STOP -> {
                    if (mediaPlayer!!.isPlaying) {
                        mediaPlayer!!.pause()
                        lifecycleScope.launch{
                            isPlaying.emit(false)
                        }

                    } else {
                        mediaPlayer!!.start()
                        lifecycleScope.launch{
                            isPlaying.emit(true)
                        }
                    }
                }
                NEXT -> {
                    isPlaying.value = false
                    playerController.nextSound()
                }
                PREVIOUS -> {
                    isPlaying.value = false
                    playerController.previousSound()
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onPrepared(p0: MediaPlayer?) {
        Log.e(TAG, "onPrepared: called" )
        if (p0 != null) {
            p0.start()
            if (p0.isPlaying) {
                p0.isLooping = true
                lifecycleScope.launch {
                    isPlaying.emit(true)
                }
            }
        }
    }

    override fun onInfo(p0: MediaPlayer?, p1: Int, p2: Int): Boolean {
        Log.e(TAG, "onInfo: called" )
        return true
    }

    override fun onCompletion(p0: MediaPlayer?) {
        if(p0 != null && !p0.isLooping){
            isPlaying.value = false
            playerController.nextSound()
        }
    }

    override fun onBufferingUpdate(p0: MediaPlayer?, p1: Int) {
        Log.e(TAG, "onBufferingUpdate: called" )
    }

    override fun onError(p0: MediaPlayer?, p1: Int, p2: Int): Boolean {
        Log.e(TAG, "onError: called" )
        return true
    }

    override fun onSeekComplete(p0: MediaPlayer?) {
        Log.e(TAG, "onSeekComplete: called")
    }

    fun getMediaPlayer(): MediaPlayer {
        return mediaPlayer!!
    }

    fun setController(playerController: PlayerController){
        this.playerController = playerController
    }

    fun getSoundItem() : SoundItem?{
        if (::soundItem.isInitialized){
            return soundItem
        }
        return null
    }

    fun setCurrentSoundItem(soundItem: SoundItem){
        this.soundItem = soundItem
    }
    inner class LocalBinder : Binder() {

        fun getService(): PlaybackService = this@PlaybackService
    }
}