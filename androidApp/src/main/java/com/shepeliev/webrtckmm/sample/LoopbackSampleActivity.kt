package com.shepeliev.webrtckmm.sample

import android.Manifest
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.shepeliev.webrtckmp.VideoStreamTrack
import com.shepeliev.webrtckmp.WebRtcKmp
import com.shepeliev.webrtckmp.eglBase
import com.shepeliev.webrtckmp.sample.shared.LoopbackSample
import com.shepeliev.webrtckmp.sample.shared.LoopbackSampleListener
import org.webrtc.SurfaceViewRenderer

class LoopbackSampleActivity : AppCompatActivity(R.layout.activity_loopback_sample),
    LoopbackSampleListener {

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            startCall()
        }

    private val btnCall by lazy { findViewById<FloatingActionButton>(R.id.btn_call) }
    private val btnHangup by lazy { findViewById<FloatingActionButton>(R.id.btn_hangup) }

    private val localVideo by lazy {
        findViewById<SurfaceViewRenderer>(R.id.local_video).apply {
            init(WebRtcKmp.eglBase.eglBaseContext, null)
        }
    }

    private val remoteVideo by lazy {
        findViewById<SurfaceViewRenderer>(R.id.remote_video).apply {
            init(WebRtcKmp.eglBase.eglBaseContext, null)
        }
    }

    private val loopbackSample by lazy { LoopbackSample(this) }

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

    override fun onLocalTrackAvailable(track: VideoStreamTrack) {
        localVideo.visibility = View.VISIBLE
        track.addSink(localVideo)
    }

    override fun onRemoteTrackAvailable(track: VideoStreamTrack) {
        remoteVideo.visibility = View.VISIBLE
        track.addSink(remoteVideo)
    }

    override fun onCallEstablished() {
        btnHangup.visibility = View.VISIBLE
    }

    override fun onError(description: String) {
        Toast.makeText(this, "Error: $description", Toast.LENGTH_LONG).show()
        btnHangup.visibility = View.GONE
        btnCall.visibility = View.VISIBLE
    }

    override fun onCallEnded() {
        localVideo.visibility = View.GONE
        remoteVideo.visibility = View.GONE
    }

    companion object {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
    }
}
