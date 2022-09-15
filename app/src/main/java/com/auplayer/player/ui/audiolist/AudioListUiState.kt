package com.auplayer.player.ui.audiolist

import com.auplayer.player.domain.model.SoundItem

data class AudioListUiState(
    var list : List<SoundItem> = emptyList()
)