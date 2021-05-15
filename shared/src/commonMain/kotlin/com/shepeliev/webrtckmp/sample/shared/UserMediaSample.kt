package com.shepeliev.webrtckmp.sample.shared

import com.shepeliev.webrtckmp.MediaDevices
import com.shepeliev.webrtckmp.MediaStream
import com.shepeliev.webrtckmp.MediaStreamTrackKind
import com.shepeliev.webrtckmp.VideoStreamTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

interface LocalVideoListener {
    fun onAddVideoTrack(track: VideoStreamTrack)
    fun onRemoveVideoTrack(track: VideoStreamTrack)
    fun onError(description: String?)
}

class UserMediaSample(private val listener: LocalVideoListener) : CoroutineScope by MainScope() {

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
                listener.onAddVideoTrack(mediaStream!!.videoTracks.first())
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
        stream.tracks.forEach { track ->
            if (track.kind == MediaStreamTrackKind.Video) {
                listener.onRemoveVideoTrack(track as VideoStreamTrack)
            }
            track.stop()
        }
        mediaStream = null
    }
}
