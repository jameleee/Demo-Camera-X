package com.example.democamerax.ui.fragments

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.example.democamerax.BuildConfig
import com.example.democamerax.R
import com.example.democamerax.extensions.isPhotoType
import com.example.democamerax.extensions.padWithDisplayCutout
import com.example.democamerax.extensions.showImmersive
import com.example.democamerax.ui.fragments.CameraFragment.Companion.EXTENSION_WHITELIST
import com.example.democamerax.ui.fragments.CameraFragment.Companion.KEY_ROOT_DIRECTORY
import kotlinx.android.synthetic.main.fragment_gallery.*
import java.io.File

/**
 * @author Dat Bui T. on 2019-05-16.
 */
class GalleryFragment : Fragment() {
    private lateinit var mediaList: MutableList<File>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Mark this as a retain fragment, so the lifecycle does not get restarted on config change
        retainInstance = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_gallery, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Walk through all files in the root directory
        // We reverse the order of the list to present the last photos first
        arguments?.let {
            mediaList = File(it.getString(KEY_ROOT_DIRECTORY)).listFiles { file ->
                EXTENSION_WHITELIST.contains(file.extension.toUpperCase())
            }.sorted().reversed().toMutableList()

            // Populate the ViewPager and implement a cache of two media items
            viewPagerPhoto.apply {
                offscreenPageLimit = 2
                adapter = MediaPagerAdapter(childFragmentManager)
            }
        }

        handleForDisplayCutout()
        initListener()
    }

    private fun handleForDisplayCutout() {
        // Make sure that the cutout "safe area" avoids the screen notch if any
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Use extension method to pad "inside" view containing UI using display cutout's bounds
            constraintSafeCutout.padWithDisplayCutout()
        }
    }

    private fun initListener() {
        // Handle back button press
        btnBack.setOnClickListener {
            fragmentManager?.popBackStack()
        }

        // Handle share button press
        btnShare.setOnClickListener {
            // Make sure that we have a file to share
            mediaList.getOrNull(viewPagerPhoto.currentItem)?.let { mediaFile ->
                val appContext = requireContext().applicationContext

                // Create a sharing intent
                val intent = Intent().apply {
                    // Infer media type from file extension
                    val mediaType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(mediaFile.extension)
                    // Get URI from our FileProvider implementation
                    val uri = FileProvider.getUriForFile(
                        appContext, BuildConfig.APPLICATION_ID + ".provider", mediaFile
                    )
                    // Set the appropriate intent extra, type, action and flags
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = mediaType
                    action = Intent.ACTION_SEND
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }

                // Launch the intent letting the user choose which app to share with
                startActivity(Intent.createChooser(intent, getString(R.string.share_hint)))
            }
        }

        // Handle delete button press
        btnDelete.setOnClickListener {
            val context = requireContext()
            AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog)
                .setTitle(getString(R.string.delete_title))
                .setMessage(getString(R.string.delete_dialog))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    mediaList.getOrNull(viewPagerPhoto.currentItem)?.let { mediaFile ->

                        // Delete current photo
                        mediaFile.delete()

                        // Notify our view pager
                        mediaList.removeAt(viewPagerPhoto.currentItem)
                        viewPagerPhoto.adapter?.notifyDataSetChanged()

                        // If all photos have been deleted, return to camera
                        if (mediaList.isEmpty()) {
                            fragmentManager?.popBackStack()
                        }
                    }
                }

                .setNegativeButton(android.R.string.no, null)
                .create().showImmersive()
        }
    }

    inner class MediaPagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
        override fun getCount(): Int = mediaList.size
        override fun getItem(position: Int): Fragment =
            when (mediaList[position].isPhotoType()) {
                false -> VideoFragment.newInstance(mediaList[position])
                else -> PhotoFragment.newInstance(mediaList[position])
            }

        override fun getItemPosition(obj: Any): Int = POSITION_NONE
    }
}
