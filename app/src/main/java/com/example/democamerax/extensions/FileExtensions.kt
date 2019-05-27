package com.example.democamerax.extensions

import java.io.File

/**
 * @author Dat Bui T. on 2019-05-21.
 */
fun File.isPhotoType(): Boolean? {
    if (this.toString().toLowerCase().endsWith(".jpeg") || this.toString().toLowerCase().endsWith(".jpg")) {
        // Create thumbnail for this photo
        return true
    } else if (this.toString().toLowerCase().endsWith(".mp4")) {
        return false
    }
    return null
}
