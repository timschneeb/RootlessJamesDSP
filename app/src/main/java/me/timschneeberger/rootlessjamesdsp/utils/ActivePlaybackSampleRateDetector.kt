package me.timschneeberger.rootlessjamesdsp.utils

import android.content.Context
import me.timschneeberger.rootlessjamesdsp.session.dump.provider.AudioServiceDumpProvider
import me.timschneeberger.rootlessjamesdsp.session.dump.utils.DumpUtils
import timber.log.Timber

object ActivePlaybackSampleRateDetector {
    private val uidRegex = """u/pid:(\d+)/(\d+)""".toRegex()
    private val usageRegex = """usage=(\w+)""".toRegex()
    private val sampleRateRegex = """sampleRate=(\d+)""".toRegex()
    private val playbackConfigurationRegex = Regex(
        """AudioPlaybackConfiguration.*?(?=\n\s*AudioPlaybackConfiguration|\z)""",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val capturedUsages = setOf("USAGE_MEDIA", "USAGE_GAME", "USAGE_UNKNOWN")

    fun detect(context: Context, excludedUid: Int): Int {
        val dump = DumpUtils.dumpAll(context, AudioServiceDumpProvider.TARGET_SERVICE)
            ?: return 0
        return parse(dump, excludedUid)
    }

    internal fun parse(dump: String, excludedUid: Int): Int {
        return playbackConfigurationRegex.findAll(dump).mapNotNull { match ->
            val configuration = match.value
            if (!configuration.contains("state:started")) {
                return@mapNotNull null
            }

            val uid = uidRegex.find(configuration)?.groupValues?.getOrNull(1)?.toIntOrNull()
                ?: return@mapNotNull null
            val usage = usageRegex.find(configuration)?.groupValues?.getOrNull(1)
                ?: return@mapNotNull null
            val sampleRate = sampleRateRegex.find(configuration)
                ?.groupValues?.getOrNull(1)?.toIntOrNull()
                ?: return@mapNotNull null

            sampleRate.takeIf {
                uid != excludedUid &&
                    usage in capturedUsages &&
                    AudioSampleRateDetector.isSupportedProcessingRate(it)
            }
        }.maxOrNull().also {
            Timber.d("Audio service dump active content sample rate: ${it ?: 0}Hz")
        } ?: 0
    }
}
