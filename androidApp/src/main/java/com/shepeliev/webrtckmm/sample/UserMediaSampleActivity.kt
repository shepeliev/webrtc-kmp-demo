package com.shepeliev.webrtckmm.sample

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.shepeliev.webrtckmp.MediaDevices
import com.shepeliev.webrtckmp.MediaStream
import com.shepeliev.webrtckmp.VideoStreamTrack
import com.shepeliev.webrtckmp.eglBaseContext
import org.webrtc.SurfaceViewRenderer

class UserMediaSampleActivity : AppCompatActivity(R.layout.activity_user_media_sample) {

    private val tag = "UserMediaSampleActivity"

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            startVideo()
        }

    private val btnStartCamera by lazy { findViewById<MaterialButton>(R.id.btn_start_video) }
    private val btnSwitchCamera by lazy { findViewById<MaterialButton>(R.id.btn_switch_camera) }
    private val btnStopVideo by lazy { findViewById<MaterialButton>(R.id.btn_stop_video) }

    private val videoView by lazy { findViewById<SurfaceViewRenderer>(R.id.video) }

    private var localMediaStream: MediaStream? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        btnStartCamera.setOnClickListener {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }

        btnSwitchCamera.isEnabled = false
        btnSwitchCamera.setOnClickListener {
            localMediaStream?.videoTracks?.forEach { it.switchCameraAsync() }
        }

        btnStopVideo.setOnClickListener {
            btnSwitchCamera.isEnabled = false
            stopVideo()
        }
    }

    private fun VideoStreamTrack.switchCameraAsync() = lifecycleScope.launchWhenResumed {
        switchCamera()
    }

    override fun onDestroy() {
        stopVideo()
        videoView.release()
        super.onDestroy()
    }

    private fun stopVideo() {
        videoView.release()
        localMediaStream?.release()
        localMediaStream = null
        btnSwitchCamera.isEnabled = false
        btnStartCamera.isEnabled = true
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

        startVideoAsync()
    }

    private fun startVideoAsync() = lifecycleScope.launchWhenStarted {
        try {
            localMediaStream = MediaDevices.getUserMedia(video = true)
            btnSwitchCamera.isEnabled = true
            btnStartCamera.isEnabled = false
            localMediaStream
                ?.videoTracks
                ?.firstOrNull()
                ?.also {
                    videoView?.init(eglBaseContext, null)
                    it.addSink(videoView)
                }
        } catch (e: Throwable) {
            Log.e(tag, "Local video error", e)
        }
    }
}
