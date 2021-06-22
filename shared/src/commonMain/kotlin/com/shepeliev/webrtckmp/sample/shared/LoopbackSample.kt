package com.shepeliev.webrtckmp.sample.shared

import com.shepeliev.webrtckmp.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class LoopbackSample() {

    private val tag = "LoopbackSample"

    private var localStream: MediaStream? = null
    private var localPeerConnection: PeerConnection? = null
    private var remotePeerConnection: PeerConnection? = null

    private var scope: CoroutineScope? = null

    var onLocalStream: (MediaStream) -> Unit = {}
    var onRemoteStream: (MediaStream) -> Unit = {}
    var onRemoteVideoTrack: (VideoStreamTrack) -> Unit = {}
    var onCallEstablished: () -> Unit = {}
    var onCallEnded: () -> Unit = {}

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
                                onCallEstablished()
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
                                Log.d(
                                    tag,
                                    "Remote PC is not in Stable state. Collect local candidate"
                                )
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
                            Log.d(
                                tag,
                                "Remote PC on add track ${trackEvent.track}, streams: ${trackEvent.streams}"
                            )
                            trackEvent.streams.firstOrNull()?.also { onRemoteStream(it) }
                                ?: trackEvent.track?.takeIf { it.kind == MediaStreamTrackKind.Video }
                                    ?.also { onRemoteVideoTrack(it as VideoStreamTrack) }
                        }
                        .launchIn(this@launch)

                }

                localStream = WebRtc.mediaDevices.getUserMedia(audio = true, video = true)
                onLocalStream(localStream!!)

                localStream!!.tracks.forEach {
                    localPeerConnection?.addTrack(it, localStream!!)
                }

                val offer = localPeerConnection?.createOffer(OfferAnswerOptions())
                Log.d(tag, "$offer")
                localPeerConnection?.setLocalDescription(offer!!)

                remotePeerConnection?.setRemoteDescription(offer!!)
                val answer = remotePeerConnection?.createAnswer(OfferAnswerOptions())
                remotePeerConnection?.setLocalDescription(answer!!)

                localPeerConnection?.setRemoteDescription(answer!!)
            } catch (e: Throwable) {
                Log.e(tag, "Error", e)
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
        localStream?.release()
        localPeerConnection?.close()
        localPeerConnection = null
        remotePeerConnection?.close()
        remotePeerConnection = null
        localStream = null
        scope?.cancel()
        scope = null
        onCallEnded()
    }
}
