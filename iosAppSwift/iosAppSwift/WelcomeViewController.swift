//
//  ViewController.swift
//  iosAppSwift
//
//  Created by Aleksandr Shepeliev on 09.03.2021.
//

import UIKit
import shared
import WebRTC

class WelcomeViewController: UIViewController {

    @IBOutlet weak var videoView: RTCMTLVideoView!
    private let mediaDevices = MediaDevicesCompanion()
    private var stream: MediaStream?
    
    @IBAction func startVideoPressed(_ sender: UIButton) {
        NSLog("Trying to start video...")
        mediaDevices.getUserMedia(
            audio: false,
            video: true,
            completionHandler: { stream, error in
                if let videoTrack = stream?.videoTracks.first {
                    self.stream = stream
                    videoTrack.addRenderer(renderer: self.videoView)
                } else if let errorText = error?.localizedDescription {
                    NSLog("Local video error: \(errorText)")
                } else {
                    NSLog("Something went wrong!")
                }
            }
        )
    }
    
    @IBAction func stopVideoPressed(_ sender: UIButton) {
        stopVideo()
    }
    
    private func stopVideo() {
        NSLog("Stop video")
        stream?.videoTracks.first?.removeRenderer(renderer: videoView)
        stream?.release()
    }
        
    @IBAction func switchPressed(_ sender: UIButton) {
        NSLog("Switch camera")
        stream?.videoTracks.forEach { $0.switchCamera(deviceId: nil, completionHandler: {_, _ in }) }
    }
    
    @IBAction func backPressed(_ sender: UIButton) {
        stopVideo()
        dismiss(animated: true, completion: nil)
    }
}


