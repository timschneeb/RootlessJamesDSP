package me.timschneeberger.rootlessjamesdsp

import android.accessibilityservice.AccessibilityService
import android.media.AudioManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class PlaybackAccessibilityService : AccessibilityService() {

    private var audioManager: AudioManager? = null

    override fun onServiceConnected() {
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        super.onServiceConnected()
    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(accessibilityEvent: AccessibilityEvent) {
        Log.d("PlaybackAccessibilityService", accessibilityEvent.toString())
    }
}