package com.auplayer.player.data

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import com.auplayer.player.PLAY
import com.auplayer.player.PlaybackService
import com.auplayer.player.domain.model.SoundItem
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class SoundRepositoryImpl @Inject constructor(): SoundRepository {

    @SuppressLint("Range")
    override fun getSounds(contentResolver: ContentResolver): Flow<List<SoundItem>> = flow {
        val PROJECTION = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.IS_MUSIC,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val SELECTION = MediaStore.Audio.Media.IS_MUSIC + " != ?" +
                " AND " + MediaStore.Audio.Media.DURATION + " >= ?"
        val selectionArgs = arrayOf("0", "60000")

        val cursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            PROJECTION,
            SELECTION,
            selectionArgs,
            MediaStore.Audio.Media.DATE_MODIFIED +" DESC"
        )
        val idIndex = cursor!!.getColumnIndex(MediaStore.Audio.Media._ID)
        val soundNameIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
        val durationIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
        val artistIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
        val isMusicIndex = cursor.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC)
        val dataIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
        val albumIdIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)

        val list = mutableListOf<SoundItem>()
        while (cursor.moveToNext()) {
            list.add(
                SoundItem(
                    cursor.getLong(idIndex),
                    cursor.getString(dataIndex),
                    cursor.getString(soundNameIndex),
                    cursor.getLong(durationIndex),
                    cursor.getString(artistIndex),
                    cursor.getInt(isMusicIndex),
                    cursor.getInt(albumIdIndex)
                )
            )
        }
        cursor.close()
        emit(list)
    }

}