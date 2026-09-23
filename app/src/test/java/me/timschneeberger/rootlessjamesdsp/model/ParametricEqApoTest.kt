package me.timschneeberger.rootlessjamesdsp.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ParametricEqApoTest {

    private fun bands(vararg b: ParametricEqBand) = ParametricEqBandList().apply { addAll(b) }

    @Test
    fun exportUsesChannelLinesAndRoundTrips() {
        val list = bands(
            ParametricEqBand(100.0, 3.0, 0.7, ParametricEqFilterType.LOW_SHELF),
            ParametricEqBand(1000.0, -2.0, 1.4, channel = ParametricEqChannel.LEFT),
            ParametricEqBand(2000.0, 1.0, 2.0, channel = ParametricEqChannel.LEFT),
            ParametricEqBand(3000.0, -4.0, 3.0, channel = ParametricEqChannel.RIGHT),
            ParametricEqBand(8000.0, 2.0, 1.0),
        )
        val apo = list.toApoString(-3.0)
        val lines = apo.lines().filter { it.isNotBlank() }
        assertEquals("Channel: L", lines[2])
        assertEquals("Channel: R", lines[5])
        assertEquals("Channel: all", lines[7])
        assertEquals(1, lines.count { it == "Channel: L" })

        val parsed = ParametricEqBandList()
        val result = parsed.fromApoString(apo)
        assertEquals(list.map { it.channel }, parsed.map { it.channel })
        assertEquals(list.map { it.frequency }, parsed.map { it.frequency })
        assertEquals(-3.0, result.preampDb, 1e-9)
        assertEquals(0, result.skippedFilters)
    }

    @Test
    fun importsEqualizerApoChannelSyntax() {
        val parsed = ParametricEqBandList()
        val result = parsed.fromApoString(
            """
            Preamp: -6 dB
            Filter 1: ON PK Fc 50 Hz Gain 2 dB Q 1
            Channel: L
            Filter 2: ON PK Fc 100 Hz Gain 1 dB Q 1
            Channel: 2
            Filter 3: ON PK Fc 200 Hz Gain 1 dB Q 1
            Channel: C LFE
            Filter 4: ON PK Fc 300 Hz Gain 1 dB Q 1
            Channel: L R
            Filter 5: ON PK Fc 400 Hz Gain 1 dB Q 1
            """.trimIndent()
        )
        assertEquals(
            listOf(ParametricEqChannel.LEFT_RIGHT, ParametricEqChannel.LEFT, ParametricEqChannel.RIGHT,
                ParametricEqChannel.LEFT_RIGHT),
            parsed.map { it.channel }
        )
        assertEquals(1, result.skippedFilters)
    }

    @Test
    fun importsLegacyChannelSuffix() {
        val parsed = ParametricEqBandList()
        parsed.fromApoString("Filter 1: ON PK Fc 1000 Hz Gain -2 dB Q 1.4 Channel R")
        assertEquals(ParametricEqChannel.RIGHT, parsed.single().channel)
    }
}
