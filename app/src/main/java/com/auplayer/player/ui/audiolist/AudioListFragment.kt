package com.auplayer.player.ui.audiolist

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class AudioListFragment : Fragment(R.layout.fragment_audio_list), PlayerController {

    private lateinit var binding: FragmentAudioListBinding

    private lateinit var mPlaybackService: PlaybackService

    private var mBound: Boolean = false

    private val audioListViewModel: AudioListViewModel by viewModels()

    @Inject
    lateinit var audioRecyclerAdapter: AudioRecyclerAdapter

    private lateinit var gestureDetector: GestureDetectorCompat

    private lateinit var timer: Timer

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            val binder = p1 as PlaybackService.LocalBinder
            mPlaybackService = binder.getService()
            mBound = true
            mPlaybackService.setController(this@AudioListFragment)
            lifecycleScope.launch {
                mPlaybackService.playingState.collect { isPlaying ->
                    if (mPlaybackService.getSoundItem() != null) {
                        val uri = ContentUris.withAppendedId(
                            Uri.parse(ALBUM_ART_URI),
                            mPlaybackService.getSoundItem()!!.albumId + 0L
                        )
                        var picture: Bitmap?
                        runBlocking(Dispatchers.IO) {
                            try {
                                picture =
                                    Glide.with(requireContext()).asBitmap().load(uri).submit().get()
                            } catch (e: Exception) {
                                picture = null
                                e.printStackTrace()
                            }
                        }
                        if (picture != null) {
                            binding.playerController.playerImage.setImageBitmap(picture)
                        } else binding.playerController.playerImage.setImageDrawable(
                            ResourcesCompat.getDrawable(
                                resources,
                                R.drawable.auplayer_disc,
                                null
                            )
                        )
                        binding.playerController.soundTitle.text = mPlaybackService.getSoundItem()!!.soundName
                        binding.playerController.playAndPause.isChecked = !isPlaying
                        binding.playerController.totalTime.text = milliSecondsToTime(mPlaybackService.getSoundItem()!!.duration)
                        binding.playerController.soundTimerSlider.apply {
                            valueFrom = 0f
                            valueTo = mPlaybackService.getSoundItem()!!.duration.toFloat()
                        }
                        if (isPlaying) {
                            timer.schedule(object : TimerTask() {
                                override fun run() {
                                    runBlocking(Dispatchers.Main) {
                                        binding.playerController.soundTimer.text = milliSecondsToTime(mPlaybackService.getMediaPlayer().currentPosition.toLong())
                                        binding.playerController.soundTimerSlider.value = mPlaybackService.getMediaPlayer().currentPosition.toFloat()
                                    }
                                }

                            }, 0, 500)
                        }
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
        timer = Timer()
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
                        mPlaybackService.currentPosition =
                            binding.listOfSounds.getChildAdapterPosition(view)
                        val soundItem =
                            audioRecyclerAdapter.getItem(position = mPlaybackService.currentPosition)
                        val intent = Intent(requireContext(), PlaybackService::class.java)
                            .setAction(PLAY)
                            .putExtra("soundItem", soundItem)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            mPlaybackService.startForegroundService(intent)
                        } else {
                            mPlaybackService.startService(intent)
                        }
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mPlaybackService.startForegroundService(intent)
            } else {
                mPlaybackService.startService(intent)
            }
        }

        binding.playerController.playAndPause.setOnClickListener {
            val intent = Intent(requireContext(), PlaybackService::class.java)
                .setAction(PAUSE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mPlaybackService.startForegroundService(intent)
            } else {
                mPlaybackService.startService(intent)
            }

        }

        binding.playerController.previous.setOnClickListener {
            val intent = Intent(requireContext(), PlaybackService::class.java)
                .setAction(PREVIOUS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mPlaybackService.startForegroundService(intent)
            } else {
                mPlaybackService.startService(intent)
            }
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

    override fun pauseSound(): Flow<Boolean> = flow {
        if (mPlaybackService.getMediaPlayer().isPlaying) {
            mPlaybackService.getMediaPlayer().pause()
            emit(true)
        } else {
            mPlaybackService.getMediaPlayer().start()
            emit(false)
        }
    }


    override fun nextSound() {
        if (mPlaybackService.currentPosition < audioRecyclerAdapter.itemCount) {
            mPlaybackService.currentPosition += 1
            val nextSoundItem = audioRecyclerAdapter.getItem(mPlaybackService.currentPosition)
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
        if (mPlaybackService.currentPosition >= 0) {
            mPlaybackService.currentPosition -= 1
            val previousSoundItem = audioRecyclerAdapter.getItem(mPlaybackService.currentPosition)
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

    private fun milliSecondsToTime(milliseconds: Long): String {
        val minutes = milliseconds / 1000 / 60
        val seconds = milliseconds / 1000 % 60

        return String.format("%02d:%02d", minutes, seconds)

    }
}