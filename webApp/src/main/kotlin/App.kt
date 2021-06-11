import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLVideoElement
import org.w3c.dom.mediacapture.MediaStreamConstraints

fun main() {
    document.querySelector("#showVideo")?.addEventListener("click", callback = {
        // TODO implement getting user media by shared KMM lib
        val constraints = MediaStreamConstraints(video = true)
        window.navigator.getUserMedia(
            constraints, successCallback = {
                val video = document.querySelector("#gum-local") as? HTMLVideoElement
                video?.srcObject = it
            },
            errorCallback = {
                console.error(it)
            })
    })
}
