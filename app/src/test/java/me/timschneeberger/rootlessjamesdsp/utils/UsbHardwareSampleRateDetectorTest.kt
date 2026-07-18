package me.timschneeberger.rootlessjamesdsp.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class UsbHardwareSampleRateDetectorTest {
    @Test
    fun `reads active physical USB pcm rate`() {
        val dump = """
            Output thread 0x1, name AudioOut_15, tid 1, type 0 (MIXER):
              Standby: no
              Sample rate: 48000 Hz
              Output devices: 0x4000000 (AUDIO_DEVICE_OUT_USB_HEADSET)
              output pcm config sample rate: 192000
              1 Tracks of which 1 are active
            Output thread 0x2, name AudioOut_D, tid 2, type 0 (MIXER):
              Standby: no
              Sample rate: 48000 Hz
              Output devices: 0x2 (AUDIO_DEVICE_OUT_SPEAKER)
              1 Tracks of which 1 are active
        """.trimIndent()

        assertEquals(192_000, UsbHardwareSampleRateDetector.parse(dump))
    }

    @Test
    fun `ignores standby and inactive USB threads`() {
        val dump = """
            Output thread 0x1, name AudioOut_15, tid 1, type 0 (MIXER):
              Standby: yes
              Output devices: 0x4000000 (AUDIO_DEVICE_OUT_USB_HEADSET)
              output pcm config sample rate: 192000
              1 Tracks of which 0 are active
        """.trimIndent()

        assertEquals(0, UsbHardwareSampleRateDetector.parse(dump))
    }
}
