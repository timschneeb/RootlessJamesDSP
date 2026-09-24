package me.timschneeberger.rootlessjamesdsp.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.withStyledAttributes
import me.timschneeberger.rootlessjamesdsp.analysis.TonalBand
import me.timschneeberger.rootlessjamesdsp.analysis.TonalityFrame
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * The song (and the last seconds) against the tonality reference, over frequency. Compensated
 * mode draws the deviation from the reference with its normal range shaded; raw mode draws the
 * measured spectrum with the reference as a dashed line.
 */
class MNoiseDeviationSurface @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val zeroPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val primaryPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val secondaryPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val referencePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rangePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val limitPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val limitFillPaint = Paint()
    private val tracePath = Path()
    private var frequencyHz = FloatArray(0)
    private var referenceDb = FloatArray(0)
    private var liveDeviationDb = FloatArray(0)
    private var trackDeviationDb: FloatArray? = null
    private var bandLimitHz: Float? = null
    private var lowLimitHz: Float? = null

    // The vertical window drawn now and the one it glides towards, see updateWindow()
    private var windowTopDb = 12f
    private var windowBottomDb = -12f
    private var targetTopDb = 12f
    private var targetBottomDb = -12f
    private var windowShowsRaw: Boolean? = null
    private var lastWindowNs = 0L

    /** Draw the live trace as the main one instead of the song average. */
    var showLive = false
        set(value) { field = value; invalidate() }

    /** Draw the measured spectrum instead of the deviation from the reference. */
    var showRaw = false
        set(value) { field = value; invalidate() }

    /** Smoothing width: 1/[smoothingBandsPerOctave] octave. */
    var smoothingBandsPerOctave = 6
        set(value) { field = value; invalidate() }

    /** Smallest vertical range of compensated mode, +/- this many dB; it grows when a trace needs more. */
    var rangeDb = 12f
        set(value) { field = value; invalidate() }

    init {
        gridPaint.color = getColor(android.R.attr.colorControlHighlight)
        gridPaint.style = Paint.Style.STROKE
        gridPaint.strokeWidth = 2f

        zeroPaint.color = getColor(android.R.attr.textColorPrimary)
        zeroPaint.alpha = 128
        zeroPaint.style = Paint.Style.STROKE
        zeroPaint.strokeWidth = 3f

        textPaint.color = getColor(android.R.attr.textColorSecondary)
        textPaint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10f, resources.displayMetrics)

        primaryPaint.color = getColor(android.R.attr.colorAccent)
        primaryPaint.style = Paint.Style.STROKE
        primaryPaint.strokeWidth = 5f
        primaryPaint.strokeJoin = Paint.Join.ROUND

        secondaryPaint.color = getColor(android.R.attr.colorAccent)
        secondaryPaint.alpha = 110
        secondaryPaint.style = Paint.Style.STROKE
        secondaryPaint.strokeWidth = 3f

        referencePaint.color = getColor(android.R.attr.textColorPrimary)
        referencePaint.alpha = 170
        referencePaint.style = Paint.Style.STROKE
        referencePaint.strokeWidth = 3f
        referencePaint.pathEffect = DashPathEffect(floatArrayOf(12f, 10f), 0f)

        rangePaint.color = getColor(android.R.attr.colorAccent)
        rangePaint.alpha = 34
        rangePaint.style = Paint.Style.FILL

        limitPaint.color = getColor(android.R.attr.textColorSecondary)
        limitPaint.style = Paint.Style.STROKE
        limitPaint.strokeWidth = 2f
        limitPaint.pathEffect = DashPathEffect(floatArrayOf(8f, 8f), 0f)

        limitFillPaint.color = getColor(android.R.attr.colorControlHighlight)
        limitFillPaint.alpha = 90
    }

    fun submitFrame(frame: TonalityFrame?) {
        if (frame == null) {
            frequencyHz = FloatArray(0)
            referenceDb = FloatArray(0)
            liveDeviationDb = FloatArray(0)
            trackDeviationDb = null
            bandLimitHz = null
            lowLimitHz = null
        }
        else {
            frequencyHz = frame.frequencyHz.copyOf()
            referenceDb = frame.referenceDb.copyOf()
            liveDeviationDb = frame.liveDeviationDb.copyOf()
            trackDeviationDb = frame.trackDeviationDb?.copyOf()
            bandLimitHz = frame.bandLimitHz
            lowLimitHz = frame.lowLimitHz
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val track = trackDeviationDb?.let(::smooth)
        val live = if (liveDeviationDb.isNotEmpty()) smooth(liveDeviationDb) else null
        val primary = if (showLive) live else track
        val secondary = if (showLive) track else live

        updateWindow(primary)
        drawLimits(canvas)
        if (!showRaw) drawNormalRange(canvas)
        drawGrid(canvas)
        if (frequencyHz.isEmpty()) return

        if (showRaw && referenceDb.size == frequencyHz.size)
            drawTrace(canvas, referenceDb, referencePaint, clipToSource = false)
        secondary?.let { drawTrace(canvas, toDisplay(it), secondaryPaint) }
        primary?.let { drawTrace(canvas, toDisplay(it), primaryPaint) }

        // Keep animating until the window has arrived
        if (windowTopDb != targetTopDb || windowBottomDb != targetBottomDb)
            postInvalidateOnAnimation()
    }

    // In raw mode a deviation plus the reference is the measured spectrum
    private fun toDisplay(deviationDb: FloatArray): FloatArray =
        if (showRaw && referenceDb.size == deviationDb.size) FloatArray(deviationDb.size) { deviationDb[it] + referenceDb[it] }
        else deviationDb

    /*
     * The window follows the main trace (in raw mode also the reference), so a bass-heavy song is
     * not cut off. It glides instead of jumping: it opens up within a fraction of a second and
     * closes over a few seconds, so loud passages never make the axis lurch.
     */
    private fun updateWindow(primary: FloatArray?) {
        updateTarget(primary)
        val now = System.nanoTime()
        if (windowShowsRaw != showRaw) {
            // A new mode starts where it belongs
            windowShowsRaw = showRaw
            windowTopDb = targetTopDb
            windowBottomDb = targetBottomDb
        }
        else {
            val seconds = ((now - lastWindowNs) / 1e9f).coerceIn(0f, MAX_FRAME_SECONDS)
            windowTopDb = glide(windowTopDb, targetTopDb, opening = targetTopDb > windowTopDb, seconds)
            windowBottomDb = glide(windowBottomDb, targetBottomDb, opening = targetBottomDb < windowBottomDb, seconds)
        }
        lastWindowNs = now
    }

    private fun glide(current: Float, target: Float, opening: Boolean, seconds: Float): Float {
        val timeConstant = if (opening) OPEN_SECONDS else CLOSE_SECONDS
        val next = current + (target - current) * (1f - exp(-seconds / timeConstant))
        return if (abs(next - target) < SETTLED_DB) target else next
    }

    // Rounded to whole steps, so small changes of the trace leave the target alone
    private fun updateTarget(primary: FloatArray?) {
        if (showRaw) {
            var high = Float.NEGATIVE_INFINITY
            var low = Float.POSITIVE_INFINITY
            fun include(db: Float) {
                if (!db.isFinite()) return
                high = max(high, db)
                low = min(low, db)
            }
            if (referenceDb.size == frequencyHz.size)
                referenceDb.forEach(::include)
            if (primary != null && primary.size == referenceDb.size && primary.size == frequencyHz.size)
                for (i in primary.indices)
                    if (inSource(frequencyHz[i])) include(primary[i] + referenceDb[i])
            if (!high.isFinite()) return

            val top = ceil((high + RAW_MARGIN_DB) / RAW_ROUND_DB) * RAW_ROUND_DB
            val bottom = floor((low - RAW_MARGIN_DB) / RAW_ROUND_DB) * RAW_ROUND_DB
            targetTopDb = top
            targetBottomDb = min(bottom, top - RAW_MIN_SPAN_DB).coerceAtLeast(top - RAW_MAX_SPAN_DB)
        }
        else {
            var reach = 0f
            if (primary != null && primary.size == frequencyHz.size)
                for (i in primary.indices)
                    if (inSource(frequencyHz[i]) && primary[i].isFinite()) reach = max(reach, abs(primary[i]))
            val half = max(rangeDb, ceil((reach + COMPENSATED_MARGIN_DB) / COMPENSATED_ROUND_DB) * COMPENSATED_ROUND_DB)
                .coerceAtMost(COMPENSATED_MAX_DB)
            targetTopDb = half
            targetBottomDb = -half
        }
    }

    // The normal range of each tonal band, joined across frequency
    private fun drawNormalRange(canvas: Canvas) {
        val left = paddingLeft.toFloat()
        val right = (width - paddingRight).toFloat()
        tracePath.rewind()
        val steps = 64
        for (s in 0..steps) {
            val x = left + (right - left) * s / steps
            val y = yForDb(toleranceAt(frequencyForX(x)))
            if (s == 0) tracePath.moveTo(x, y) else tracePath.lineTo(x, y)
        }
        for (s in steps downTo 0) {
            val x = left + (right - left) * s / steps
            tracePath.lineTo(x, yForDb(-toleranceAt(frequencyForX(x))))
        }
        tracePath.close()
        canvas.drawPath(tracePath, rangePaint)
    }

    private fun toleranceAt(freqHz: Float): Float {
        val bands = TonalBand.entries
        if (freqHz <= bands.first().centerHz) return bands.first().toleranceDb
        if (freqHz >= bands.last().centerHz) return bands.last().toleranceDb
        val upper = bands.indexOfFirst { it.centerHz >= freqHz }
        val a = bands[upper - 1]
        val b = bands[upper]
        val t = ln(freqHz / a.centerHz) / ln(b.centerHz / a.centerHz)
        return a.toleranceDb + (b.toleranceDb - a.toleranceDb) * t
    }

    private fun drawLimits(canvas: Canvas) {
        val top = paddingTop.toFloat()
        val bottom = (height - paddingBottom).toFloat()
        bandLimitHz?.let {
            val x = xForFrequency(it)
            canvas.drawRect(x, top, (width - paddingRight).toFloat(), bottom, limitFillPaint)
            canvas.drawLine(x, top, x, bottom, limitPaint)
        }
        lowLimitHz?.let {
            val x = xForFrequency(it)
            canvas.drawRect(paddingLeft.toFloat(), top, x, bottom, limitFillPaint)
            canvas.drawLine(x, top, x, bottom, limitPaint)
        }
    }

    private fun drawGrid(canvas: Canvas) {
        val left = paddingLeft.toFloat()
        val right = (width - paddingRight).toFloat()
        val top = paddingTop.toFloat()
        val bottom = (height - paddingBottom).toFloat()

        val span = windowTopDb - windowBottomDb
        val step = if (span <= 12f) 3 else if (span <= 36f) 6 else 12
        var db = ceil(windowBottomDb / step).toInt() * step
        while (db <= windowTopDb) {
            drawDbLine(canvas, db.toFloat(), left, right, top, bottom, db == 0)
            db += step
        }

        for (freq in FREQ_GRID) {
            val x = xForFrequency(freq.toFloat())
            canvas.drawLine(x, top, x, bottom, gridPaint)
            if (freq in FREQ_LABELS) {
                val label = freqLabel(freq)
                canvas.drawText(label, x - textPaint.measureText(label) / 2, height - 4f, textPaint)
            }
        }
    }

    private fun drawDbLine(canvas: Canvas, db: Float, left: Float, right: Float, top: Float, bottom: Float, zero: Boolean) {
        val y = yForDb(db)
        canvas.drawLine(left, y, right, y, if (zero) zeroPaint else gridPaint)
        canvas.drawText(dbLabel(db.toInt()), 0f, (y + textPaint.textSize / 3).coerceIn(top + textPaint.textSize, bottom), textPaint)
    }

    private fun drawTrace(canvas: Canvas, db: FloatArray, paint: Paint, clipToSource: Boolean = true) {
        if (frequencyHz.size != db.size || frequencyHz.isEmpty()) return

        tracePath.rewind()
        var started = false
        for (i in frequencyHz.indices) {
            if (clipToSource && !inSource(frequencyHz[i])) {
                started = false
                continue
            }
            val x = xForFrequency(frequencyHz[i])
            val y = yForDb(db[i])
            if (!started) tracePath.moveTo(x, y) else tracePath.lineTo(x, y)
            started = true
        }
        canvas.drawPath(tracePath, paint)
    }

    private fun inSource(freqHz: Float) =
        freqHz <= (bandLimitHz ?: Float.MAX_VALUE) && freqHz >= (lowLimitHz ?: 0f)

    // Power-domain average over +/- half the smoothing width; the analysis uses 12 bands per octave
    private fun smooth(deviationDb: FloatArray): FloatArray {
        val halfWidth = (ANALYSIS_BANDS_PER_OCTAVE / smoothingBandsPerOctave) / 2
        if (halfWidth < 1) return deviationDb
        return FloatArray(deviationDb.size) { i ->
            var sum = 0.0
            var count = 0
            for (j in (i - halfWidth).coerceAtLeast(0)..(i + halfWidth).coerceAtMost(deviationDb.lastIndex)) {
                sum += 10.0.pow(deviationDb[j] / 10.0)
                ++count
            }
            (10.0 * log10(sum / count)).toFloat()
        }
    }

    private fun xForFrequency(freqHz: Float): Float {
        val clamped = freqHz.coerceIn(MIN_FREQ_HZ, MAX_FREQ_HZ)
        val x = (ln(clamped) - ln(MIN_FREQ_HZ)) / (ln(MAX_FREQ_HZ) - ln(MIN_FREQ_HZ))
        return paddingLeft + x * (width - paddingLeft - paddingRight)
    }

    private fun frequencyForX(x: Float): Float {
        val t = ((x - paddingLeft) / (width - paddingLeft - paddingRight)).coerceIn(0f, 1f)
        return MIN_FREQ_HZ * (MAX_FREQ_HZ / MIN_FREQ_HZ).pow(t)
    }

    private fun yForDb(db: Float): Float {
        val y = (windowTopDb - db.coerceIn(windowBottomDb, windowTopDb)) / (windowTopDb - windowBottomDb)
        return paddingTop + y * (height - paddingTop - paddingBottom)
    }

    private fun getColor(colorAttribute: Int): Int {
        if (isInEditMode) return 0xff000000.toInt()

        var color = 0
        context.withStyledAttributes(TypedValue().data, intArrayOf(colorAttribute)) {
            color = getColor(0, 0)
        }
        return color
    }

    private fun dbLabel(db: Int): String = if (db > 0) "+$db" else db.toString()

    private fun freqLabel(freq: Int): String = if (freq >= 1_000) "${freq / 1_000}k" else freq.toString()

    companion object {
        private const val MIN_FREQ_HZ = 20f
        private const val MAX_FREQ_HZ = 20_000f
        private const val ANALYSIS_BANDS_PER_OCTAVE = 12
        private const val RAW_MARGIN_DB = 3f
        private const val RAW_ROUND_DB = 6f
        private const val RAW_MIN_SPAN_DB = 36f
        private const val RAW_MAX_SPAN_DB = 96f
        private const val COMPENSATED_MARGIN_DB = 2f
        private const val COMPENSATED_ROUND_DB = 3f
        private const val COMPENSATED_MAX_DB = 36f
        private const val OPEN_SECONDS = 0.25f
        private const val CLOSE_SECONDS = 3f
        private const val SETTLED_DB = 0.05f
        private const val MAX_FRAME_SECONDS = 0.5f
        private val FREQ_GRID = intArrayOf(20, 50, 100, 200, 500, 1_000, 2_000, 5_000, 10_000, 20_000)
        private val FREQ_LABELS = intArrayOf(50, 100, 200, 500, 1_000, 2_000, 5_000, 10_000)
    }
}
