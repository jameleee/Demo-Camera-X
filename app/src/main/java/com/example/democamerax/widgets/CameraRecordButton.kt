package com.example.democamerax.widgets

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.democamerax.R
import com.example.democamerax.interfaces.CaptureListener
import com.example.democamerax.utils.ButtonState
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

/**
 * @author Dat Bui T. on 2019-05-16.
 */
class CameraRecordButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val STATE_IDLE = 10001        // Idle state
        const val STATE_CAPTURE = 10002       // Capture state
        const val STATE_LONG_PRESS = 10003  // Long press state
        const val STATE_RECORDING = 10004 // Recording state
        const val STATE_BAN = 10005
    }

    private var state = 0
    private var buttonState = 0

    private var eventY: Float = 0f // Touch event down

    private val paint: Paint = Paint()

    private var strokeWidth: Float = 0f
    private var outsideAddSize: Int = 0
    private var insideReduceSize: Int = 0

    private var centerX: Float = 0f
    private var centerY: Float = 0f

    private var buttonRadius: Float = 0f
    private var buttonOutsideRadius: Float = 0f
    private var buttonInsideRadius: Float = 0f
    private var circleRadius: Float = 0f
    private var buttonSize: Int = 0

    private var progress: Float = 360f
    private var recordedTime: Long = 0

    private var rectF: RectF = RectF()
    private var rectReDraw = RectF()

    private val animatorSet = AnimatorSet()

    private var disposable: Disposable? = null
    private var captureListener: CaptureListener? = null

    init {
        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CameraRecordButton, defStyleAttr, 0
        )
        buttonSize = a.getDimensionPixelSize(
            R.styleable.CameraRecordButton_buttonSize, TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 92f, resources.displayMetrics
            ).toInt()
        )

        a.recycle()

        buttonRadius = buttonSize / 2f

        buttonOutsideRadius = buttonRadius
        buttonInsideRadius = buttonRadius * 0.85f

        strokeWidth = buttonSize / 15f
        outsideAddSize = buttonSize / 5
        insideReduceSize = buttonSize / 8

        circleRadius = buttonRadius - outsideAddSize + strokeWidth / 2

        paint.isAntiAlias = true
        paint.flags = Paint.ANTI_ALIAS_FLAG

        state = STATE_IDLE
        buttonState = ButtonState.BUTTON_STATE_BOTH.type()

        centerX = (buttonSize + outsideAddSize * 2) / 2f
        centerY = (buttonSize + outsideAddSize * 2) / 2f

        rectF.set(
            RectF(
                centerX - outsideAddSize,
                centerY - outsideAddSize,
                centerX + outsideAddSize,
                centerY + outsideAddSize
            )
        )

        // Out side rectangle
        rectReDraw.set(
            RectF(
                centerX - (circleRadius + outsideAddSize - strokeWidth / 2),
                centerY - (circleRadius + outsideAddSize - strokeWidth / 2),
                centerX + (circleRadius + outsideAddSize - strokeWidth / 2),
                centerY + (circleRadius + outsideAddSize - strokeWidth / 2)
            )
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(buttonSize + outsideAddSize * 2, buttonSize + outsideAddSize * 2)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        // If the status is recording, draw a recording progress bar
        paint.color = ContextCompat.getColor(context, R.color.icFocused)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        canvas?.drawArc(rectReDraw, -90f, progress, false, paint)

        // Draw the filled circle in center
        paint.style = Paint.Style.FILL
        paint.color = ContextCompat.getColor(context, R.color.icActive)
        canvas?.drawCircle(centerX, centerY, buttonInsideRadius, paint)
        canvas?.drawRoundRect(rectF, 10f, 10f, paint)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                if (event.pointerCount > 1)
                    return false
                eventY = event.y
                animatorSet.cancel()
                if ((buttonState == ButtonState.BUTTON_STATE_ONLY_RECORDER.type() || buttonState == ButtonState.BUTTON_STATE_BOTH.type())) {
                    // Record video event
                    if (state == STATE_IDLE) startZoomAnimation() else handlerUnPressByState()
                } else if (buttonState == ButtonState.BUTTON_STATE_ONLY_CAPTURE.type()) {
                    // Capture picture event
                    if (state == STATE_IDLE) startCaptureAnimation(buttonInsideRadius)
                }
            }
            MotionEvent.ACTION_MOVE -> if (captureListener != null
                && state == STATE_RECORDING
                && (buttonState == ButtonState.BUTTON_STATE_ONLY_RECORDER.type()
                        || buttonState == ButtonState.BUTTON_STATE_BOTH.type())
            ) {
                captureListener?.recordZoom(eventY - event.y)
            }
            MotionEvent.ACTION_UP -> {
                if (buttonState == ButtonState.BUTTON_STATE_ONLY_CAPTURE.type() && state == STATE_CAPTURE)
                    stopCaptureAnimation(buttonRadius * 0.85f)
            }
        }
        return true
    }

    //The logic that is processed when the finger releases the button
    private fun handlerUnPressByState() {
        disposable?.dispose()
        // Process according to current state
        recordEnd()
    }

    private fun animateRoundOutsize(time: Long) {
        var isDrawIn = false
        disposable = Observable.interval(time, TimeUnit.MILLISECONDS).subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread()).subscribe {
                when {
                    strokeWidth <= buttonSize / 15f -> {
                        strokeWidth += 1f
                        isDrawIn = false
                    }
                    strokeWidth >= buttonSize / 15f + 20 -> {
                        strokeWidth -= 1f
                        isDrawIn = true
                    }
                    isDrawIn -> strokeWidth -= 1f
                    else -> strokeWidth += 1f
                }
                redrawRectF()
                invalidate()
            }
    }

    private fun recordEnd() {
        if (captureListener != null) {
            captureListener?.recordEnd(recordedTime)
        }
        resetRecordAnim()
    }

    // Remastered state
    private fun resetRecordAnim() {
        state = STATE_IDLE
        // Restore button initial state animation
        startRecordAnimation(
            buttonOutsideRadius,
            buttonRadius,
            buttonInsideRadius,
            buttonRadius * 0.85f,
            buttonRadius,
            buttonRadius - outsideAddSize + strokeWidth / 2
        )
    }

    private fun startZoomAnimation() {
        state = STATE_RECORDING
        // No recording permission
        // todo will open later
        // Start button animation, the outer circle becomes zoom animation, and the inner circle to the border rectangle
        startRecordAnimation(
            strokeWidth,
            strokeWidth + 20,
            buttonInsideRadius,
            0f,
            circleRadius,
            buttonRadius
        )
    }

    private fun startCaptureAnimation(insideRadius: Float) {
        state = STATE_CAPTURE
        val insideAnim = ValueAnimator.ofFloat(insideRadius, insideRadius * 0.75f)
        insideAnim.addUpdateListener { animation ->
            buttonInsideRadius = animation.animatedValue as Float
            invalidate()
        }

        /*insideAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                // Callback camera interface
                captureListener?.takePictures()
            }
        })*/
        insideAnim.duration = 100
        insideAnim.start()
    }

    private fun stopCaptureAnimation(insideRadius: Float) {
        state = STATE_IDLE

        captureListener?.takePictures()

        val insideAnim = ValueAnimator.ofFloat(insideRadius * 0.75f, insideRadius)
        insideAnim.addUpdateListener { animation ->
            buttonInsideRadius = animation.animatedValue as Float
            invalidate()
        }
        insideAnim.duration = 100
        insideAnim.start()
    }

    private fun startRecordAnimation(
        outsideStart: Float,
        outsideEnd: Float,
        insideStart: Float,
        insideEnd: Float,
        circleStart: Float,
        circleEnd: Float
    ) {
        val outsideAnim = ValueAnimator.ofFloat(outsideStart, outsideEnd)
        val insideAnim = ValueAnimator.ofFloat(insideStart, insideEnd)
        val circleAnim = ValueAnimator.ofFloat(circleStart, circleEnd)
        disposable?.dispose()
        animateRoundOutsize(30)
        // outside circle animation
        outsideAnim.addUpdateListener { animation ->
            buttonOutsideRadius = animation.animatedValue as Float
            invalidate()
        }
        // Inner circle animation
        insideAnim.addUpdateListener { animation ->
            buttonInsideRadius = animation.animatedValue as Float
            invalidate()
        }
        // Circle animation
        circleAnim.addUpdateListener { animation ->
            circleRadius = animation.animatedValue as Float
            invalidate()
        }
        // Start recording Runnable when the animation is over and call back the recording start interface
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                // Set to recording status
                if (state == STATE_RECORDING) {
                    if (captureListener != null)
                        captureListener?.recordStart()
                } else if (state == STATE_IDLE) {
                    disposable?.dispose()
                    strokeWidth = buttonSize / 15f
                    redrawRectF()
                    invalidate()
                }
            }
        })
        animatorSet.playTogether(outsideAnim, insideAnim, circleAnim)
        animatorSet.duration = 500
        animatorSet.start()
    }

    private fun redrawRectF() {
        rectReDraw.set(
            RectF(
                centerX - (circleRadius + outsideAddSize - strokeWidth / 2),
                centerY - (circleRadius + outsideAddSize - strokeWidth / 2),
                centerX + (circleRadius + outsideAddSize - strokeWidth / 2),
                centerY + (circleRadius + outsideAddSize - strokeWidth / 2)
            )
        )
    }

    // init capture listener
    fun setCaptureListener(captureListener: CaptureListener) {
        this.captureListener = captureListener
    }

    // animatorSet button state
    fun setButtonState(state: Int) {
        buttonState = state
        Log.d("zxc", "stateeeee $state")
    }
}
