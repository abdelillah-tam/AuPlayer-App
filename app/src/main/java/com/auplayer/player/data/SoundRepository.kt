package com.auplayer.player.data

import android.content.ContentResolver
import android.net.Uri
import com.auplayer.player.domain.model.SoundItem
import kotlinx.coroutines.flow.Flow

interface SoundRepository {

    fun getSounds(contentResolver: ContentResolver) : Flow<List<SoundItem>>
}