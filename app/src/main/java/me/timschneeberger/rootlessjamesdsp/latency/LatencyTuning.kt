package me.timschneeberger.rootlessjamesdsp.latency

/**
 * Buffer settings for the rootless capture loop.
 *
 * The constructor defaults match the legacy loop, [LOW_LATENCY] is used by the "Low-latency mode"
 * setting. Values are read in startRecording(), so a hard core reboot applies them.
 */
data class LatencyTuning(
    /** Frames per AudioRecord.read(); 0 = legacy (whole buffer, bufferSize / 2 frames). */
    val readFrames: Int = 0,
    /** AudioTrack.setBufferSizeInFrames() target; 0 = legacy (full track capacity). */
    val trackBufferFrames: Int = 0,
    /** Request AudioTrack.PERFORMANCE_MODE_LOW_LATENCY for the output track. */
    val lowLatencyTrack: Boolean = false,
    /** Drop a block when the output track holds more than this many frames; 0 = off. */
    val maxQueueFrames: Int = 0,
) {
    companion object {
        val LOW_LATENCY = LatencyTuning(
            readFrames = 960,
            trackBufferFrames = 3840,
            lowLatencyTrack = true,
            maxQueueFrames = 2880,
        )

        fun forMode(lowLatencyMode: Boolean) = if (lowLatencyMode) LOW_LATENCY else LatencyTuning()
    }
}
