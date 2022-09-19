package com.auplayer.player.domain.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


@Parcelize
data class SoundItem(
    val id: Long,
    val data : String,
    val soundName : String,
    val duration : Long,
    val artist : String,
    val isMusic : Int,
    val albumId : Int
) : Parcelable
