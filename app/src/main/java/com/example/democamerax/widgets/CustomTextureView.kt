package com.example.democamerax.widgets

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

    private var onCameraEventListener: OnCameraEventListener? = null
    private var isMutiplePoint = false

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                Log.i("zxc", "sss = ${event.pointerCount}")

                if (event.pointerCount == 1) {
                    // Display focus indicator
                    isMutiplePoint = false
                    setFocusViewWidthAnimation(event.x, event.y)
                }
                if (event.pointerCount == 2) {
                    isMutiplePoint = true
                } else isMutiplePoint = false
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 1) {
                    // Display focus indicator
                    isMutiplePoint = false
                }
                if (event.pointerCount == 2) {
                    Log.i("zxc", "ACTION_DOWN = " + 2)
                    isMutiplePoint = true
                } else isMutiplePoint = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (isMutiplePoint)
                    setZoomPreView(event)
            }
            MotionEvent.ACTION_UP -> {
                isMutiplePoint = false
            }
        }
        return true
    }

    // Focus frame indicator animation
    private fun setFocusViewWidthAnimation(x: Float, y: Float) {
        onCameraEventListener?.onFocus(x, y)
    }

    private fun setZoomPreView(event: MotionEvent) {
        onCameraEventListener?.onZoom(event)
    }

    fun setOnFocusListener(onCameraEventListener: OnCameraEventListener) {
        this.onCameraEventListener = onCameraEventListener
    }

    /**
     * Interface to init the focus event
     */
    interface OnCameraEventListener {
        fun onFocus(x: Float, y: Float)

        fun onZoom(event: MotionEvent)
    }
}
