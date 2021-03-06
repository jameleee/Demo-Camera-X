package com.example.democamerax.utils

import android.content.Context
import android.graphics.Matrix
import android.hardware.display.DisplayManager
import android.util.Size
import android.view.*
import androidx.camera.core.Preview
import androidx.camera.core.PreviewConfig
import java.lang.ref.WeakReference
import java.util.*

/**
 * @author Dat Bui T. on 2019-05-16.
 *
 * Builder for [Preview] that takes in a [WeakReference] of the view finder and
 * [PreviewConfig], then instantiates a [Preview] which automatically
 * resize and rotates reacting to config changes.
 */
class AutoFitPreviewBuilder private constructor(
    config: PreviewConfig,
    viewFinderRef: WeakReference<TextureView>
) {
    // Initialize public use-case with the given config
    val preview: Preview

    /** Internal variable used to keep track of the use-case's output rotation */
    private var bufferRotation: Int = 0
    /** Internal variable used to keep track of the view's rotation */
    private var viewFinderRotation: Int? = null
    /** Internal variable used to keep track of the use-case's output dimension */
    private var bufferDimens: Size = Size(0, 0)
    /** Internal variable used to keep track of the view's dimension */
    private var viewFinderDimens: Size = Size(0, 0)
    /** Internal variable used to keep track of the view's display */
    private var viewFinderDisplay: Int = -1

    private lateinit var displayManager: DisplayManager
    /** We need a display listener for 180 degree device orientation changes */
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) {
            val viewFinder = viewFinderRef.get() ?: return
            if (displayId != viewFinderDisplay) {
                val display = displayManager.getDisplay(displayId)
                val rotation = getDisplaySurfaceRotation(display)
                updateTransform(viewFinder, rotation, bufferDimens, viewFinderDimens)
            }
        }
    }

    init {
        val viewFinder = viewFinderRef.get() ?: throw IllegalArgumentException("Invalid reference to view finder used")

        // Initialize the display and rotation from texture view information
        viewFinderDisplay = viewFinder.display.displayId
        viewFinderRotation = getDisplaySurfaceRotation(viewFinder.display) ?: 0

        preview = Preview(config)

        // Every time the viewfinder is updated, recompute layout
        preview.setOnPreviewOutputUpdateListener {
            // To update the SurfaceTexture, we have to remove it and re-add it
            (viewFinder.parent as ViewGroup).apply {
                removeView(viewFinder)
                addView(viewFinder, 0)
            }

            // Set the texture view to show the preview
            viewFinder.surfaceTexture = it.surfaceTexture
            bufferRotation = it.rotationDegrees
            val rotation = getDisplaySurfaceRotation(viewFinder.display)

            //Update transform
            updateTransform(viewFinder, rotation, it.textureSize, viewFinderDimens)
        }

        // Every time the provided texture view changes, recompute layout
        viewFinder.addOnLayoutChangeListener { view, left, top, right, bottom, _, _, _, _ ->
            val newViewFinderDimens = Size(right - left, bottom - top)
            val rotation = getDisplaySurfaceRotation(viewFinder.display)
            updateTransform(view as TextureView, rotation, bufferDimens, newViewFinderDimens)
        }

        // Every time the orientation of device changes, recompute layout
        displayManager = viewFinder.context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.registerDisplayListener(displayListener, null)

        // Remove the display listeners when the view is detached to avoid
        // holding a reference to the View outside of a Fragment.
        // NOTE: Even though using a weak reference should take care of this,
        // we still try to avoid unnecessary calls to the listener this way.
        viewFinder.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View?) = Unit
            override fun onViewDetachedFromWindow(view: View?) {
                displayManager.unregisterDisplayListener(displayListener)
            }
        })
    }

    /** Helper function that fits a camera preview into the given [TextureView] */
    private fun updateTransform(
        textureView: TextureView?,
        rotation: Int?,
        newBufferDimens: Size,
        newViewFinderDimens: Size
    ) {
        // This should happen anyway, but now the linter knows
        if (textureView == null) return

        if (rotation == viewFinderRotation &&
            Objects.equals(newBufferDimens, bufferDimens) &&
            Objects.equals(newViewFinderDimens, viewFinderDimens)
        ) {
            // Nothing has changed, no need to transform output again
            return
        }

        if (rotation == null) {
            // Invalid rotation - wait for valid inputs before setting matrix
            return
        } else {
            // Update internal field with new inputs
            viewFinderRotation = rotation
        }

        if (newBufferDimens.width == 0 || newBufferDimens.height == 0) {
            // Invalid buffer dimens - wait for valid inputs before setting matrix
            return
        } else {
            // Update internal field with new inputs
            bufferDimens = newBufferDimens
        }

        if (newViewFinderDimens.width == 0 || newViewFinderDimens.height == 0) {
            // Invalid view finder dimens - wait for valid inputs before setting matrix
            return
        } else {
            // Update internal field with new inputs
            viewFinderDimens = newViewFinderDimens
        }

        val matrix = Matrix()


        // Compute the center of the view finder
        val centerX = viewFinderDimens.width / 2f
        val centerY = viewFinderDimens.height / 2f

        // Correct preview output to account for display rotation
        viewFinderRotation?.let { matrix.postRotate(-it.toFloat(), centerX, centerY) }

        // Buffers are rotated relative to the device's 'natural' orientation: swap width and height
        val bufferRatio = bufferDimens.height / bufferDimens.width.toFloat()

        val scaledWidth: Int
        val scaledHeight: Int
        // Match longest sides together -- i.e. apply center-crop transformation
        if (viewFinderDimens.width > viewFinderDimens.height) {
            scaledHeight = viewFinderDimens.width
            scaledWidth = Math.round(viewFinderDimens.width * bufferRatio)
        } else {
            scaledHeight = viewFinderDimens.height
            scaledWidth = Math.round(viewFinderDimens.height * bufferRatio)
        }

        // Compute the relative scale value
        val xScale = scaledWidth / viewFinderDimens.width.toFloat()
        val yScale = scaledHeight / viewFinderDimens.height.toFloat()

        // Scale input buffers to fill the view finder
        matrix.preScale(xScale, yScale, centerX, centerY)

        // Finally, apply transformations to our TextureView
        textureView.setTransform(matrix)
    }

    companion object {
        /** Helper function that gets the rotation of a [Display] in degrees */
        fun getDisplaySurfaceRotation(display: Display?) = when (display?.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> null
        }

        /**
         * Main entrypoint for users of this class: instantiates the adapter and returns an instance
         * of [Preview] which automatically adjusts in size and rotation to compensate for
         * config changes.
         */
        fun build(config: PreviewConfig, viewFinder: TextureView) =
            AutoFitPreviewBuilder(config, WeakReference(viewFinder)).preview
    }
}