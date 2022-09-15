package com.auplayer.player.ui.audiolist

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auplayer.player.data.SoundRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AudioListViewModel @Inject constructor(
    private val soundRepository: SoundRepository
) : ViewModel() {



    private val _state = MutableStateFlow(AudioListUiState())
    val state = _state.asStateFlow()


    fun getAllSounds(contentResolver: ContentResolver){
        viewModelScope.launch {
            soundRepository.getSounds(contentResolver).collect{ list ->
                _state.update {
                    it.copy(list = list)
                }
            }
        }
    }

    fun playSound(uri: Uri){
        viewModelScope.launch {
        }
    }
}