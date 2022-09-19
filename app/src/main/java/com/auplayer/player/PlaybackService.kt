package com.auplayer.player

import android.app.Notification
import android.app.PendingIntent
import android.content.ContentUris
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.auplayer.player.domain.model.SoundItem
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private const val TAG = "PlaybackService"

class PlaybackService : LifecycleService(),
    MediaPlayer.OnPreparedListener,
    MediaPlayer.OnInfoListener,
    MediaPlayer.OnCompletionListener,
    MediaPlayer.OnBufferingUpdateListener,
    MediaPlayer.OnErrorListener,
    MediaPlayer.OnSeekCompleteListener {

    private var mediaPlayer: MediaPlayer? = null

    var currentPosition = 0

    private var isPlaying: MutableStateFlow<Boolean> = MutableStateFlow(false)
    var playingState: StateFlow<Boolean> = isPlaying.asStateFlow()
    private val binder = LocalBinder()

    private lateinit var soundItem: SoundItem

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
            when (focus) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    if (!mediaPlayer!!.isPlaying && playingState.value) {
                        mediaPlayer!!.start()
                    }
                }

                AudioManager.AUDIOFOCUS_LOSS -> {
                    if (mediaPlayer != null) {
                        mediaPlayer!!.stop()
                        mediaPlayer!!.release()
                        mediaPlayer = null
                    }
                }

                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
                        mediaPlayer!!.pause()
                    }
                }
            }
        }


        audioManager.requestAudioFocus(
            requestListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
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
                            lifecycleScope.launch {
                                isPlaying.emit(false)
                            }
                            startForeground(
                                1,
                                showNotification(getSoundItem()!!.soundName, playingState.value)
                            )

                        } else {
                            mediaPlayer!!.start()
                            lifecycleScope.launch {
                                isPlaying.emit(true)
                            }
                            startForeground(
                                1,
                                showNotification(getSoundItem()!!.soundName, playingState.value)
                            )
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onPrepared(p0: MediaPlayer?) {
        Log.e(TAG, "onPrepared: called")
        if (p0 != null) {
            p0.start()
            if (p0.isPlaying) {
                p0.isLooping = true
                lifecycleScope.launch {
                    isPlaying.emit(true)
                }
                startForeground(1, showNotification(getSoundItem()!!.soundName, playingState.value))
            }
        }
    }

    override fun onInfo(p0: MediaPlayer?, p1: Int, p2: Int): Boolean {
        Log.e(TAG, "onInfo: called")
        return true
    }

    override fun onCompletion(p0: MediaPlayer?) {
        if (p0 != null && !p0.isLooping) {
            isPlaying.value = false
            playerController.nextSound()
        }
    }

    override fun onBufferingUpdate(p0: MediaPlayer?, p1: Int) {
        Log.e(TAG, "onBufferingUpdate: called")
    }

    override fun onError(p0: MediaPlayer?, p1: Int, p2: Int): Boolean {
        Log.e(TAG, "onError: called")
        return true
    }

    override fun onSeekComplete(p0: MediaPlayer?) {
        Log.e(TAG, "onSeekComplete: called")
    }

    fun getMediaPlayer(): MediaPlayer {
        return mediaPlayer!!
    }

    fun setController(playerController: PlayerController) {
        this.playerController = playerController
    }

    fun getSoundItem(): SoundItem? {
        if (::soundItem.isInitialized) {
            return soundItem
        }
        return null
    }


    fun setCurrentSoundItem(soundItem: SoundItem) {
        this.soundItem = soundItem
    }

    private fun showNotification(soundName: String, playing: Boolean): Notification {

        val activityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val activityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            activityIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val playIntent = Intent(this, NotificationReceiver::class.java).setAction(STOP)
        val play = PendingIntent.getBroadcast(
            this,
            1,
            playIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val nextIntent = Intent(this, NotificationReceiver::class.java).setAction(NEXT)
        val next = PendingIntent.getBroadcast(
            this,
            2,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val previousIntent =
            Intent(this, NotificationReceiver::class.java).setAction(PREVIOUS)
        val previous = PendingIntent.getBroadcast(
            this,
            3,
            previousIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val albumArt = Uri.parse("content://media/external/audio/albumart")
        val uri = ContentUris.withAppendedId(
            albumArt,
            getSoundItem()!!.albumId + 0L
        )
        var picture: Bitmap?
        runBlocking(Dispatchers.IO) {
            try {
                picture = Glide.with(applicationContext).asBitmap().load(uri).submit().get()
            } catch (e: Exception) {
                picture = null
                e.printStackTrace()
            }
        }

        val builder =
            NotificationCompat.Builder(this, Application.CHANNEL_PLAYER_ID).apply {
                setPriority(NotificationCompat.PRIORITY_MAX)
                setOngoing(true)
                setContentIntent(activityPendingIntent)
                setStyle(androidx.media.app.NotificationCompat.MediaStyle())
                setSmallIcon(R.drawable.ic_launcher_background)
                if (picture != null) {
                    setLargeIcon(
                        picture
                    )
                } else {
                    setLargeIcon(
                        ResourcesCompat.getDrawable(
                            resources,
                            R.drawable.ic_launcher_background,
                            null
                        )!!.toBitmap()
                    )
                }
                setContentTitle(soundName)
                setOnlyAlertOnce(true)
                setColorized(true)
                setShowWhen(false)
                addAction(R.drawable.ic_previous, "Previous", previous)
                if (playing) {
                    addAction(R.drawable.ic_pause, "play or stop", play)
                } else {
                    addAction(R.drawable.ic_play, "play or stop", play)
                }
                addAction(R.drawable.ic_next, "Next", next)

            }.build()

        return builder
    }

    inner class LocalBinder : Binder() {

        fun getService(): PlaybackService = this@PlaybackService
    }
}