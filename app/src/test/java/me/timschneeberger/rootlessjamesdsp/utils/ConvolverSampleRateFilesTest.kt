package me.timschneeberger.rootlessjamesdsp.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class ConvolverSampleRateFilesTest {

    @Test
    fun `sample rates are formatted accurately in kilohertz`() {
        assertEquals("11.025", ConvolverSampleRateFiles.formatKilohertz(11_025))
        assertEquals("44.1", ConvolverSampleRateFiles.formatKilohertz(44_100))
        assertEquals("192", ConvolverSampleRateFiles.formatKilohertz(192_000))
        assertEquals("192", ConvolverSampleRateFiles.formatKilohertz(192_000))
    }

    @Test
    fun `assignments through 192 kHz are retained and higher rates are rejected`() {
        val encoded = ConvolverSampleRateFiles.encode(
            mapOf(192_000 to "Convolver/192.wav", 384_000 to "Convolver/384.wav")
        )

        assertEquals(
            mapOf(192_000 to "Convolver/192.wav"),
            ConvolverSampleRateFiles.decode(encoded),
        )
        assertEquals(
            "Convolver/default.wav",
            ConvolverSampleRateFiles.resolve(
                "{\"384000\":\"Convolver/384.wav\"}",
                384_000,
                "Convolver/default.wav",
            ),
        )
    }

    @Test
    fun `assignments survive serialization`() {
        val assignments = mapOf(
            44_100 to "Convolver/room 44.wav",
            48_000 to "Convolver/room 48.wav",
            96_000 to "Convolver/room 96.wav",
        )

        assertEquals(
            assignments,
            ConvolverSampleRateFiles.decode(ConvolverSampleRateFiles.encode(assignments)),
        )
    }

    @Test
    fun `exact sample rate selects its assigned file`() {
        val encoded = ConvolverSampleRateFiles.encode(
            mapOf(44_100 to "Convolver/44.wav", 48_000 to "Convolver/48.wav")
        )

        assertEquals(
            "Convolver/44.wav",
            ConvolverSampleRateFiles.resolve(encoded, 44_100, "Convolver/default.wav"),
        )
        assertEquals(
            "Convolver/48.wav",
            ConvolverSampleRateFiles.resolve(encoded, 48_000, "Convolver/default.wav"),
        )
    }

    @Test
    fun `unassigned sample rate uses default file`() {
        val encoded = ConvolverSampleRateFiles.encode(
            mapOf(48_000 to "Convolver/48.wav")
        )

        assertEquals(
            "Convolver/default.wav",
            ConvolverSampleRateFiles.resolve(encoded, 96_000, "Convolver/default.wav"),
        )
    }
}
