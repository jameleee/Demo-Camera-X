package com.example.democamerax.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import com.example.democamerax.utils.ScreenUtils

/**
 * Copyright © 2018 AsianTech inc.
 * Create by Dat Bui T. on 4/18/19.
 */
class ExampleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var viewSize: Int = 0
    private val paint = Paint()
    private var rect = Rect()

    init {
        viewSize = ScreenUtils.getScreenWidth(context)
        paint.isAntiAlias = true
        paint.isDither = true
        paint.color = -0x11e951ea
        paint.strokeWidth = 4f
        paint.style = Paint.Style.STROKE
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(ScreenUtils.getScreenWidth(context), ScreenUtils.getScreenHeight(context))
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawRect(
            rect,
            paint
        )
    }

    fun reDrawRect(rect: Rect) {
        this.rect = rect
        invalidate()
    }
}
