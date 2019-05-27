package com.example.democamerax

import android.app.Application
import leakcanary.LeakSentry

/**
 * @author Dat Bui T. on 2019-05-27.
 */
class VideoEditorApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        LeakSentry.config = LeakSentry.config.copy(watchFragmentViews = false)
    }
}
