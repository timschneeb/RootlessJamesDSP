package me.timschneeberger.rootlessjamesdsp.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import timber.log.Timber

/** Detects output sample rates that can have dedicated convolver impulse responses. */
object AudioSampleRateDetector {

    fun getOutputSampleRates(context: Context): List<Int> {
        val audioManager = context.getSystemService<AudioManager>()
        val nativeRate = audioManager?.let(::getActiveOutputSampleRate) ?: 0

        val mixerRates = if (audioManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            getAndroid14MixerRates(audioManager)
        } else {
            emptyList()
        }

        return normalizeSampleRates(mixerRates, nativeRate)
    }

    fun getActiveOutputSampleRate(audioManager: AudioManager): Int {
        val preferredMixerRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            getAndroid14PreferredMixerRate(audioManager)
        } else {
            0
        }
        val nativeRate = audioManager
            .getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
            ?.toIntOrNull()
            ?: 0

        return preferredMixerRate.takeIf(::isSupportedProcessingRate)
            ?: nativeRate.takeIf(::isSupportedProcessingRate)
            ?: DEFAULT_SAMPLE_RATE
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun getAndroid14MixerRates(audioManager: AudioManager): List<Int> {
        return try {
            val mediaAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()

            audioManager.getAudioDevicesForAttributes(mediaAttributes)
                .flatMap { device ->
                    audioManager.getSupportedMixerAttributes(device).map { it.format.sampleRate } +
                        device.sampleRates.toList()
                }
        } catch (exception: RuntimeException) {
            // Some vendor audio services expose the API but reject the query.
            Timber.w(exception, "Failed to query Android 14 mixer sample rates")
            emptyList()
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun getAndroid14PreferredMixerRate(audioManager: AudioManager): Int {
        return try {
            val mediaAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()

            audioManager.getAudioDevicesForAttributes(mediaAttributes)
                .firstNotNullOfOrNull { device ->
                    audioManager.getPreferredMixerAttributes(mediaAttributes, device)
                        ?.format
                        ?.sampleRate
                        ?.takeIf(::isSupportedProcessingRate)
                } ?: 0
        } catch (exception: RuntimeException) {
            Timber.w(exception, "Failed to query Android 14 preferred mixer sample rate")
            0
        }
    }

    internal fun normalizeSampleRates(
        detectedRates: Collection<Int>,
        fallbackRate: Int,
    ): List<Int> {
        val rates = detectedRates
            .filter(::isSupportedProcessingRate)
            .toSortedSet()
        if (rates.isNotEmpty()) return rates.toList()

        return listOf(
            fallbackRate.takeIf(::isSupportedProcessingRate) ?: DEFAULT_SAMPLE_RATE
        )
    }

    internal fun isSupportedProcessingRate(rate: Int) =
        rate in MIN_SAMPLE_RATE..MAX_SAMPLE_RATE

    private const val DEFAULT_SAMPLE_RATE = 48_000
    private const val MIN_SAMPLE_RATE = 1
    private const val MAX_SAMPLE_RATE = 384_000
}
