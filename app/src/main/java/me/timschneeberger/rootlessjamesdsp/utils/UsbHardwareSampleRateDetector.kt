package me.timschneeberger.rootlessjamesdsp.utils

import android.content.Context
import me.timschneeberger.rootlessjamesdsp.session.dump.utils.DumpUtils
import timber.log.Timber

/** Reads the physical PCM rate of the active USB output from AudioFlinger. */
object UsbHardwareSampleRateDetector {
    private const val AUDIO_FLINGER_SERVICE = "media.audio_flinger"
    private val outputThreadRegex = Regex(
        """Output thread .*?(?=\nOutput thread |\z)""",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val pcmConfigRateRegex = """output pcm config sample rate:\s*(\d+)""".toRegex()
    private val activeTracksRegex = """(\d+) Tracks of which (\d+) are active""".toRegex()

    fun detect(context: Context): Int {
        return DumpUtils.dumpAll(context, AUDIO_FLINGER_SERVICE)
            ?.let(::parse)
            ?: 0
    }

    internal fun parse(dump: String): Int {
        return outputThreadRegex.findAll(dump).mapNotNull { match ->
            val thread = match.value
            val isUsb = thread.contains("AUDIO_DEVICE_OUT_USB_DEVICE") ||
                thread.contains("AUDIO_DEVICE_OUT_USB_HEADSET") ||
                thread.contains("AUDIO_DEVICE_OUT_USB_ACCESSORY")
            val activeTrackCount = activeTracksRegex.find(thread)
                ?.groupValues?.getOrNull(2)?.toIntOrNull()
                ?: 0
            if (!isUsb || thread.contains("Standby: yes") || activeTrackCount == 0) {
                return@mapNotNull null
            }

            pcmConfigRateRegex.find(thread)?.groupValues?.getOrNull(1)?.toIntOrNull()
                ?.takeIf(AudioSampleRateDetector::isSupportedProcessingRate)
        }.maxOrNull().also {
            Timber.i("Active physical USB sample rate: ${it ?: 0}Hz")
        } ?: 0
    }
}
