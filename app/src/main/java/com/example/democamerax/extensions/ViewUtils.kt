package com.example.democamerax.extensions

import android.content.Context
import android.widget.Toast
import java.io.File

/**
 * @author Dat Bui T. on 2019-05-16.
 */
object ViewUtils {
    fun isPhotoType(file: File): Boolean? {
        if (file.toString().toLowerCase().endsWith(".jpeg") || file.toString().toLowerCase().endsWith(".jpg")) {
            // Create thumbnail for this photo
            return true
        } else if (file.toString().toLowerCase().endsWith(".mp4")) {
            return false
        }
        return null
    }

    fun toast(context: Context?, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}
