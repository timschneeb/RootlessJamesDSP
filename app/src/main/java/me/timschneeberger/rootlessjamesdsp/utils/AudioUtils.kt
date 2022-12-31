package me.timschneeberger.rootlessjamesdsp.utils

import android.media.AudioFormat

object AudioUtils {
    fun toLogFriendlyEncoding(enc: Int): String? {
        return when (enc) {
            AudioFormat.ENCODING_INVALID -> "ENCODING_INVALID"
            AudioFormat.ENCODING_PCM_16BIT -> "ENCODING_PCM_16BIT"
            AudioFormat.ENCODING_PCM_8BIT -> "ENCODING_PCM_8BIT"
            AudioFormat.ENCODING_PCM_FLOAT -> "ENCODING_PCM_FLOAT"
            AudioFormat.ENCODING_AC3 -> "ENCODING_AC3"
            AudioFormat.ENCODING_E_AC3 -> "ENCODING_E_AC3"
            AudioFormat.ENCODING_DTS -> "ENCODING_DTS"
            AudioFormat.ENCODING_DTS_HD -> "ENCODING_DTS_HD"
            AudioFormat.ENCODING_MP3 -> "ENCODING_MP3"
            AudioFormat.ENCODING_AAC_LC -> "ENCODING_AAC_LC"
            AudioFormat.ENCODING_AAC_HE_V1 -> "ENCODING_AAC_HE_V1"
            AudioFormat.ENCODING_AAC_HE_V2 -> "ENCODING_AAC_HE_V2"
            AudioFormat.ENCODING_IEC61937 -> "ENCODING_IEC61937"
            AudioFormat.ENCODING_DOLBY_TRUEHD -> "ENCODING_DOLBY_TRUEHD"
            AudioFormat.ENCODING_AAC_ELD -> "ENCODING_AAC_ELD"
            AudioFormat.ENCODING_AAC_XHE -> "ENCODING_AAC_XHE"
            AudioFormat.ENCODING_AC4 -> "ENCODING_AC4"
            AudioFormat.ENCODING_E_AC3_JOC -> "ENCODING_E_AC3_JOC"
            AudioFormat.ENCODING_DOLBY_MAT -> "ENCODING_DOLBY_MAT"
            AudioFormat.ENCODING_OPUS -> "ENCODING_OPUS"
            AudioFormat.ENCODING_PCM_24BIT_PACKED -> "ENCODING_PCM_24BIT_PACKED"
            AudioFormat.ENCODING_PCM_32BIT -> "ENCODING_PCM_32BIT"
            AudioFormat.ENCODING_MPEGH_BL_L3 -> "ENCODING_MPEGH_BL_L3"
            AudioFormat.ENCODING_MPEGH_BL_L4 -> "ENCODING_MPEGH_BL_L4"
            AudioFormat.ENCODING_MPEGH_LC_L3 -> "ENCODING_MPEGH_LC_L3"
            AudioFormat.ENCODING_MPEGH_LC_L4 -> "ENCODING_MPEGH_LC_L4"
            AudioFormat.ENCODING_DTS_UHD -> "ENCODING_DTS_UHD"
            AudioFormat.ENCODING_DRA -> "ENCODING_DRA"
            else -> "unknown encoding $enc"
        }
    }
}