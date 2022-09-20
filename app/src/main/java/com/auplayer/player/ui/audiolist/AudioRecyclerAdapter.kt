package com.auplayer.player.ui.audiolist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.auplayer.player.R
import com.auplayer.player.domain.model.SoundItem
import com.google.android.material.textview.MaterialTextView
import javax.inject.Inject

class AudioRecyclerAdapter @Inject constructor() : RecyclerView.Adapter<AudioRecyclerAdapter.ViewHolder>() {

    private var list = emptyList<SoundItem>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.sound_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val soundName = holder.itemView.findViewById(R.id.sound_name) as MaterialTextView
        val soundArtist = holder.itemView.findViewById(R.id.sound_artist) as MaterialTextView

        val soundItem = list[position]
        soundName.text = soundItem.soundName
        soundArtist.text = soundItem.artist

    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun setList(list : List<SoundItem>){
        this.list = list
        notifyDataSetChanged()
    }

    fun getItem(position: Int) : SoundItem{
        return list[position]
    }
}