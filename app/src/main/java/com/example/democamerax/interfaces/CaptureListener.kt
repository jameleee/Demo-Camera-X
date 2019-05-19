package com.example.democamerax.interfaces

/**
 * Copyright © 2018 AsianTech inc.
 * Create by Dat Bui T. on 4/18/19.
 */
interface CaptureListener {
    fun takePictures()

    fun recordStart()

    fun recordEnd(time: Long)

    fun recordZoom(zoom: Float)
}
