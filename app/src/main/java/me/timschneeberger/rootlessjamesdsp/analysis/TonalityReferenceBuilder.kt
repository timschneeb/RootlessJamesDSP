package me.timschneeberger.rootlessjamesdsp.analysis

class TonalityReferenceBuilder {
    fun build(
        decoded: DecodedWavPcm,
        targetSampleRate: Int,
        hashes: ReferenceFileHashes,
        verifiedMNoise: Boolean
    ): TonalityReference {
        val config = TonalityAnalysisConfig(sampleRate = targetSampleRate)
        val layout = TonalityBandLayout.create(config)
        val analyzer = StereoFftBandAnalyzer(config, layout)
        val resampled = PcmResampler.resampleInterleavedStereo(
            decoded.interleavedStereo,
            decoded.sampleRate,
            targetSampleRate
        )
        val frames = analyzer.analyzeWholeInterleavedStereo(resampled, resampled.size)
        require(frames.isNotEmpty()) { "Reference WAV is shorter than the analysis window" }

        val meanPower = FloatArray(layout.centerHz.size)
        for (frame in frames) {
            for (i in frame.indices) {
                meanPower[i] += frame[i]
            }
        }
        for (i in meanPower.indices) {
            meanPower[i] /= frames.size
        }

        val kind = if (verifiedMNoise) {
            TonalityReferenceKind.VerifiedMNoiseWav
        }
        else {
            TonalityReferenceKind.ImportedReferenceWav
        }

        return TonalityReference(
            referenceName = if (verifiedMNoise) "M-Noise WAV" else decoded.displayName,
            referenceKind = kind,
            sourceSha256 = hashes.sha256,
            sourceMd5 = hashes.md5,
            analysisVersion = TonalityReferenceFactory.ANALYSIS_VERSION,
            sampleRate = targetSampleRate,
            fftSize = config.fftSize,
            hopSize = config.hopSize,
            window = TonalityReferenceFactory.WINDOW_HANN,
            bandsPerOctave = config.bandsPerOctave,
            minHz = config.minHz,
            maxHz = config.maxHz,
            frequencyHz = layout.centerHz.copyOf(),
            referenceDb = FloatArray(meanPower.size) { i -> StereoFftBandAnalyzer.powerToDb(meanPower[i]) }
        )
    }
}
