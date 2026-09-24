package me.timschneeberger.rootlessjamesdsp.analysis

data class TrackIdentity(
    val packageName: String? = null,
    val mediaId: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val durationMs: Long? = null
)

enum class TrackBoundaryReason {
    MetadataChanged,
    PlaybackRestarted,
    SessionChanged,
    SilenceThenResume,
    SpectralNovelty,
    ManualReset
}

data class TrackBoundaryEvent(
    val timestampNs: Long,
    val identity: TrackIdentity? = null,
    val confidence: Float,
    val reason: TrackBoundaryReason
)

/**
 * [toleranceDb] is the normal range around the reference. Songs differ far more at the ends
 * of the spectrum than in the midrange, so the ends get a wider range.
 */
enum class TonalBand(val label: String, val shortLabel: String, val centerHz: Float, val toleranceDb: Float) {
    Sub("sub", "Sub", 40f, 6f),
    Bass("bass", "Bass", 100f, 4f),
    LowMid("low-mid", "Lo-mid", 300f, 3f),
    Mid("mid", "Mid", 1_000f, 3f),
    Presence("presence", "Pres", 3_000f, 3f),
    Treble("treble", "Treble", 7_000f, 4f),
    Air("air", "Air", 14_000f, 6f)
}

data class TonalityFrame(
    val timestampNs: Long,
    val track: TrackIdentity?,
    val validAudioMs: Long,
    /** Seconds of audio in the song average. */
    val analyzedSeconds: Float,
    val referenceName: String,
    val referenceKind: TonalityReferenceKind,
    val referenceIsVerified: Boolean,
    val referenceLabel: String,
    val frequencyHz: FloatArray,
    /** The reference curve, 0 dB at 1 kHz. Adding a deviation to it gives the measured spectrum. */
    val referenceDb: FloatArray,
    /** Last ~3 s, relative to the reference. */
    val liveDeviationDb: FloatArray,
    /** Whole song so far, relative to the reference; null until the song has audio past its first seconds. */
    val trackDeviationDb: FloatArray?,
    /** Song average per tonal band; NaN where the song has no content or no data yet. */
    val tonalBandsDb: Map<TonalBand, Float>,
    val liveTonalBandsDb: Map<TonalBand, Float>,
    /** Short verdict such as "Balanced" or "Bright"; changes only after it held for a moment. */
    val verdict: String,
    val descriptor: String,
    val deviationScoreDb: Float,
    val confidence: Float,
    val isTransition: Boolean,
    /** Upper edge of the source's bandwidth when it ends in a brick-wall cut (lossy codecs). */
    val bandLimitHz: Float?,
    /** Lowest frequency the source has content at, when it starts well above 20 Hz. */
    val lowLimitHz: Float?,
    val lastResetReason: TrackBoundaryReason?,
    /** Audio buffers skipped because analysis fell behind. */
    val droppedBuffers: Int
)
