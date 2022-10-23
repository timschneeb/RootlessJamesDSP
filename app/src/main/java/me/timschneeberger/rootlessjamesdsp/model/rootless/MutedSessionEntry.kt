package me.timschneeberger.rootlessjamesdsp.model.rootless

import android.media.audiofx.AudioEffect

data class MutedSessionEntry(
    var audioSession: AudioSessionEntry,
    var audioMuteEffect: AudioEffect?
)