package me.timschneeberger.rootlessjamesdsp.fragment

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Build
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.activity.MainActivity
import me.timschneeberger.rootlessjamesdsp.activity.SettingsActivity
import me.timschneeberger.rootlessjamesdsp.analysis.PreDspTonalityAnalyzer
import me.timschneeberger.rootlessjamesdsp.analysis.TonalBand
import me.timschneeberger.rootlessjamesdsp.analysis.TonalityFrame
import me.timschneeberger.rootlessjamesdsp.analysis.TonalityReferenceStore
import me.timschneeberger.rootlessjamesdsp.databinding.CardSongTonalityBinding
import me.timschneeberger.rootlessjamesdsp.fragment.settings.SettingsTonalityFragment
import me.timschneeberger.rootlessjamesdsp.session.rootless.MediaSessionTrackObserver
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.toast
import me.timschneeberger.rootlessjamesdsp.utils.preferences.Preferences
import java.util.Locale
import kotlin.math.abs

/** The song tonality card on the DSP page. */
class SongTonalityCard(
    private val fragment: Fragment,
    private val card: CardSongTonalityBinding,
    private val prefs: Preferences.App
) {
    private val context get() = fragment.requireContext()
    private val bandValues = mutableMapOf<TonalBand, TextView>()
    private var latestFrame: TonalityFrame? = null
    private var held = false
    private var normalColors: ColorStateList? = null

    fun setup() {
        // One column per tonal band: value above, name below
        card.songTonalityBands.removeAllViews()
        bandValues.clear()
        for (band in TonalBand.entries) {
            val column = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            val value = TextView(context).apply {
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge)
                text = EMPTY_VALUE
                maxLines = 1
            }
            val label = TextView(context).apply {
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
                setTextColor(MaterialColors.getColor(card.root, android.R.attr.textColorSecondary))
                text = band.shortLabel
                maxLines = 1
            }
            column.addView(value)
            column.addView(label)
            card.songTonalityBands.addView(column)
            bandValues[band] = value
            normalColors = value.textColors
        }

        card.songTonalityView.check(if (showLive()) R.id.song_tonality_view_live else R.id.song_tonality_view_song)
        card.songTonalityView.addOnButtonCheckedListener { _, id, checked ->
            if (!checked) return@addOnButtonCheckedListener
            prefs.set(R.string.key_tonality_view, if (id == R.id.song_tonality_view_live) VIEW_LIVE else VIEW_SONG)
            update()
        }
        card.songTonalityDisplay.check(if (showRaw()) R.id.song_tonality_display_raw else R.id.song_tonality_display_compensated)
        card.songTonalityDisplay.addOnButtonCheckedListener { _, id, checked ->
            if (!checked) return@addOnButtonCheckedListener
            prefs.set(R.string.key_tonality_display, if (id == R.id.song_tonality_display_raw) DISPLAY_RAW else DISPLAY_COMPENSATED)
            update()
        }
        card.songTonalityHold.addOnCheckedChangeListener { button, checked ->
            held = checked
            button.setIconResource(if (checked) R.drawable.ic_baseline_play_arrow_24dp else R.drawable.ic_baseline_pause_24dp)
            if (checked && latestFrame != null)
                card.songTonalityStatus.text = listOf(card.songTonalityStatus.text, context.getString(R.string.tonality_held))
                    .filter { it.isNotEmpty() }.joinToString(" · ")
            else
                update()
        }
        card.songTonalityReset.setOnClickListener {
            (fragment.activity as? MainActivity)?.resetTonality()
        }
        card.songTonalitySettings.setOnClickListener { openSettings() }
        card.songTonalityReference.setOnClickListener { showReferencePicker() }
        card.songTonalityNoticeAction.setOnClickListener { openNotificationAccessSettings() }
    }

    fun submit(frame: TonalityFrame?) {
        latestFrame = frame
        update()
    }

    fun update() {
        if (!fragment.isAdded) return
        val enabled = prefs.get<Boolean>(R.string.key_tonality_enabled)
        card.root.isVisible = enabled
        if (!enabled) return

        val androidEq = prefs.get<Boolean>(R.string.key_android_eq_enable)
        val accessGranted = MediaSessionTrackObserver.isNotificationAccessGranted(context)
        card.songTonalityNotice.isVisible = androidEq || !accessGranted
        card.songTonalityNoticeText.text = context.getString(
            if (androidEq) R.string.tonality_android_eq_unavailable else R.string.tonality_notification_access
        )
        card.songTonalityNoticeAction.isVisible = !androidEq && !accessGranted

        val surface = card.mnoiseDeviationSurface
        val showLive = showLive()
        surface.showLive = showLive
        surface.showRaw = showRaw()
        surface.smoothingBandsPerOctave = prefs.get<String>(R.string.key_tonality_smoothing).toIntOrNull() ?: 6
        surface.rangeDb = prefs.get<String>(R.string.key_tonality_range).toFloatOrNull() ?: 12f

        // While held, the frozen frame stays; analysis goes on in the background
        if (held && latestFrame != null)
            return

        val frame = if (androidEq) null else latestFrame
        surface.submitFrame(frame)

        card.songTonalityReference.text = context.getString(R.string.tonality_versus,
            frame?.referenceLabel ?: selectedModeLabel())
        card.songTonalityVerdict.isVisible = frame != null
        card.songTonalityVerdict.text = frame?.verdict ?: PreDspTonalityAnalyzer.VERDICT_WARMING_UP
        val songReady = frame?.trackDeviationDb != null && frame.verdict != PreDspTonalityAnalyzer.VERDICT_WARMING_UP
        card.songTonalityDescriptor.text = when {
            androidEq -> context.getString(R.string.tonality_android_eq_unavailable)
            frame == null -> context.getString(R.string.tonality_waiting)
            showLive -> context.getString(R.string.tonality_live_hint)
            !songReady -> context.getString(R.string.tonality_warming_up)
            else -> frame.descriptor
        }

        val track = frame?.track
        val trackText = listOfNotNull(track?.title, track?.artist).joinToString(" · ")
        card.songTonalityTrack.isVisible = trackText.isNotEmpty()
        card.songTonalityTrack.text = trackText

        val bands = if (showLive) frame?.liveTonalBandsDb else frame?.tonalBandsDb
        val accent = MaterialColors.getColor(card.root, android.R.attr.colorAccent)
        for ((band, view) in bandValues) {
            val db = bands?.get(band)
            val outside = db != null && db.isFinite() && abs(db) > band.toleranceDb
            view.text = if (db == null || !db.isFinite()) EMPTY_VALUE else String.format(Locale.ROOT, "%+.1f", db)
            if (outside) view.setTextColor(accent) else normalColors?.let(view::setTextColor)
            view.setTypeface(null, if (outside) Typeface.BOLD else Typeface.NORMAL)
        }

        card.songTonalityStatus.text = if (frame == null) "" else listOfNotNull(
            context.getString(R.string.tonality_seconds_analyzed, frame.analyzedSeconds.toInt()),
            frame.lowLimitHz?.let { context.getString(R.string.tonality_content_from, it.toInt()) },
            frame.bandLimitHz?.let {
                context.getString(R.string.tonality_band_limited, String.format(Locale.ROOT, "%.1f", it / 1000f))
            }
        ).joinToString(" · ")
    }

    private fun showLive() = prefs.get<String>(R.string.key_tonality_view) == VIEW_LIVE

    private fun showRaw() = prefs.get<String>(R.string.key_tonality_display) == DISPLAY_RAW

    private fun selectedMode() = TonalityReferenceStore.normalizeMode(prefs.get<String>(R.string.key_tonality_reference_mode))

    private fun selectedModeLabel(): String {
        val values = context.resources.getStringArray(R.array.tonality_reference_modes_values)
        val labels = context.resources.getStringArray(R.array.tonality_reference_modes)
        return labels.getOrNull(values.indexOf(selectedMode())) ?: labels[0]
    }

    private fun showReferencePicker() {
        val store = TonalityReferenceStore(context)
        val values = context.resources.getStringArray(R.array.tonality_reference_modes_values)
        val labels = context.resources.getStringArray(R.array.tonality_reference_modes)
        val secondary = MaterialColors.getColor(card.root, android.R.attr.textColorSecondary)
        val items = values.indices.map { i ->
            SpannableStringBuilder(labels[i]).apply {
                val start = length
                append("\n").append(SettingsTonalityFragment.referenceSummary(context, store, values[i], prefs))
                setSpan(RelativeSizeSpan(0.8f), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(ForegroundColorSpan(secondary), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }.toTypedArray<CharSequence>()

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.tonality_reference_mode)
            .setSingleChoiceItems(items, values.indexOf(selectedMode())) { dialog, which ->
                if (values[which] == TonalityReferenceStore.MODE_CUSTOM && !store.hasCustom()) {
                    context.toast(R.string.tonality_reference_custom_none)
                    return@setSingleChoiceItems
                }
                prefs.set(R.string.key_tonality_reference_mode, values[which])
                dialog.dismiss()
                update()
            }
            .setNeutralButton(R.string.tonality_use_current_song) { _, _ -> useCurrentSong() }
            .setPositiveButton(R.string.action_settings) { _, _ -> openSettings() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // Saves the playing song's spectrum as the custom reference; bands outside the song's range follow the current reference
    private fun useCurrentSong() {
        val frame = latestFrame
        val song = frame?.trackDeviationDb
        if (frame == null || song == null || frame.analyzedSeconds < MIN_CAPTURE_SECONDS) {
            context.toast(context.getString(R.string.tonality_use_current_song_wait, MIN_CAPTURE_SECONDS.toInt()))
            return
        }
        val spectrum = FloatArray(song.size) { i ->
            val f = frame.frequencyHz[i]
            val inSource = f >= (frame.lowLimitHz ?: 0f) && f <= (frame.bandLimitHz ?: Float.MAX_VALUE)
            frame.referenceDb[i] + if (inSource && song[i].isFinite()) song[i] else 0f
        }
        val name = listOfNotNull(frame.track?.title, frame.track?.artist).joinToString(" · ").ifEmpty { "Captured song" }
        TonalityReferenceStore(context).saveCustomFromSong(name, spectrum)
        prefs.set(R.string.key_tonality_reference_mode, TonalityReferenceStore.MODE_CUSTOM)
        SettingsTonalityFragment.notifyReferenceChanged(prefs)
        context.toast(context.getString(R.string.tonality_use_current_song_done, name))
    }

    private fun openSettings() {
        fragment.startActivity(Intent(context, SettingsActivity::class.java)
            .putExtra(SettingsActivity.EXTRA_PAGE, SettingsActivity.PAGE_TONALITY))
    }

    private fun openNotificationAccessSettings() {
        val component = MediaSessionTrackObserver.notificationAccessComponent(context)
        val detail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component.flattenToString())
        else null
        try {
            fragment.startActivity(detail ?: Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        catch (ex: Exception) {
            fragment.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    companion object {
        private const val VIEW_SONG = "song"
        private const val VIEW_LIVE = "live"
        private const val DISPLAY_COMPENSATED = "compensated"
        private const val DISPLAY_RAW = "raw"
        private const val EMPTY_VALUE = "–"
        private const val MIN_CAPTURE_SECONDS = 20f
    }
}
