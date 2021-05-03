package com.shepeliev.webrtckmp.sample.shared

import com.shepeliev.webrtckmm.sample.shared.CommonMainScope
import com.shepeliev.webrtckmm.sample.shared.Log
import com.shepeliev.webrtckmp.IceCandidate
import com.shepeliev.webrtckmp.IceConnectionState
import com.shepeliev.webrtckmp.MediaDevices
import com.shepeliev.webrtckmp.MediaStream
import com.shepeliev.webrtckmp.MediaStreamTrack
import com.shepeliev.webrtckmp.PeerConnection
import com.shepeliev.webrtckmp.RtcConfiguration
import com.shepeliev.webrtckmp.SdpSemantics
import com.shepeliev.webrtckmp.SignalingState
import com.shepeliev.webrtckmp.VideoTrack
import com.shepeliev.webrtckmp.create
import com.shepeliev.webrtckmp.mediaConstraints
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

interface LoopbackSampleListener {
    fun onLocalTrackAvailable(track: VideoTrack)
    fun onRemoteTrackAvailable(track: VideoTrack)
    fun onCallEstablished()
    fun onCallEnded()
    fun onError(description: String)
}

class LoopbackSample(private val listener: LoopbackSampleListener) :
    CoroutineScope by CommonMainScope() {

    private val tag = "LoopbackSample"

    private var userMedia: MediaStream? = null
    private var localPeerConnection: PeerConnection? = null
    private var remotePeerConnection: PeerConnection? = null

    fun startCall() = launch {
        try {
            val config = RtcConfiguration(
                disableIpv6 = true,
                disableIpv6OnWifi = true,
                sdpSemantics = SdpSemantics.UnifiedPlan,
            )
            val loopbackConstraints = mediaConstraints {
                optional { "DtlsSrtpKeyAgreement" to "false" }
            }

            localPeerConnection = PeerConnection.create(config, loopbackConstraints)
            remotePeerConnection = PeerConnection.create(config, loopbackConstraints)

            val localIceCandidates = mutableListOf<IceCandidate>()
            val remoteIceCandidates = mutableListOf<IceCandidate>()

            with(localPeerConnection!!.events) {
                onSignalingState
                    .onEach {
                        Log.d(tag, "Local PC signaling state $it")
                        if (it == SignalingState.Stable) {
                            Log.d(tag, "Drain remote candidates into local PC")
                            drainCandidates(remoteIceCandidates, localPeerConnection!!)
                        }
                    }
                    .launchIn(this@LoopbackSample)
                onIceGatheringState
                    .onEach { Log.d(tag, "Local PC ICE gathering state $it") }
                    .launchIn(this@LoopbackSample)
                onIceConnectionState
                    .onEach {
                        Log.d(tag, "Local PC ICE connection state $it")
                        if (it == IceConnectionState.Connected) {
                            listener.onCallEstablished()
                        }
                        if (it == IceConnectionState.Disconnected) {
                            stopCall()
                        }
                    }
                    .launchIn(this@LoopbackSample)
                onConnectionState
                    .onEach { Log.d(tag, "Local PC PeerConnection state $it") }
                    .launchIn(this@LoopbackSample)
                onIceCandidate
                    .onEach {
                        Log.d(tag, "Local PC ICE candidate $it")
                        if (remotePeerConnection?.signalingState == SignalingState.Stable) {
                            Log.d(tag, "Remote PC is in Stable state. Add Ice candidate")
                            remotePeerConnection?.addIceCandidate(it)
                        } else {
                            Log.d(tag, "Remote PC is not in Stable state. Collect local candidate")
                            localIceCandidates += it
                        }
                    }
                    .launchIn(this@LoopbackSample)
            }

            with(remotePeerConnection!!.events) {
                onSignalingState
                    .onEach {
                        Log.d(tag, "Remote PC signaling state $it")
                        if (it == SignalingState.Stable) {
                            Log.d(tag, "Drain local candidates into remote PC")
                            drainCandidates(localIceCandidates, remotePeerConnection!!)
                        }
                    }
                    .launchIn(this@LoopbackSample)
                onIceGatheringState
                    .onEach { Log.d(tag, "Remote PC ICE gathering state $it") }
                    .launchIn(this@LoopbackSample)
                onIceConnectionState
                    .onEach { Log.d(tag, "Remote PC ICE connection state $it") }
                    .launchIn(this@LoopbackSample)
                onConnectionState
                    .onEach { Log.d(tag, "Remote PC PeerConnection state $it") }
                    .launchIn(this@LoopbackSample)
                onIceCandidate
                    .onEach {
                        Log.d(tag, "Remote PC ICE candidate $it")
                        if (localPeerConnection?.signalingState == SignalingState.Stable) {
                            Log.d(tag, "Local PC is in Stable state. Add Ice candidate")
                            localPeerConnection?.addIceCandidate(it)
                        } else {
                            Log.d(tag, "Local PC is not in Stable state. Collect Ice candidate")
                            remoteIceCandidates += it
                        }
                    }
                    .launchIn(this@LoopbackSample)
                onAddTrack
                    .onEach { (receiver, _) ->
                        Log.d(tag, "Remote PC on add track ${receiver.track}")
                        if (receiver.track?.kind == MediaStreamTrack.VIDEO_TRACK_KIND) {
                            val track = (receiver.track as VideoTrack)
                            listener.onRemoteTrackAvailable(track)
                        }
                    }
                    .launchIn(this@LoopbackSample)
            }

            userMedia = MediaDevices.getUserMedia(audio = true, video = true)
            listener.onLocalTrackAvailable(userMedia!!.videoTrack()!!)

            userMedia!!.audioTracks.forEach {
                localPeerConnection?.addTrack(it, listOf(userMedia!!.id))
            }
            userMedia!!.videoTracks.forEach {
                localPeerConnection?.addTrack(it, listOf(userMedia!!.id))
            }

            val offerConstraints = mediaConstraints {
                mandatory { "OfferToReceiveAudio" to "true" }
                mandatory { "OfferToReceiveVideo" to "true" }
            }
            val offer = localPeerConnection?.createOffer(offerConstraints)
            Log.d(tag, "$offer")
            localPeerConnection?.setLocalDescription(offer!!)

            remotePeerConnection?.setRemoteDescription(offer!!)
            val answer = remotePeerConnection?.createAnswer(mediaConstraints())
            remotePeerConnection?.setLocalDescription(answer!!)

            localPeerConnection?.setRemoteDescription(answer!!)
        } catch (e: Throwable) {
            Log.e(tag, "Error", e)
            listener.onError("${e.message}")
            stopCall()
            return@launch
        }
    }

    private fun drainCandidates(
        candidates: MutableList<IceCandidate>,
        peerConnection: PeerConnection
    ) {
        if (candidates.isEmpty()) return
        candidates.forEach {
            Log.d(tag, "Drain candidate into PC")
            peerConnection.addIceCandidate(it)
        }
        candidates.clear()
    }

    fun stopCall() {
        localPeerConnection?.close()
        localPeerConnection = null
        remotePeerConnection?.close()
        remotePeerConnection = null
        listener.onCallEnded()
    }
}