package me.timschneeberger.rootlessjamesdsp.analysis

import java.util.Locale
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow

enum class TonalityReferenceKind {
    IecProgrammeNoise,
    MNoise,
    PinkNoise,
    Tilt,
    MusicAverage,
    MusicLowPercussion,
    MusicHighPercussion,
    Library,
    SongCapture,
    VerifiedMNoiseWav,
    ImportedReferenceWav
}

data class TonalityAnalysisConfig(
    val sampleRate: Int,
    val fftSize: Int = 8192,
    val hopSize: Int = 2048,
    val bandsPerOctave: Int = 12,
    val minHz: Float = 20f,
    val maxHz: Float = minOf(20_000f, sampleRate / 2f)
)

data class TonalityReference(
    val referenceName: String,
    val referenceKind: TonalityReferenceKind,
    val sourceSha256: String?,
    val sourceMd5: String?,
    val analysisVersion: Int,
    val sampleRate: Int,
    val fftSize: Int,
    val hopSize: Int,
    val window: String,
    val bandsPerOctave: Int,
    val minHz: Float,
    val maxHz: Float,
    val frequencyHz: FloatArray,
    val referenceDb: FloatArray
) {
    val isVerified: Boolean
        get() = referenceKind == TonalityReferenceKind.VerifiedMNoiseWav

    /** Short name for the card, e.g. "IEC noise". */
    val displayLabel: String
        get() = when (referenceKind) {
            TonalityReferenceKind.IecProgrammeNoise -> "IEC noise"
            TonalityReferenceKind.MNoise -> "M-Noise"
            TonalityReferenceKind.PinkNoise -> "pink noise"
            TonalityReferenceKind.Tilt, TonalityReferenceKind.SongCapture,
            TonalityReferenceKind.ImportedReferenceWav -> referenceName
            TonalityReferenceKind.MusicAverage -> "music average"
            TonalityReferenceKind.MusicLowPercussion -> "acoustic music"
            TonalityReferenceKind.MusicHighPercussion -> "percussive music"
            TonalityReferenceKind.Library -> "your library"
            TonalityReferenceKind.VerifiedMNoiseWav -> "M-Noise WAV"
        }
}

data class TonalityBandLayout(
    val centerHz: FloatArray,
    val lowerHz: FloatArray,
    val upperHz: FloatArray,
    val binStart: IntArray,
    val binEndExclusive: IntArray
) {
    companion object {
        fun create(config: TonalityAnalysisConfig): TonalityBandLayout {
            val centers = mutableListOf<Float>()
            var center = config.minHz
            val step = 2.0.pow(1.0 / config.bandsPerOctave).toFloat()
            while (center <= config.maxHz * 1.0001f) {
                centers += center
                center *= step
            }

            val halfStep = 2.0.pow(0.5 / config.bandsPerOctave).toFloat()
            val centerArray = centers.toFloatArray()
            val lower = FloatArray(centerArray.size) { i -> centerArray[i] / halfStep }
            val upper = FloatArray(centerArray.size) { i -> centerArray[i] * halfStep }
            val binStart = IntArray(centerArray.size)
            val binEnd = IntArray(centerArray.size)
            val hzPerBin = config.sampleRate.toFloat() / config.fftSize
            val nyquistBin = config.fftSize / 2

            for (i in centerArray.indices) {
                binStart[i] = (lower[i] / hzPerBin).toInt().coerceIn(1, nyquistBin)
                binEnd[i] = ((upper[i] / hzPerBin).toInt() + 1).coerceIn(binStart[i] + 1, nyquistBin + 1)
            }

            return TonalityBandLayout(centerArray, lower, upper, binStart, binEnd)
        }
    }
}

/**
 * Built-in reference curves. All of them are power spectral densities (dB per Hz, the same
 * domain as the analyzer's per-bin band power); only their shape matters, since the analyzer
 * normalizes the level away.
 */
object TonalityReferenceFactory {
    const val ANALYSIS_VERSION = 1
    const val WINDOW_HANN = "hann"
    const val DEFAULT_TILT_DB_PER_OCTAVE = -4.5f

    /** The layout every reference and the analyzer share at [sampleRate]. */
    fun layoutFor(sampleRate: Int) = TonalityBandLayout.create(TonalityAnalysisConfig(sampleRate = sampleRate))

    fun create(kind: TonalityReferenceKind, sampleRate: Int, tiltDbPerOctave: Float = DEFAULT_TILT_DB_PER_OCTAVE): TonalityReference =
        when (kind) {
            TonalityReferenceKind.MNoise -> curve(kind, "M-Noise (AES75)", sampleRate, ::mNoiseDb)
            TonalityReferenceKind.PinkNoise -> curve(kind, "Pink noise", sampleRate) { tiltDb(it, -3f) }
            TonalityReferenceKind.Tilt -> curve(kind, tiltLabel(tiltDbPerOctave), sampleRate) { tiltDb(it, tiltDbPerOctave) }
            TonalityReferenceKind.MusicAverage -> table(kind, "Music average", sampleRate, MUSIC_AVERAGE)
            TonalityReferenceKind.MusicLowPercussion -> table(kind, "Acoustic music", sampleRate, MUSIC_LOW_PERCUSSION)
            TonalityReferenceKind.MusicHighPercussion -> table(kind, "Percussive music", sampleRate, MUSIC_HIGH_PERCUSSION)
            else -> curve(TonalityReferenceKind.IecProgrammeNoise, "IEC 60268-1 noise", sampleRate, ::iecProgrammeNoiseDb)
        }

    /** The default reference. */
    fun fallback(sampleRate: Int) = create(TonalityReferenceKind.IecProgrammeNoise, sampleRate)

    fun pinkNoise(sampleRate: Int) = create(TonalityReferenceKind.PinkNoise, sampleRate)

    /** A reference from a measured spectrum on the shared layout, e.g. a song or the library average. */
    fun measured(kind: TonalityReferenceKind, name: String, sampleRate: Int, referenceDb: FloatArray): TonalityReference {
        val base = fallback(sampleRate)
        require(referenceDb.size == base.frequencyHz.size) { "Reference has ${referenceDb.size} bands, expected ${base.frequencyHz.size}" }
        return base.copy(referenceName = name, referenceKind = kind, referenceDb = referenceDb.copyOf())
    }

    fun tiltLabel(dbPerOctave: Float) = String.format(Locale.ROOT, "%.1f dB/oct tilt", dbPerOctave)

    private fun curve(kind: TonalityReferenceKind, name: String, sampleRate: Int, db: (Float) -> Float): TonalityReference {
        val config = TonalityAnalysisConfig(sampleRate = sampleRate)
        val layout = TonalityBandLayout.create(config)
        return TonalityReference(
            referenceName = name,
            referenceKind = kind,
            sourceSha256 = null,
            sourceMd5 = null,
            analysisVersion = ANALYSIS_VERSION,
            sampleRate = sampleRate,
            fftSize = config.fftSize,
            hopSize = config.hopSize,
            window = WINDOW_HANN,
            bandsPerOctave = config.bandsPerOctave,
            minHz = config.minHz,
            maxHz = config.maxHz,
            frequencyHz = layout.centerHz.copyOf(),
            referenceDb = FloatArray(layout.centerHz.size) { i -> db(layout.centerHz[i]) }
        )
    }

    private fun table(kind: TonalityReferenceKind, name: String, sampleRate: Int, table: FloatArray) =
        curve(kind, name, sampleRate) { interpolateThirdOctaves(table, it) }

    private fun tiltDb(freqHz: Float, dbPerOctave: Float) = dbPerOctave * ln(freqHz / 1_000f) / ln(2f)

    /** IEC 60268-1 programme simulation noise: pink noise with the standard's third-octave shaping. */
    fun iecProgrammeNoiseDb(freqHz: Float) = tiltDb(freqHz, -3f) + interpolateThirdOctaves(IEC_60268_1, freqHz)

    /**
     * M-Noise (AES75): pink (-3 dB/oct) up to a few hundred Hz, bending into a steeper roll-off
     * above. Fitted to the official test signal measured through this analyzer (0.24 dB rms, 0.9
     * dB max over 20 Hz-20 kHz).
     */
    fun mNoiseDb(freqHz: Float): Float {
        val octaveFromKnee = ln(freqHz / KNEE_HZ) / ln(2f)
        val bend = BEND_OCTAVES * ln(1f + exp(octaveFromKnee / BEND_OCTAVES))
        return tiltDb(freqHz, -3f) + (HIGH_SLOPE_DB_PER_OCTAVE + 3f) * bend
    }

    // Linear in dB over log frequency between third-octave centers, held flat beyond the ends
    private fun interpolateThirdOctaves(table: FloatArray, freqHz: Float): Float {
        val position = (ln(freqHz / THIRD_OCTAVE_HZ[0]) / ln(2f) * 3f).coerceIn(0f, (table.size - 1).toFloat())
        val index = position.toInt().coerceAtMost(table.size - 2)
        val fraction = position - index
        return table[index] + (table[index + 1] - table[index]) * fraction
    }

    private const val KNEE_HZ = 639f
    private const val HIGH_SLOPE_DB_PER_OCTAVE = -5.44f
    private const val BEND_OCTAVES = 0.39f

    // Preferred third-octave centers 20 Hz-20 kHz; exactly 1/3 octave apart when computed as 20 * 2^(n/3)
    private val THIRD_OCTAVE_HZ = FloatArray(31) { n -> 20f * 2f.pow(n / 3f) }

    /** IEC 60268-1 simulated programme signal, third-octave band power relative to pink noise (dB). */
    private val IEC_60268_1 = floatArrayOf(
        -13.4f, -10.2f, -7.4f, -5.2f, -3.5f, -2.3f, -1.4f, -0.9f, -0.5f, -0.2f, -0.1f, 0f, 0f, 0f, 0f, 0f,
        0f, -0.1f, -0.3f, -0.6f, -1.0f, -1.6f, -2.5f, -3.7f, -5.1f, -7.0f, -9.4f, -11.9f, -14.8f, -18.2f, -21.6f
    )

    /*
     * Long-term average spectra of popular music, relative to 1 kHz, at third-octave centers.
     * Digitized from Elowsson & Friberg, "Long-term Average Spectrum in Popular Music and its
     * Relation to the Level of the Percussion" (AES 142, 2017): the mean of the whole dataset, and
     * the means of the least and most percussive groups (their percussion level explains most of
     * the spectral differences between genres). Ends beyond the published 20 Hz-17 kHz range are
     * extrapolated.
     */
    private val MUSIC_AVERAGE = floatArrayOf(
        -8.5f, -5.5f, -2.4f, 0.8f, 4.7f, 7.7f, 10.2f, 11.4f, 11.0f, 10.7f, 10.4f, 8.9f, 7.3f, 6.0f, 5.2f, 4.1f,
        2.0f, 0.0f, -1.6f, -3.1f, -5.2f, -7.0f, -9.4f, -12.5f, -15.6f, -17.9f, -20.3f, -23.3f, -28.0f, -31.0f, -33.7f
    )
    private val MUSIC_LOW_PERCUSSION = floatArrayOf(
        -13.0f, -11.8f, -9.7f, -6.8f, -3.5f, 0.0f, 4.6f, 8.5f, 9.0f, 10.1f, 10.6f, 10.5f, 9.0f, 7.1f, 6.0f, 4.7f,
        2.3f, 0.0f, -1.9f, -4.2f, -7.2f, -9.7f, -13.2f, -16.6f, -20.6f, -23.5f, -26.4f, -29.7f, -35.1f, -42.5f, -49.2f
    )
    private val MUSIC_HIGH_PERCUSSION = floatArrayOf(
        -5.0f, -2.1f, 2.6f, 7.9f, 11.7f, 13.7f, 14.2f, 13.8f, 12.5f, 11.4f, 10.4f, 8.4f, 6.7f, 5.3f, 4.6f, 3.8f,
        2.0f, 0.0f, -1.3f, -2.5f, -4.2f, -5.6f, -8.0f, -10.4f, -13.0f, -14.8f, -17.0f, -19.7f, -23.4f, -29.8f, -35.5f
    )
}
