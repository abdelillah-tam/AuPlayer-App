package com.auplayer.player

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.motion.widget.MotionLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textview.MaterialTextView

class PlayerControllerMotionLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttrs: Int = 0
) : MotionLayout(context, attrs, defStyleAttrs) {

    private var motionLayout: MotionLayout

    var soundTitle: MaterialTextView
    var playAndPause: MaterialCheckBox
    var next: MaterialButton
    var previous: MaterialButton
    private var view: View


    init {
        motionLayout = LayoutInflater.from(context)
            .inflate(R.layout.player_layout, this, false) as MotionLayout
        addView(motionLayout)
        soundTitle = motionLayout.findViewById(R.id.player_sound_name) as MaterialTextView
        playAndPause = motionLayout.findViewById(R.id.play_and_pause) as MaterialCheckBox
        next = motionLayout.findViewById(R.id.next_audio) as MaterialButton
        previous = motionLayout.findViewById(R.id.previous_audio) as MaterialButton
        view = motionLayout.findViewById(R.id.backgrounds) as View


    }


    private fun touchEventInsideTargetView(v: View, ev: MotionEvent?): Boolean {
        if (ev != null && ev.x > v.left && ev.x < v.right) {
            if (ev.y > v.top && ev.y < v.bottom) {
                return true
            }
        }
        return false
    }


    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        return if (touchEventInsideTargetView(view, event)
        ) {

            super.onInterceptTouchEvent(event)

        } else {
            true
        }
    }

}