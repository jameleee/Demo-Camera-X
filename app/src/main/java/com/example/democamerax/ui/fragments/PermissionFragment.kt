package com.example.democamerax.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import com.example.democamerax.R
import com.example.democamerax.utils.ViewUtils.toast

/**
 * @author Dat Bui T. on 2019-05-16.
 */
class PermissionFragment : Fragment() {
    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 10
        private val PERMISSIONS_REQUIRED = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }

    private val navOptions = NavOptions.Builder().setPopUpTo(R.id.permissionFragment, true).build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasPermissions()) {
            // Request camera-related permissions
            requestPermissions(PERMISSIONS_REQUIRED, PERMISSIONS_REQUEST_CODE)
        } else {
            // If permissions have already been granted, proceed
            Navigation.findNavController(requireActivity(), R.id.frContainer)
                .navigate(
                    R.id.action_permissionFragment_to_splashFragment, null,
                    navOptions
                )
        }
    }

    private fun hasPermissions(): Boolean {
        for (permission in PERMISSIONS_REQUIRED) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Take the user to the success fragment when permission is granted
                toast(context, "Permission request granted")

                Navigation.findNavController(requireActivity(), R.id.frContainer)
                    .navigate(
                        R.id.action_permissionFragment_to_splashFragment, null,
                        navOptions
                    )
            } else {
                toast(context, "Permission request denied")
            }
        }
    }
}
