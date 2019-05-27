package com.example.democamerax.utils

import android.content.Context
import android.widget.Toast

/**
 * @author Dat Bui T. on 2019-05-16.
 */
object ViewUtils {

    fun toast(context: Context?, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}
