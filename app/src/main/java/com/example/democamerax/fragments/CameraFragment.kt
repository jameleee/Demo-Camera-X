package com.example.democamerax.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.hardware.Camera
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.*
import android.webkit.MimeTypeMap
import androidx.camera.core.*
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.example.democamerax.MainActivity.Companion.KEY_EVENT_ACTION
import com.example.democamerax.MainActivity.Companion.KEY_EVENT_EXTRA
import com.example.democamerax.R
import com.example.democamerax.extensions.ANIMATION_FAST_MILLIS
import com.example.democamerax.extensions.ANIMATION_SLOW_MILLIS
import com.example.democamerax.extensions.ViewUtils
import com.example.democamerax.extensions.ViewUtils.toast
import com.example.democamerax.extensions.singleClick
import com.example.democamerax.interfaces.CaptureListener
import com.example.democamerax.utils.AutoFitPreviewBuilder
import com.example.democamerax.utils.ButtonState
import com.example.democamerax.utils.ImageUtils
import com.example.democamerax.utils.ScreenUtils
import kotlinx.android.synthetic.main.fragment_camera.*
import kotlinx.coroutines.*
import java.io.File
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

/**
 * @author Dat Bui T. on 2019-05-16.
 */
class CameraFragment : Fragment(), CoroutineScope {

    companion object {
        const val KEY_ROOT_DIRECTORY = "root_folder"
        val EXTENSION_WHITELIST = arrayOf("JPG", "JPEG", "MP4")

        private fun calculateTapArea(x: Float, y: Float, coefficient: Float, context: Context): Rect {
            val focusAreaSize = 300f
            val areaSize = java.lang.Float.valueOf(focusAreaSize * coefficient).toInt()
            val centerX = (x / ScreenUtils.getScreenWidth(context) * 2000 - 1000).toInt()
            val centerY = (y / ScreenUtils.getScreenHeight(context) * 2000 - 1000).toInt()
            val left = clamp(centerX - areaSize / 2, -1000, 1000)
            val top = clamp(centerY - areaSize / 2, -1000, 1000)
            val rectF = RectF(left.toFloat(), top.toFloat(), (left + areaSize).toFloat(), (top + areaSize).toFloat())
            return Rect(
                Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom)
            )
        }

        private fun clamp(x: Int, min: Int, max: Int): Int {
            return when {
                x > max -> max
                x < min -> min
                else -> x
            }
        }
    }

    private var lensFacing = CameraX.LensFacing.BACK
    private var isRecording = false
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture? = null
    private var type: String? = null
    private val job = Job()
    private lateinit var outputDirectory: File

    // Volume down button receiver
    private val volumeDownReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(KEY_EVENT_EXTRA, KeyEvent.KEYCODE_UNKNOWN)) {
                // When the volume down button is pressed, simulate a shutter button click
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    btnCapture.singleClick()
                }
            }
        }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Mark this as a retain fragment, so the lifecycle does not get restarted on config change
        retainInstance = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        outputDirectory = getOutputDirectory(requireContext())

        // Set up the intent filter that will receive events from our main activity
        val filter = IntentFilter().apply { addAction(KEY_EVENT_ACTION) }
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(volumeDownReceiver, filter)

        type = arguments?.getString(SplashFragment.KEY_TYPE)

        textureView.post {
            startCamera()
            // In the background, load latest photo taken (if any) for gallery thumbnail
            launch(coroutineContext) {
                outputDirectory.listFiles { file ->
                    EXTENSION_WHITELIST.contains(file.extension.toUpperCase())
                }.sorted().reversed().firstOrNull()?.let { setGalleryThumbnail(it) }
            }
        }

        // Init listener event
        initListener()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Unregister the broadcast receivers
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(volumeDownReceiver)

        CameraX.unbindAll()
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    private fun initListener() {
        // Set state to detect capture or record mode
        btnCapture.setButtonState(
            if (type == SplashFragment.KEY_CAPTURE)
                ButtonState.BUTTON_STATE_ONLY_CAPTURE.type()
            else
                ButtonState.BUTTON_STATE_ONLY_RECORDER.type()
        )

        // Build the image capture use case and attach button click listener
        btnCapture.setCaptureListener(object : CaptureListener {
            override fun takePictures() {
                captureImage(File(outputDirectory.path, "${System.currentTimeMillis()}.jpeg"))
                // We can only change the foreground Drawable using API level 23+ API
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Display flash animation to indicate that photo was captured
                    container.postDelayed({
                        container.foreground = ColorDrawable(Color.WHITE)
                        container.postDelayed({ container.foreground = null }, ANIMATION_FAST_MILLIS)
                    }, ANIMATION_SLOW_MILLIS)
                }
            }

            override fun recordStart() {
                recordVideo(File(outputDirectory.path, "${System.currentTimeMillis()}.mp4"))
            }

            override fun recordEnd(time: Long) {
                videoCapture?.stopRecording()
            }

            override fun recordZoom(zoom: Float) {
            }
        })

        /* btnCapture.setOnClickListener {
             if (type == SplashFragment.KEY_CAPTURE) {
                 captureImage(File(outputDirectory.path, "${System.currentTimeMillis()}.jpeg"))
             } else if (type == SplashFragment.KEY_RECORD) {
                 isRecording = !isRecording
                 recordVideo(File(outputDirectory.path, "${System.currentTimeMillis()}.mp4"))
             }

             // We can only change the foreground Drawable using API level 23+ API
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                 // Display flash animation to indicate that photo was captured
                 container.postDelayed({
                     container.foreground = ColorDrawable(Color.WHITE)
                     container.postDelayed({ container.foreground = null }, ANIMATION_FAST_MILLIS)
                 }, ANIMATION_SLOW_MILLIS)
             }
         }*/

        btnSwitch.setOnClickListener {
            lensFacing = if (CameraX.LensFacing.FRONT == lensFacing) {
                CameraX.LensFacing.BACK
            } else {
                CameraX.LensFacing.FRONT
            }
            try {
                // Only bind use cases if we can query a camera with this orientation
                val xx = CameraX.getCameraWithLensFacing(lensFacing)
                Log.d("zxc", "kskskskksksksk 000000 $xx")
                startCamera()
            } catch (exc: Exception) {
                // Do nothing
            }
        }

        btnGallery.setOnClickListener {
            Navigation.findNavController(requireActivity(), R.id.frContainer)
                .navigate(
                    R.id.action_cameraFragment_to_galleryFragment,
                    Bundle().apply { putString(KEY_ROOT_DIRECTORY, outputDirectory.absolutePath) })
        }
    }

    // Add this after onCreate
    private fun startCamera() {
        // Make sure that there are no other use cases bound to CameraX
        CameraX.unbindAll()

        val metrics = DisplayMetrics().also { textureView.display.getRealMetrics(it) }
        val screenSize = Size(metrics.widthPixels, metrics.heightPixels)
        val screenAspectRatio = Rational(metrics.widthPixels, metrics.heightPixels)

        // Create configuration object for the viewfinder use case
        val previewConfig = PreviewConfig.Builder().apply {
            setLensFacing(lensFacing)
            setTargetResolution(screenSize)
            // We also provide an aspect ratio in case the exact resolution is not available
            setTargetAspectRatio(screenAspectRatio)
            setTargetRotation(textureView.display.rotation)
        }.build()

        // Use the auto-fit preview builder to automatically handle size and orientation changes
        val preview = AutoFitPreviewBuilder.build(previewConfig, textureView)

        // Init config to capture or record
        handleSettingByType(screenAspectRatio)

        // Setup image analysis pipeline that computes average pixel luminance
        val analyzerConfig = ImageAnalysisConfig.Builder().apply {
            setLensFacing(lensFacing)
            // Use a worker thread for image analysis to prevent glitches
            val analyzerThread = HandlerThread("LuminosityAnalysis").apply { start() }
            setCallbackHandler(Handler(analyzerThread.looper))
            // In our analysis, we care more about the latest image than
            // analyzing *every* image
            setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
        }.build()

        // Build the image analysis use case and instantiate our analyzer
        val analyzerUseCase = ImageAnalysis(analyzerConfig).apply {
            analyzer = LuminosityAnalyzer()
        }

        if (type == SplashFragment.KEY_CAPTURE) {
            CameraX.bindToLifecycle(this, preview, imageCapture, analyzerUseCase)
        } else {
            // Do not but the analyzer into cycle to avoid crash app
            CameraX.bindToLifecycle(this, preview, videoCapture)
        }
//        preview.zoom(Rect(10, 10, 20, 20))
//        val focusRect = calculateTapArea(x, y, 1f, context)
//
//        preview.focus()
    }

    private fun handleSettingByType(screenAspectRatio: Rational) {
        if (type == SplashFragment.KEY_CAPTURE) {
            // Create configuration object for the image capture use case
            val imageCaptureConfig = ImageCaptureConfig.Builder()
                .apply {
                    setLensFacing(lensFacing)
                    setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                    // We don't set a resolution for image capture; instead, we
                    // select a capture mode which will infer the appropriate
                    // resolution based on aspect ration and requested mode
                    setTargetAspectRatio(screenAspectRatio)
                    setTargetRotation(textureView.display.rotation)
                }.build()
            // Build the image capture use case and attach button click listener
            imageCapture = ImageCapture(imageCaptureConfig)
        } else if (type == SplashFragment.KEY_RECORD) {
            // Create configuration object for the image capture use case
            val videoCaptureConfig = VideoCaptureConfig.Builder()
                .apply {
                    setLensFacing(lensFacing)
                    setVideoFrameRate(30)
                    setTargetAspectRatio(screenAspectRatio)
                    setTargetRotation(textureView.display.rotation)
                }.build()

            // Build the image capture use case and attach button click listener
            videoCapture = VideoCapture(videoCaptureConfig)
        }
    }

    private fun captureImage(file: File) {
        // Setup image capture metadata
        val metadata = ImageCapture.Metadata().apply {
            // Mirror image when using the front camera
            isReversedHorizontal = lensFacing == CameraX.LensFacing.FRONT
        }

        imageCapture?.takePicture(
            file,
            object : ImageCapture.OnImageSavedListener {
                override fun onError(
                    error: ImageCapture.UseCaseError,
                    message: String, exc: Throwable?
                ) {
                    val msg = "Photo capture failed: $message"
                    toast(context, msg)
                    Log.e("CameraXApp", msg)
                    exc?.printStackTrace()
                }

                override fun onImageSaved(file: File) {
                    val msg = "Photo capture succeeded: ${file.absolutePath}"
                    toast(context, msg)
                    Log.d("CameraXApp", msg)
                    handleWhenImageSaved(file)
                }
            }, metadata
        )
    }

    private fun recordVideo(file: File) {
        // Setup image capture metadata
        val metadata = VideoCapture.Metadata()
        // Start recording
//        if (isRecording)
        videoCapture?.startRecording(
            file,
            object : VideoCapture.OnVideoSavedListener {
                override fun onError(
                    useCaseError: VideoCapture.UseCaseError?,
                    message: String?,
                    cause: Throwable?
                ) {
                    val msg = "Video record failed: $message"
                    toast(context, msg)
                    Log.e("CameraXApp", msg)
                    cause?.printStackTrace()
                }

                override fun onVideoSaved(file: File?) {
                    val msg = "Video record succeeded: ${file?.absolutePath}"
                    toast(context, msg)
                    Log.d("CameraXApp", msg)
                    file?.let { handleWhenImageSaved(it) }
                }
            }, metadata
        )
        /* // Stop recording
         else videoCapture?.stopRecording()*/
    }

    private fun setGalleryThumbnail(file: File) {
        // Use a coroutine to perform thumbnail operations in background
        launch(coroutineContext) {
            // Create thumbnail for this photo
            val bitmap = when (ViewUtils.isPhotoType(file)) {
                true -> {
                    // Create thumbnail for this photo
                    ImageUtils.decodeBitmap(file)
                }
                false -> {
                    // Create thumbnail for this photo
                    ImageUtils.decodeVideo(requireContext(), file)
                }
                else -> {
                    return@launch
                }
            }

            bitmap?.let {
                // Crop the bitmap into a circle for the thumbnail
                val thumbnailBitmap = ImageUtils.cropCircularThumbnail(it)

                // Set the foreground drawable if we can, fallback using Glide
                // This must be done in the main thread, so use main thread's context
                withContext(Dispatchers.Main) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        btnGallery.foreground = BitmapDrawable(resources, thumbnailBitmap)
                    } else {
                        Glide.with(requireContext()).load(thumbnailBitmap).into(btnGallery)
                    }
                }
            }
        }
    }

    fun handleFocus(context: Context, x: Float, y: Float) {
        val focusRect = calculateTapArea(x, y, 1f, context)
    }

    private fun handleWhenImageSaved(photoFile: File) {
        // We can only change the foreground Drawable using API level 23+ API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Update the gallery thumbnail with latest picture taken
            setGalleryThumbnail(photoFile)
        }

        // Implicit broadcasts will be ignored for devices running API
        // level >= 24, so if you only target 24+ you can remove this statement
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            requireActivity().sendBroadcast(
                Intent(Camera.ACTION_NEW_PICTURE).setData(Uri.fromFile(photoFile))
            )
        }

        // If the folder selected is an external media directory, this is unnecessary
        // but otherwise other apps will not be able to access our images unless we
        // scan them using [MediaScannerConnection]
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(photoFile.extension)
        MediaScannerConnection.scanFile(context, arrayOf(photoFile.absolutePath), arrayOf(mimeType), null)
    }

    /** Use external media if it is available, our app's file directory otherwise */
    private fun getOutputDirectory(context: Context): File {
        val appContext = context.applicationContext
        val mediaDir = context.externalMediaDirs.firstOrNull()/*?.let {
            File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() }
        }*/
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else appContext.filesDir
    }

    private class LuminosityAnalyzer : ImageAnalysis.Analyzer {
        private var lastAnalyzedTimestamp = 0L

        /**
         * Helper extension function used to extract a byte array from an
         * image plane buffer
         */
        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        override fun analyze(image: ImageProxy, rotationDegrees: Int) {
            val currentTimestamp = System.currentTimeMillis()
            // Calculate the average luma no more often than every second
            if (currentTimestamp - lastAnalyzedTimestamp >=
                TimeUnit.SECONDS.toMillis(1)
            ) {
                // Since format in ImageAnalysis is YUV, image.planes[0]
                // contains the Y (luminance) plane
                val buffer = image.planes[0].buffer
                // Extract image data from callback object
                val data = buffer.toByteArray()
                // Convert the data into an array of pixel values
                val pixels = data.map { it.toInt() and 0xFF }
                // Compute average luminance for the image
                val luma = pixels.average()
                // Log the new luma value
                Log.d("CameraXApp", "Average luminosity: $luma")
                // Update timestamp of last analyzed frame
                lastAnalyzedTimestamp = currentTimestamp
            }
        }
    }
}
