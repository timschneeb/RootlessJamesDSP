package me.timschneeberger.rootlessjamesdsp.androideq

import android.media.audiofx.DynamicsProcessing
import android.os.Build
import androidx.annotation.RequiresApi
import me.timschneeberger.rootlessjamesdsp.interop.JamesDspBaseEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.math.ln

/**
 * "Android EQ" mode: instead of capturing audio, the EQ curve runs inside each app through a
 * DynamicsProcessing effect on its audio session. The effect sits in the app's own output path, so
 * the player's A/V clock stays correct; only the effect's block latency is added.
 */
@RequiresApi(Build.VERSION_CODES.P)
object AndroidEq {
    data class Settings(
        val enabled: Boolean = false,
        val blockSize: Int = DEFAULT_BLOCK_SIZE,
        val limiter: Boolean = true,
        val sampleRate: Int = 48000,
    )

    @Volatile
    var settings = Settings()
        private set

    val isEnabled get() = settings.enabled

    private val effects = CopyOnWriteArraySet<DynamicsProcessing>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()

    // Left and right target curves; null means flat
    private var curves: Pair<List<Pair<Double, Double>>?, List<Pair<Double, Double>>?> = null to null
    private var fitter: AndroidEqFitter? = null
    @Volatile
    private var fitted: Fitted? = null

    // Band counts are fixed when an effect is created, so stages are only reused for the same layout.
    // stages[0] is the left channel, stages[1] the right one.
    private class Fitted(val blockSize: Int, val sampleRate: Int, val stages: List<AndroidEqFitter.Stages>)

    /** Returns true if existing effects must be recreated (block size or mode changed). */
    fun configure(newSettings: Settings): Boolean {
        val old = settings
        settings = newSettings
        val recreate = old.enabled != newSettings.enabled ||
                old.blockSize != newSettings.blockSize ||
                old.sampleRate != newSettings.sampleRate
        if (recreate)
            refit()
        else if (old.limiter != newSettings.limiter)
            effects.forEach { applyLimiter(it) }
        return recreate
    }

    /**
     * Receives the engine's combined GEQ/PEQ curve ("GraphicEQ: f g; ..."), or a left and a right
     * curve joined by [JamesDspBaseEngine.STEREO_GRAPHIC_EQ_SPLIT].
     */
    fun setCurve(enabled: Boolean, graphicEq: String) {
        curves = if (!enabled) {
            null to null
        } else {
            val parts = graphicEq.split(JamesDspBaseEngine.STEREO_GRAPHIC_EQ_SPLIT)
            val left = parseGraphicEq(parts[0])
            left to (parts.getOrNull(1)?.let { parseGraphicEq(it) } ?: left)
        }
        refit()
    }

    fun create(sessionId: Int): DynamicsProcessing? {
        val s = settings
        val bands = currentStages(s) ?: flatStages(s)
        return try {
            val builder = DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION, 2,
                true, bands[0].preEq.size,
                true, bands[0].mbc.size,
                true, bands[0].postEq.size,
                true
            )
                .setPreferredFrameDuration(frameDurationMs(s))
                .setLimiterAllChannelsTo(limiter(s.limiter))
            for (ch in 0..1) {
                builder.setPreEqByChannelIndex(ch, eq(bands[ch].preEq))
                    .setMbcByChannelIndex(ch, mbc(bands[ch].mbc))
                    .setPostEqByChannelIndex(ch, eq(bands[ch].postEq))
            }
            val config = builder.build()
            DynamicsProcessing(Int.MAX_VALUE, sessionId, config).apply {
                enabled = true
                setEnableStatusListener { effect, enabled ->
                    if (!enabled) {
                        try {
                            effect.enabled = true
                        } catch (ex: Exception) {
                            Timber.w(ex, "Android EQ: could not re-enable effect (session $sessionId)")
                        }
                    }
                }
                effects += this
                // A fit may have finished while this effect was being built
                currentStages(s)?.takeIf { it !== bands }?.let { applyStages(this, it) }
            }
        } catch (ex: Exception) {
            Timber.e(ex, "Android EQ: failed to attach to session $sessionId")
            null
        }
    }

    fun release(effect: DynamicsProcessing) {
        effects -= effect
    }

    private fun refit() {
        val s = settings
        if (!s.enabled)
            return
        val (left, right) = curves
        scope.launch {
            mutex.withLock {
                val f = fitterFor(s)
                val started = System.nanoTime()
                fun fit(target: List<Pair<Double, Double>>?) =
                    if (target == null) f.toStages(DoubleArray(f.edges.size))
                    else f.fit { freq -> interpolate(target, freq) }
                val leftStages = fit(left)
                val result = listOf(leftStages, if (right == left) leftStages else fit(right))
                Timber.i("Android EQ: fitted ${f.edges.size} segments (${if (right == left) "both channels" else "per channel"}) " +
                        "in ${(System.nanoTime() - started) / 1_000_000} ms")
                fitted = Fitted(s.blockSize, s.sampleRate, result)
                effects.forEach { applyStages(it, result) }
            }
        }
    }

    private fun applyStages(effect: DynamicsProcessing, bands: List<AndroidEqFitter.Stages>) {
        try {
            // Channels beyond the first two (multichannel outputs) follow the left curve
            for (ch in 0 until effect.channelCount) {
                val stages = bands[if (ch == 1) 1 else 0]
                effect.setPreEqByChannelIndex(ch, eq(stages.preEq))
                effect.setMbcByChannelIndex(ch, mbc(stages.mbc))
                effect.setPostEqByChannelIndex(ch, eq(stages.postEq))
            }
        } catch (ex: Exception) {
            Timber.w(ex, "Android EQ: failed to update effect")
        }
    }

    private fun applyLimiter(effect: DynamicsProcessing) {
        try {
            effect.setLimiterAllChannelsTo(limiter(settings.limiter))
        } catch (ex: Exception) {
            Timber.w(ex, "Android EQ: failed to update limiter")
        }
    }

    private fun frameDurationMs(s: Settings) =
        // Just under one block, so the effect rounds up to exactly blockSize
        (s.blockSize - 0.5f) * 1000f / s.sampleRate

    private fun currentStages(s: Settings) =
        fitted?.takeIf { it.blockSize == s.blockSize && it.sampleRate == s.sampleRate }?.stages

    @Synchronized
    private fun fitterFor(s: Settings): AndroidEqFitter =
        fitter?.takeIf { it.blockSize == s.blockSize && it.sampleRate == s.sampleRate }
            ?: AndroidEqFitter(s.sampleRate, s.blockSize).also { fitter = it }

    private fun flatStages(s: Settings) = fitterFor(s).let { f ->
        f.toStages(DoubleArray(f.edges.size)).let { listOf(it, it) }
    }

    private fun eq(bands: List<AndroidEqFitter.Band>) =
        DynamicsProcessing.Eq(true, true, bands.size).apply {
            bands.forEachIndexed { i, b -> setBand(i, DynamicsProcessing.EqBand(true, b.cutoffHz, b.gainDb)) }
        }

    // Ratio 1 and no gate: each band is a plain gain
    private fun mbc(bands: List<AndroidEqFitter.Band>) =
        DynamicsProcessing.Mbc(true, true, bands.size).apply {
            bands.forEachIndexed { i, b ->
                setBand(i, DynamicsProcessing.MbcBand(true, b.cutoffHz, 3f, 80f, 1f, 0f, 0f, -90f, 1f, 0f, b.gainDb))
            }
        }

    private fun limiter(enabled: Boolean) =
        DynamicsProcessing.Limiter(true, enabled, 0, 1f, 60f, 10f, -1f, 0f)

    private fun parseGraphicEq(str: String): List<Pair<Double, Double>>? =
        str.substringAfter(":", "").split(";").mapNotNull { node ->
            val parts = node.trim().split(" ").filter { it.isNotBlank() }
            val f = parts.getOrNull(0)?.toDoubleOrNull()
            val g = parts.getOrNull(1)?.toDoubleOrNull()
            if (f != null && g != null && f > 0) f to g else null
        }.sortedBy { it.first }.takeIf { it.isNotEmpty() }

    /** Linear in log frequency, held flat beyond the first and last node. */
    private fun interpolate(nodes: List<Pair<Double, Double>>, freq: Double): Double {
        if (freq <= nodes.first().first) return nodes.first().second
        if (freq >= nodes.last().first) return nodes.last().second
        val i = nodes.indexOfFirst { it.first >= freq }
        val (f0, g0) = nodes[i - 1]
        val (f1, g1) = nodes[i]
        return g0 + (g1 - g0) * (ln(freq / f0) / ln(f1 / f0))
    }

    const val DEFAULT_BLOCK_SIZE = 1024
}
