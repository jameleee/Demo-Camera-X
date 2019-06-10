package com.example.democamerax.ui.camera

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.democamerax.utils.AngleUtil
import java.io.File

/**
 * @author Dat Bui T. on 2019-05-28.
 */
class CameraViewModel {

    private var angle = 0
    private var rotation = 0
    private var endRotation = 0
    private var startRotation = 0
    private var sm: SensorManager? = null
    private var rotationListener: RotationListener? = null

    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (Sensor.TYPE_ACCELEROMETER != event.sensor.type) {
                return
            }
            val values = event.values
            angle = AngleUtil.getSensorAngle(values[0], values[1])
            rotationAnimation()
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    //Switch camera icon to rotate with the phone angle
    @SuppressLint("ObjectAnimatorBinding")
    private fun rotationAnimation() {
        if (rotation != angle) {

            when (rotation) {
                0 -> {
                    startRotation = 0
                    when (angle) {
                        90 -> endRotation = -90
                        270 -> endRotation = 90
                    }
                }
                90 -> {
                    startRotation = -90
                    when (angle) {
                        0 -> endRotation = 0
                        180 -> endRotation = -180
                    }
                }
                180 -> {
                    startRotation = 180
                    when (angle) {
                        90 -> endRotation = 270
                        270 -> endRotation = 90
                    }
                }
                270 -> {
                    startRotation = 90
                    when (angle) {
                        0 -> endRotation = 0
                        180 -> endRotation = 180
                    }
                }
            }
            /* val animCamera =
                 ObjectAnimator.ofFloat(mSwitchView, "rotation", startRotation.toFloat(), endRotation.toFloat())
             val animFlash =
                 ObjectAnimator.ofFloat(mFlashLamp, "rotation", startRotation.toFloat(), endRotation.toFloat())
             val set = AnimatorSet()
             set.playTogether(animCamera, animFlash)
             set.duration = 500
             set.start()*/
            setRotationValue(startRotation, endRotation)

            rotationListener?.onRotation(startRotation, endRotation)

            rotation = angle
        }
    }

    private fun setRotationValue(startRotation: Int, endRotation: Int) {
        this.startRotation = startRotation
        this.endRotation = endRotation
    }

    internal fun setRotationListener(rotationListener: RotationListener) {
        this.rotationListener = rotationListener
    }

    /** Use external media if it is available, our app's file directory otherwise */
    internal fun getOutputDirectory(context: Context): File {
        val appContext = context.applicationContext
        val mediaDir = context.externalMediaDirs.firstOrNull()/*?.let {
            File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() }
        }*/
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else appContext.filesDir
    }

    /**
     * This func use to register sensor manager
     * @param context
     */
    internal fun registerSensorManager(context: Context) {
        if (sm == null) {
            sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        }
        sm?.registerListener(
            sensorEventListener,
            sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    /**
     * This func use to unregister sensor manager
     * @param context
     */
    internal fun unregisterSensorManager(context: Context) {
        if (sm == null) {
            sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        }
        sm?.unregisterListener(sensorEventListener)
    }

    internal fun getRotationValue() = endRotation
}
