package com.shepeliev.webrtckmp.sample.shared

import com.shepeliev.webrtckmp.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

interface LoopbackSampleListener {
    fun onLocalTrackAvailable(track: VideoStreamTrack)
    fun onRemoteTrackAvailable(track: VideoStreamTrack)
    fun onCallEstablished()
    fun onCallEnded()
    fun onError(description: String)
}

class LoopbackSample(private val listener: LoopbackSampleListener) {

    private val tag = "LoopbackSample"

    private var localStream: MediaStream? = null
    private var localPeerConnection: PeerConnection? = null
    private var remotePeerConnection: PeerConnection? = null

    private var scope: CoroutineScope? = null

    fun startCall() {
        if (scope == null) {
            scope = MainScope()
        }

        scope?.launch {
            try {
                val config = RtcConfiguration()
                localPeerConnection = PeerConnection(config)
                remotePeerConnection = PeerConnection(config)

                val localIceCandidates = mutableListOf<IceCandidate>()
                val remoteIceCandidates = mutableListOf<IceCandidate>()

                with(localPeerConnection!!) {
                    onSignalingStateChange
                        .onEach {
                            Log.d(tag, "Local PC signaling state $it")
                            if (it == SignalingState.Stable) {
                                Log.d(tag, "Drain remote candidates into local PC")
                                drainCandidates(remoteIceCandidates, localPeerConnection!!)
                            }
                        }
                        .launchIn(this@launch)

                    onIceGatheringState
                        .onEach { Log.d(tag, "Local PC ICE gathering state $it") }
                        .launchIn(this@launch)

                    onIceConnectionStateChange
                        .onEach {
                            Log.d(tag, "Local PC ICE connection state $it")
                            if (it == IceConnectionState.Connected) {
                                listener.onCallEstablished()
                            }
                            if (it == IceConnectionState.Disconnected) {
                                stopCall()
                            }
                        }
                        .launchIn(this@launch)

                    onConnectionStateChange
                        .onEach { Log.d(tag, "Local PC PeerConnection state $it") }
                        .launchIn(this@launch)

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
                        .launchIn(this@launch)
                }

                with(remotePeerConnection!!) {
                    onSignalingStateChange
                        .onEach {
                            Log.d(tag, "Remote PC signaling state $it")
                            if (it == SignalingState.Stable) {
                                Log.d(tag, "Drain local candidates into remote PC")
                                drainCandidates(localIceCandidates, remotePeerConnection!!)
                            }
                        }
                        .launchIn(this@launch)

                    onIceGatheringState
                        .onEach { Log.d(tag, "Remote PC ICE gathering state $it") }
                        .launchIn(this@launch)

                    onIceConnectionStateChange
                        .onEach { Log.d(tag, "Remote PC ICE connection state $it") }
                        .launchIn(this@launch)

                    onConnectionStateChange
                        .onEach { Log.d(tag, "Remote PC PeerConnection state $it") }
                        .launchIn(this@launch)

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
                        .launchIn(this@launch)

                    onTrack
                        .onEach { trackEvent ->
                            Log.d(tag, "Remote PC on add track ${trackEvent.track}")
                            if (trackEvent.track?.kind == MediaStreamTrackKind.Video) {
                                val track = (trackEvent.track as VideoStreamTrack)
                                listener.onRemoteTrackAvailable(track)
                            }
                        }
                        .launchIn(this@launch)

                }

                localStream = MediaDevices.getUserMedia(audio = true, video = true)
                listener.onLocalTrackAvailable(localStream!!.videoTracks.first())

                localStream!!.audioTracks.forEach {
                    localPeerConnection?.addTrack(it, listOf(localStream!!.id))
                }
                localStream!!.videoTracks.forEach {
                    localPeerConnection?.addTrack(it, listOf(localStream!!.id))
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
        localStream?.tracks?.forEach { it.stop() }
        localStream = null
        scope?.cancel()
        scope = null
    }
}
