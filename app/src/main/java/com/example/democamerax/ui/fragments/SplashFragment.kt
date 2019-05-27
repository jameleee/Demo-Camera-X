package com.example.democamerax.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.example.democamerax.R
import kotlinx.android.synthetic.main.fragment_splash.*

/**
 * @author Dat Bui T. on 2019-05-16.
 */
class SplashFragment : Fragment() {

    companion object {
        const val KEY_CAPTURE = "capture"
        const val KEY_RECORD = "record"
        const val KEY_TYPE = "captureType"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnCapture.setOnClickListener {
            val arguments = Bundle().apply {
                putString(KEY_TYPE, KEY_CAPTURE)
            }
            Navigation.findNavController(requireActivity(), R.id.frContainer)
                .navigate(R.id.action_splashFragment_to_cameraFragment, arguments)
        }

        btnRecord.setOnClickListener {
            val arguments = Bundle().apply {
                putString(KEY_TYPE, KEY_RECORD)
            }
            Navigation.findNavController(requireActivity(), R.id.frContainer)
                .navigate(R.id.action_splashFragment_to_cameraFragment, arguments)
        }
    }
}
