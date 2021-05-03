//
//  LoopbackViewController.swift
//  iosAppSwift
//
//  Created by Aleksandr Shepeliev on 08.04.2021.
//

import UIKit
import WebRTC
import shared

class LoopbackViewController: UIViewController, LoopbackSampleListener {

    @IBOutlet weak var localVideoView: RTCMTLVideoView!
    @IBOutlet weak var remoteVideoView: RTCMTLVideoView!
    
    private var loopbackSample: LoopbackSample?
    
    private var callState: CallState = .idle
    
    override func viewDidLoad() {
        super.viewDidLoad()
        localVideoView.isHidden = true
        remoteVideoView.isHidden = true
        loopbackSample =  LoopbackSample(listener: self)
    }
    
    @IBAction func callHangupPressed(_ sender: UIButton) {
        if callState == .idle {
            loopbackSample?.startCall()
            callState = .calling
        }
        if callState == .inCall {
            loopbackSample?.stopCall()
        }
    }
    
    @IBAction func backPressed(_ sender: UIButton) {
        loopbackSample?.stopCall()
        dismiss(animated: true, completion: nil)
    }
    
    func onCallEnded() {
        callState = .idle
        localVideoView.isHidden = true
        remoteVideoView.isHidden = true
    }
    
    func onCallEstablished() {
        callState = .inCall
    }
    
    func onError(description: String) {
        NSLog("Error: \(description)")
        callState = .idle
    }
    
    func onLocalTrackAvailable(track: VideoTrack) {
        addRenderer(track: track.native, renderer: localVideoView)
    }
    
    func onRemoteTrackAvailable(track: VideoTrack) {
        addRenderer(track: track.native, renderer: remoteVideoView)
    }
    
    private func addRenderer(track: RTCMediaStreamTrack, renderer: RTCMTLVideoView) {
        renderer.isHidden = false
        let rtcVideoTrack = track as! RTCVideoTrack
        rtcVideoTrack.add(renderer)
    }
    
    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        // Get the new view controller using segue.destination.
        // Pass the selected object to the new view controller.
    }
    */

}


enum CallState {
    case idle
    case calling
    case inCall
}
