package com.shepeliev.webrtckmm.sample

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.button.MaterialButton
import com.shepeliev.webrtckmp.sample.shared.UserMediaSample
import com.shepeliev.webrtckmp.sample.shared.LocalVideoListener
import com.shepeliev.webrtckmp.VideoTrack
import com.shepeliev.webrtckmp.WebRtcKmp
import com.shepeliev.webrtckmp.eglBase
import org.webrtc.SurfaceViewRenderer

class UserMediaSampleActivity : AppCompatActivity(R.layout.activity_user_media_sample),
    LocalVideoListener {

    private val tag = "UserMediaSampleActivity"

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            startVideo()
        }

    private val btnStartCamera by lazy { findViewById<MaterialButton>(R.id.btn_start_video) }
    private val btnSwitchCamera by lazy { findViewById<MaterialButton>(R.id.btn_switch_camera) }
    private val btnStopVideo by lazy { findViewById<MaterialButton>(R.id.btn_stop_video) }

    private val videoView by lazy {
        findViewById<SurfaceViewRenderer>(R.id.video).apply {
            init(WebRtcKmp.eglBase.eglBaseContext, null)
        }
    }

    private val userMedia by lazy { UserMediaSample(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        btnStartCamera.setOnClickListener {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }

        btnSwitchCamera.isEnabled = false
        btnSwitchCamera.setOnClickListener { userMedia.switchCamera() }

        btnStopVideo.setOnClickListener {
            btnSwitchCamera.isEnabled = false
            userMedia.stopVideo()
        }
    }

    override fun onDestroy() {
        userMedia.stopVideo()
        videoView.release()
        super.onDestroy()
    }

    private fun startVideo() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(tag, "Camera permission denied")
            return
        }

        btnSwitchCamera.isEnabled = false
        userMedia.startVideo()
    }

    override fun onAddVideoTrack(track: VideoTrack) {
        btnSwitchCamera.isEnabled = true
        btnStartCamera.isEnabled = false
        track.native.addSink(videoView)
    }

    override fun onRemoveVideoTrack(track: VideoTrack) {
        btnSwitchCamera.isEnabled = false
        btnStartCamera.isEnabled = true
        track.native.removeSink(videoView)
    }

    override fun onError(description: String?) {
        Log.e(tag, "Local video error: $description")
    }
}
