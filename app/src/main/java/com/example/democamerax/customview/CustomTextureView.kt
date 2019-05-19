package com.example.democamerax.customview

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.TextureView

/**
 * @author Dat Bui T. on 2019-05-19.
 */
class CustomTextureView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : TextureView(context, attrs, defStyleAttr) {

    private var onFocusListener: OnFocusListener? = null

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                if (event.pointerCount == 1) {
                    // Display focus indicator
                    setFocusViewWidthAnimation(event.x, event.y)
                }
                if (event.pointerCount == 2) {
                    Log.i("zxc", "ACTION_DOWN = " + 2)
                }
            }
        }
        return true
    }

    // Focus frame indicator animation
    private fun setFocusViewWidthAnimation(x: Float, y: Float) {
        onFocusListener?.onFocus(x, y)
    }

    private fun setOnFocusListener(onFocusListener: OnFocusListener) {
        this.onFocusListener = onFocusListener
    }

    /**
     * Interface to init the focus event
     */
    interface OnFocusListener {
        fun onFocus(x: Float, y: Float)
    }
}
