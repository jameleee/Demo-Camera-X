package com.example.democamerax.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import java.io.File

/**
 * @author Dat Bui T. on 2019-05-17.
 */
class PhotoFragment : Fragment() {

    companion object {
        fun newInstance(file: File) = PhotoFragment().apply {
            filePath = file.absolutePath
        }
    }

    private var filePath = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ImageView(context)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Glide.with(this).load(File(filePath)).into(view as ImageView)
    }
}
