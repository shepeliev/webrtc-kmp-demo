import com.shepeliev.webrtckmp.MediaDevices
import com.shepeliev.webrtckmp.sample.shared.LoopbackSample
import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLVideoElement

fun main() {
    val scope = MainScope()
    val localVideo = document.querySelector("#localVideo") as? HTMLVideoElement
    val remoteVideo = document.querySelector("#remoteVideo") as? HTMLVideoElement
    val startCallButton = document.querySelector("#startCall") as? HTMLButtonElement
    val stopCallButton = document.querySelector("#stopCall") as? HTMLButtonElement
    val showVideoButton = document.querySelector("#showVideo") as? HTMLButtonElement

    showVideoButton?.addEventListener("click", callback = {
        scope.launch {
            val stream = MediaDevices.getUserMedia(audio = true, video = true)
            localVideo?.srcObject = stream.js
        }
    })

    val loopbackSample = LoopbackSample()

    loopbackSample.onLocalStream = { stream ->
        localVideo?.srcObject = stream.js
    }
    loopbackSample.onRemoteStream = { stream ->
        remoteVideo?.srcObject = stream.js
    }

    startCallButton?.addEventListener("click", callback = {
        loopbackSample.startCall()
        startCallButton.disabled = true
        stopCallButton?.disabled = false
    })
    stopCallButton?.addEventListener("click", callback = {
        loopbackSample.stopCall()
        startCallButton?.disabled = false
        stopCallButton.disabled = true
    })
}
