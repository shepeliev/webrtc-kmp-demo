package com.shepeliev.webrtckmm.sample

import android.Manifest
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.shepeliev.webrtckmp.VideoStreamTrack
import com.shepeliev.webrtckmp.eglBaseContext
import com.shepeliev.webrtckmp.sample.shared.LoopbackSample
import org.webrtc.SurfaceViewRenderer

class LoopbackSampleActivity : AppCompatActivity(R.layout.activity_loopback_sample) {

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            startCall()
        }

    private val btnCall by lazy { findViewById<FloatingActionButton>(R.id.btn_call) }
    private val btnHangup by lazy { findViewById<FloatingActionButton>(R.id.btn_hangup) }
    private val localVideo by lazy { findViewById<SurfaceViewRenderer>(R.id.local_video) }
    private val remoteVideo by lazy { findViewById<SurfaceViewRenderer>(R.id.remote_video) }

    private val loopbackSample by lazy {
        LoopbackSample().apply {
            onLocalStream = { stream ->
                val videoTrack = stream.videoTracks.firstOrNull()
                videoTrack?.also { onLocalTrackAvailable(it) }
            }

            onRemoteVideoTrack = { onRemoteTrackAvailable(it) }
            onCallEstablished = { this@LoopbackSampleActivity.onCallEstablished() }
            onCallEnded = { this@LoopbackSampleActivity.onCallEnded() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        btnCall.setOnClickListener {
            btnCall.visibility = View.GONE
            requestPermissions.launch(permissions)
        }

        btnHangup.setOnClickListener {
            loopbackSample.stopCall()
            btnHangup.visibility = View.GONE
            btnCall.visibility = View.VISIBLE
            onCallEnded()
        }
    }

    private fun startCall() {
        loopbackSample.startCall()
    }

    override fun onDestroy() {
        loopbackSample.stopCall()
        localVideo.release()
        remoteVideo.release()
        super.onDestroy()
    }

    private fun onLocalTrackAvailable(track: VideoStreamTrack) {
        with(localVideo) {
            init(eglBaseContext, null)
            track.addSink(this)
            visibility = View.VISIBLE
        }
    }

    private fun onRemoteTrackAvailable(track: VideoStreamTrack) {
        with(remoteVideo) {
            init(eglBaseContext, null)
            track.addSink(this)
            visibility = View.VISIBLE
        }
    }

    private fun onCallEstablished() {
        btnHangup.visibility = View.VISIBLE
    }

    private fun onCallEnded() {
        with(localVideo) {
            visibility = View.GONE
            release()
        }
        with(remoteVideo) {
            visibility = View.GONE
            release()
        }
    }

    companion object {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
    }
}
