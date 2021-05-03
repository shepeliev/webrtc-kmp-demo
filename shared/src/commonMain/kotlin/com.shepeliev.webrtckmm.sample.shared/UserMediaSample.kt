package com.shepeliev.webrtckmm.sample.shared

import com.shepeliev.webrtckmp.MediaDevices
import com.shepeliev.webrtckmp.MediaStream
import com.shepeliev.webrtckmp.VideoTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

interface LocalVideoListener {
    fun onAddVideoTrack(track: VideoTrack)
    fun onRemoveVideoTrack(track: VideoTrack)
    fun onError(description: String?)
}

class UserMediaSample(private val listener: LocalVideoListener) : CoroutineScope by CommonMainScope() {

    private val tag = "UserMediaSample"
    private var mediaStream: MediaStream? = null

    fun startVideo() {
        stopVideo()

        launch {
            try {
                MediaDevices.getUserMedia(audio = false, video = true)
            } catch (e: Throwable) {
                Log.e(tag, "Error", e)
                listener.onError(e.message)
                null
            }?.also { stream ->
                mediaStream = stream
                listener.onAddVideoTrack(mediaStream!!.videoTrack()!!)
            }
        }
    }

    fun switchCamera() {
        launch {
            try {
                MediaDevices.switchCamera()
            } catch (e: Throwable) {
                listener.onError(e.message)
            }
        }
    }

    fun stopVideo() {
        val stream = mediaStream ?: return
        stream.videoTracks.forEach { track ->
            listener.onRemoveVideoTrack(track)
            track.stop()
        }
    }
}