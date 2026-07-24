package me.timschneeberger.rootlessjamesdsp.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class ActivePlaybackSampleRateDetectorTest {
    @Test
    fun `reads active external media sample rate`() {
        val dump = """
            AudioPlaybackConfiguration piid:10 deviceId:7 type:android.media.AudioTrack u/pid:10123/456 state:started attr:AudioAttributes: usage=USAGE_MEDIA content=CONTENT_TYPE_MUSIC flags=0x0 sessionId:12 mutedState:none FormatInfo{isSpatialized=false, channelMask=0x3, sampleRate=96000}
        """.trimIndent()

        assertEquals(96_000, ActivePlaybackSampleRateDetector.parse(dump, 10001))
    }

    @Test
    fun `reads Samsung wrapped playback configuration`() {
        val dump = """
            AudioPlaybackConfiguration piid:10 deviceId:788 type:android.media.AudioTrack u/pid:10123/456 state:started
            attr:AudioAttributes: usage=USAGE_MEDIA content=CONTENT_TYPE_MUSIC flags=0x0 sessionId:12 mutedState:none
            FormatInfo{isSpatialized=false, channelMask=0x3, sampleRate=96000}
            AudioPlaybackConfiguration piid:11 deviceId:788 type:android.media.AudioTrack u/pid:10001/457 state:started
            attr:AudioAttributes: usage=USAGE_MEDIA content=CONTENT_TYPE_UNKNOWN flags=0x0 sessionId:13 mutedState:none
            FormatInfo{isSpatialized=false, channelMask=0x3, sampleRate=96000}
        """.trimIndent()

        assertEquals(96_000, ActivePlaybackSampleRateDetector.parse(dump, 10001))
    }

    @Test
    fun `ignores own paused and non captured players`() {
        val dump = """
            AudioPlaybackConfiguration piid:10 deviceId:7 type:android.media.AudioTrack u/pid:10001/456 state:started attr:AudioAttributes: usage=USAGE_MEDIA content=CONTENT_TYPE_MUSIC flags=0x0 sessionId:12 mutedState:none FormatInfo{isSpatialized=false, channelMask=0x3, sampleRate=192000}
            AudioPlaybackConfiguration piid:11 deviceId:7 type:android.media.AudioTrack u/pid:10123/457 state:paused attr:AudioAttributes: usage=USAGE_MEDIA content=CONTENT_TYPE_MUSIC flags=0x0 sessionId:13 mutedState:none FormatInfo{isSpatialized=false, channelMask=0x3, sampleRate=96000}
            AudioPlaybackConfiguration piid:12 deviceId:7 type:android.media.AudioTrack u/pid:10124/458 state:started attr:AudioAttributes: usage=USAGE_NOTIFICATION content=CONTENT_TYPE_SONIFICATION flags=0x0 sessionId:14 mutedState:none FormatInfo{isSpatialized=false, channelMask=0x3, sampleRate=48000}
        """.trimIndent()

        assertEquals(0, ActivePlaybackSampleRateDetector.parse(dump, 10001))
    }

    @Test
    fun `selects highest active captured rate through 384 kHz`() {
        val dump = """
            AudioPlaybackConfiguration piid:10 deviceId:7 type:android.media.AudioTrack u/pid:10123/456 state:started attr:AudioAttributes: usage=USAGE_MEDIA content=CONTENT_TYPE_MUSIC flags=0x0 sessionId:12 mutedState:none FormatInfo{isSpatialized=false, channelMask=0x3, sampleRate=88200}
            AudioPlaybackConfiguration piid:11 deviceId:7 type:AAudio u/pid:10124/457 state:started attr:AudioAttributes: usage=USAGE_GAME content=CONTENT_TYPE_UNKNOWN flags=0x0 sessionId:13 mutedState:none FormatInfo{isSpatialized=false, channelMask=0x3, sampleRate=192000}
            AudioPlaybackConfiguration piid:12 deviceId:7 type:AAudio u/pid:10125/458 state:started attr:AudioAttributes: usage=USAGE_MEDIA content=CONTENT_TYPE_MUSIC flags=0x0 sessionId:14 mutedState:none FormatInfo{isSpatialized=false, channelMask=0x3, sampleRate=384000}
        """.trimIndent()

        assertEquals(384_000, ActivePlaybackSampleRateDetector.parse(dump, 10001))
    }
}
