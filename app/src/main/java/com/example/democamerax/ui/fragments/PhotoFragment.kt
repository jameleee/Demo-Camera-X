package com.example.democamerax.ui.fragments

import android.graphics.Matrix
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.democamerax.utils.ImageUtils
import kotlinx.coroutines.*
import java.io.File
import kotlin.coroutines.CoroutineContext

/**
 * @author Dat Bui T. on 2019-05-17.
 */
class PhotoFragment : Fragment(), CoroutineScope {

    companion object {
        fun newInstance(file: File) = PhotoFragment().apply {
            filePath = file.absolutePath
        }
    }

    private var filePath = ""
    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ImageView(context)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        launch(coroutineContext) {
            val bitmap = ImageUtils.decodeBitmap(File(filePath))
            withContext(Dispatchers.Main) {
                Glide.with(this@PhotoFragment).load(bitmap).into(view as ImageView)
            }
        }
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }
}
