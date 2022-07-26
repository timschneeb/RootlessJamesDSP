package me.timschneeberger.rootlessjamesdsp.model

import android.media.audiofx.DynamicsProcessing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

data class MutedSessionEntry(
    var audioSession: AudioSessionEntry,
    var dynamicsProcessing: DynamicsProcessing?
)