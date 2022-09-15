package com.auplayer.player.ui.audiolist

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.content.Context.NOTIFICATION_SERVICE
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.app.NotificationCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.auplayer.player.*
import com.auplayer.player.databinding.FragmentAudioListBinding
import com.auplayer.player.domain.model.SoundItem
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class AudioListFragment : Fragment(R.layout.fragment_audio_list), PlayerController {

    private lateinit var binding: FragmentAudioListBinding

    private lateinit var mPlaybackService: PlaybackService
    private var mBound: Boolean = false
    private val audioListViewModel: AudioListViewModel by viewModels()

    private var mCurrentPosition = 0

    @Inject
    lateinit var audioRecyclerAdapter: AudioRecyclerAdapter


    private lateinit var gestureDetector: GestureDetectorCompat


    private val connection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            val binder = p1 as PlaybackService.LocalBinder
            mPlaybackService = binder.getService()
            mBound = true
            mPlaybackService.setController(this@AudioListFragment)
            lifecycleScope.launch {
                mPlaybackService.playingState.collect { isPlaying ->
                    if (mPlaybackService.getSoundItem() != null) {
                        showNotification(mPlaybackService.getSoundItem()!!.soundName, isPlaying)
                        binding.playerController.soundTitle.text =
                            mPlaybackService.getSoundItem()!!.soundName
                        binding.playerController.playAndPause.isChecked = !isPlaying
                    }
                }

            }
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            mBound = false
        }

    }


    @SuppressLint("UnsafeRepeatOnLifecycleDetector")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAudioListBinding.bind(view)

        gestureDetector =
            GestureDetectorCompat(requireContext(), object : GestureDetector.OnGestureListener {
                override fun onDown(p0: MotionEvent?): Boolean {
                    return false
                }

                override fun onShowPress(p0: MotionEvent?) {

                }

                override fun onSingleTapUp(p0: MotionEvent?): Boolean {
                    val view = binding.listOfSounds.findChildViewUnder(p0!!.x, p0.y)
                    if (view != null && mBound) {
                        mCurrentPosition = binding.listOfSounds.getChildAdapterPosition(view)
                        val soundItem = audioRecyclerAdapter.getItem(position = mCurrentPosition)
                        val intent = Intent(requireContext(), PlaybackService::class.java)
                            .setAction(PLAY)
                            .putExtra("soundItem", soundItem)
                        mPlaybackService.startService(intent)
                    }
                    return true
                }

                override fun onScroll(
                    p0: MotionEvent?,
                    p1: MotionEvent?,
                    p2: Float,
                    p3: Float
                ): Boolean {
                    return true
                }

                override fun onLongPress(p0: MotionEvent?) {

                }

                override fun onFling(
                    p0: MotionEvent?,
                    p1: MotionEvent?,
                    p2: Float,
                    p3: Float
                ): Boolean {
                    return false
                }

            })
        binding.listOfSounds.also {
            it.adapter = audioRecyclerAdapter
            it.layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            it.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                    gestureDetector.onTouchEvent(e)
                    return false
                }

            })


        }
        if (activity?.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 56)
        } else {
            showAllSounds()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    audioListViewModel.state.collect {
                        audioRecyclerAdapter.setList(it.list)
                    }
                }
            }
        }

        binding.playerController.next.setOnClickListener {
            val intent = Intent(requireContext(), PlaybackService::class.java)
                .setAction(NEXT)
            mPlaybackService.startService(intent)
        }

        binding.playerController.playAndPause.setOnClickListener {
            val intent = Intent(requireContext(), PlaybackService::class.java)
                .setAction(STOP)
            mPlaybackService.startService(intent)

        }

        binding.playerController.previous.setOnClickListener {
            val intent = Intent(requireContext(), PlaybackService::class.java)
                .setAction(PREVIOUS)
            mPlaybackService.startService(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(requireContext(), PlaybackService::class.java).also { intent ->
            activity?.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

    }


    override fun onStop() {
        super.onStop()
        activity?.unbindService(connection)
        mBound = false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            56 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showAllSounds()
                }
            }
        }
    }

    private fun showAllSounds() {
        audioListViewModel.getAllSounds(requireActivity().contentResolver)
    }


    private fun showNotification(soundName: String, playing: Boolean) {

        val activityIntent = Intent(requireContext(), MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val activityPendingIntent = PendingIntent.getActivity(
            requireContext(),
            0,
            activityIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val playIntent = Intent(requireContext(), NotificationReceiver::class.java).setAction(STOP)
        val play = PendingIntent.getBroadcast(
            requireContext(),
            1,
            playIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val nextIntent = Intent(requireContext(), NotificationReceiver::class.java).setAction(NEXT)
        val next = PendingIntent.getBroadcast(
            requireContext(),
            2,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val previousIntent =
            Intent(requireContext(), NotificationReceiver::class.java).setAction(PREVIOUS)
        val previous = PendingIntent.getBroadcast(
            requireContext(),
            3,
            previousIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val albumArt = Uri.parse("content://media/external/audio/albumart")
        val uri = ContentUris.withAppendedId(
            albumArt,
            mPlaybackService.getSoundItem()!!.albumId + 0L
        )
        var picture: Bitmap?
        runBlocking(Dispatchers.IO) {
            try {
                picture = Glide.with(requireContext()).asBitmap().load(uri).submit().get()
            } catch (e: Exception) {
                picture = null
                e.printStackTrace()
            }
        }

        val builder =
            NotificationCompat.Builder(requireContext(), Application.CHANNEL_PLAYER_ID).apply {
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
                addAction(R.drawable.ic_previous, "Previous", previous)
                if (playing) {
                    addAction(R.drawable.ic_pause, "play or stop", play)
                } else {
                    addAction(R.drawable.ic_play, "play or stop", play)
                }
                addAction(R.drawable.ic_next, "Next", next)

            }.build()


        val notificationManager =
            activity?.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0, builder)
    }

    override fun playSound(soundItem: SoundItem) {
        val uri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            soundItem.id
        )
        mPlaybackService.getMediaPlayer().apply {
            reset()
            setDataSource(requireContext(), uri)
            prepareAsync()

        }
    }

    override fun nextSound() {
        if (mCurrentPosition < audioRecyclerAdapter.itemCount) {
            mCurrentPosition += 1
            val nextSoundItem = audioRecyclerAdapter.getItem(mCurrentPosition)
            val uri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                nextSoundItem.id
            )
            mPlaybackService.setCurrentSoundItem(nextSoundItem)
            mPlaybackService.getMediaPlayer().apply {
                reset()
                setDataSource(requireContext(), uri)
                prepareAsync()
            }
        }
    }

    override fun previousSound() {
        if (mCurrentPosition >= 0) {
            mCurrentPosition -= 1
            val previousSoundItem = audioRecyclerAdapter.getItem(mCurrentPosition)
            val uri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                previousSoundItem.id
            )
            mPlaybackService.setCurrentSoundItem(previousSoundItem)
            mPlaybackService.getMediaPlayer().apply {
                reset()
                setDataSource(requireContext(), uri)
                prepareAsync()
            }
        }
    }
}