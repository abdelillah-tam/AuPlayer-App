package com.auplayer.player

import com.auplayer.player.domain.model.SoundItem
import kotlinx.coroutines.flow.Flow

interface PlayerController {


    fun playSound(soundItem: SoundItem)
    fun pauseSound() : Flow<Boolean>
    fun nextSound()
    fun previousSound()

}