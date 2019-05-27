package com.example.democamerax.ui.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.democamerax.R
import com.example.democamerax.utils.ViewUtils.toast
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.fragment_video.*
import java.io.File

/**
 * @author Dat Bui T. on 2019-05-17.
 */
class VideoFragment : Fragment() {

    companion object {
        fun newInstance(file: File) = VideoFragment().apply {
            filePath = file.absolutePath
        }
    }

    private var filePath = ""
    private lateinit var exoPlayer: ExoPlayer

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_video, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Init ExoPlayer
        exoPlayer = ExoPlayerFactory.newSimpleInstance(
            context,
            DefaultRenderersFactory(context),
            DefaultTrackSelector(),
            DefaultLoadControl()
        )

        val finalMediaSource = ExtractorMediaSource
            .Factory(DefaultDataSourceFactory(context, Util.getUserAgent(context, "ExoPlayerInfo")))
            .setExtractorsFactory(DefaultExtractorsFactory())
            .createMediaSource(Uri.parse(filePath))

        exoPlayer.prepare(finalMediaSource)

        exoPlayer.playWhenReady = true
        exoPlayer.repeatMode = Player.REPEAT_MODE_ALL

        exoPlayer.addListener(object : Player.EventListener {
            override fun onPlayerError(error: ExoPlaybackException?) {
                toast(context, "Something went wrong reason : ${error?.message}")
            }

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playWhenReady && playbackState == Player.STATE_READY) {
                } else if (playWhenReady) {
                } else {
                }
            }
        })

        // Play Video
        playerView.player = exoPlayer
    }

    override fun onDestroyView() {
        super.onDestroyView()
        exoPlayer.playWhenReady = false
        exoPlayer.stop()
    }
}