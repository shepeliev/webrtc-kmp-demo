//
//  LoopbackViewController.swift
//  iosAppSwift
//
//  Created by Aleksandr Shepeliev on 08.04.2021.
//

import UIKit
import WebRTC
import shared

class LoopbackViewController: UIViewController {

    @IBOutlet weak var localVideoView: RTCMTLVideoView!
    @IBOutlet weak var remoteVideoView: RTCMTLVideoView!
    
    private let loopbackSample = LoopbackSample()
    
    private var callState: CallState = .idle
    
    override func viewDidLoad() {
        super.viewDidLoad()
        localVideoView.isHidden = true
        remoteVideoView.isHidden = true
        
        loopbackSample.onLocalStream = { stream in
            guard let track = stream.videoTracks.first else {
                return
            }
            self.onLocalTrackAvailable(track: track)
        }
        
        loopbackSample.onRemoteStream = { stream in
            guard let track = stream.videoTracks.first else {
                return
            }
            self.onRemoteTrackAvailable(track: track)
            self.onCallEstablished()
        }
        
        loopbackSample.onRemoteVideoTrack = { track in
            self.onRemoteTrackAvailable(track: track)
            self.onCallEstablished()
        }
    }
    
    @IBAction func callHangupPressed(_ sender: UIButton) {
        if callState == .idle {
            loopbackSample.startCall()
            callState = .calling
        }
        if callState == .inCall {
            loopbackSample.stopCall()
            onCallEnded()
        }
    }
    
    @IBAction func backPressed(_ sender: UIButton) {
        loopbackSample.stopCall()
        onCallEnded()
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
    
    func onError(description_ description: String) {
        NSLog("Error: \(description)")
        callState = .idle
    }
    
    func onLocalTrackAvailable(track: VideoStreamTrack) {
        addRenderer(track, renderer: localVideoView)
    }
    
    func onRemoteTrackAvailable(track: VideoStreamTrack) {
        addRenderer(track, renderer: remoteVideoView)
    }
    
    private func addRenderer(_ track: VideoStreamTrack, renderer: RTCMTLVideoView) {
        renderer.isHidden = false
        track.addRenderer(renderer: renderer)
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
