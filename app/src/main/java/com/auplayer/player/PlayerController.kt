package com.auplayer.player

import com.auplayer.player.domain.model.SoundItem

interface PlayerController {


    fun playSound(soundItem: SoundItem)
    fun nextSound()
    fun previousSound()

}