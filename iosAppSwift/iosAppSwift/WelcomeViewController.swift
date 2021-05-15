//
//  ViewController.swift
//  iosAppSwift
//
//  Created by Aleksandr Shepeliev on 09.03.2021.
//

import UIKit
import shared
import WebRTC

class WelcomeViewController: UIViewController, LocalVideoListener {

    @IBOutlet weak var videoView: RTCMTLVideoView!
    
    private var localVideo: UserMediaSample?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        localVideo = UserMediaSample(listener: self)
    }
    
    @IBAction func startVideoPressed(_ sender: UIButton) {
        NSLog("Trying to start video...")
        localVideo?.startVideo()
    }
    
    @IBAction func stopVideoPressed(_ sender: UIButton) {
        NSLog("Stop video")
        localVideo?.stopVideo()
    }
        
    @IBAction func switchPressed(_ sender: UIButton) {
        NSLog("Switch camera")
        localVideo?.switchCamera()
    }
    
    @IBAction func backPressed(_ sender: UIButton) {
        localVideo?.stopVideo()
        dismiss(animated: true, completion: nil)
    }
    
    func onError(description: String?) {
        NSLog("Local video error: \(String(describing: description))")
    }

    func onAddVideoTrack(track: VideoStreamTrack) {
        track.addRenderer(renderer: videoView)
    }
    
    func onRemoveVideoTrack(track: VideoStreamTrack) {
        track.removeRenderer(renderer: videoView)
    }
}


