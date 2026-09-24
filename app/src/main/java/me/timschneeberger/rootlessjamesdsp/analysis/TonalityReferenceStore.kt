package me.timschneeberger.rootlessjamesdsp.analysis

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Picks the active reference and keeps the user's own ones in app-private storage: a custom
 * reference (an imported WAV or a captured song) and the running average of the songs played.
 */
class TonalityReferenceStore(private val context: Context) {
    /** [mode] is one of the MODE_* values of the reference setting. */
    fun loadFor(mode: String, sampleRate: Int, tiltDbPerOctave: Float = TonalityReferenceFactory.DEFAULT_TILT_DB_PER_OCTAVE): TonalityReference =
        when (normalizeMode(mode)) {
            MODE_MNOISE -> TonalityReferenceFactory.create(TonalityReferenceKind.MNoise, sampleRate)
            MODE_PINK -> TonalityReferenceFactory.create(TonalityReferenceKind.PinkNoise, sampleRate)
            MODE_TILT -> TonalityReferenceFactory.create(TonalityReferenceKind.Tilt, sampleRate, tiltDbPerOctave)
            MODE_MUSIC_AVERAGE -> TonalityReferenceFactory.create(TonalityReferenceKind.MusicAverage, sampleRate)
            MODE_MUSIC_ACOUSTIC -> TonalityReferenceFactory.create(TonalityReferenceKind.MusicLowPercussion, sampleRate)
            MODE_MUSIC_PERCUSSIVE -> TonalityReferenceFactory.create(TonalityReferenceKind.MusicHighPercussion, sampleRate)
            MODE_LIBRARY -> libraryReference(sampleRate) ?: TonalityReferenceFactory.fallback(sampleRate)
            MODE_CUSTOM -> loadCustom(sampleRate) ?: TonalityReferenceFactory.fallback(sampleRate)
            else -> TonalityReferenceFactory.fallback(sampleRate)
        }

    // Custom reference

    fun loadCustom(sampleRate: Int): TonalityReference? {
        val file = customFile(sampleRate)
        if (!file.exists()) return null

        return runCatching {
            val json = JSONObject(file.readText())
            TonalityReference(
                referenceName = json.getString("referenceName"),
                referenceKind = TonalityReferenceKind.valueOf(json.getString("referenceKind")),
                sourceSha256 = json.optString("sourceSha256").ifBlank { null },
                sourceMd5 = json.optString("sourceMd5").ifBlank { null },
                analysisVersion = json.getInt("analysisVersion"),
                sampleRate = json.getInt("sampleRate"),
                fftSize = json.getInt("fftSize"),
                hopSize = json.getInt("hopSize"),
                window = json.getString("window"),
                bandsPerOctave = json.getInt("bandsPerOctave"),
                minHz = json.getDouble("minHz").toFloat(),
                maxHz = json.getDouble("maxHz").toFloat(),
                frequencyHz = json.getJSONArray("frequencyHz").toFloatArray(),
                referenceDb = json.getJSONArray("referenceDb").toFloatArray()
            )
        }.getOrNull()?.takeIf {
            it.analysisVersion == TonalityReferenceFactory.ANALYSIS_VERSION &&
                it.sampleRate == sampleRate &&
                it.frequencyHz.size == it.referenceDb.size
        }
    }

    fun hasCustom(): Boolean = customDirectory().listFiles()?.any { it.name.startsWith(CUSTOM_PREFIX) } == true

    fun customName(): String? = loadCustom(48_000)?.displayLabel ?: loadCustom(44_100)?.displayLabel

    fun clearCustom() {
        customDirectory().listFiles()?.filter { it.name.startsWith(CUSTOM_PREFIX) }?.forEach { it.delete() }
    }

    fun saveCustom(reference: TonalityReference) {
        val file = customFile(reference.sampleRate)
        file.parentFile?.mkdirs()
        file.writeText(reference.toJson().toString())
    }

    /**
     * Saves a song's spectrum ([referenceDb], on the shared band layout) as the custom reference.
     * The layout has the same bands at 44.1 and 48 kHz, so one measurement serves both.
     */
    fun saveCustomFromSong(name: String, referenceDb: FloatArray) {
        for (rate in SAMPLE_RATES)
            saveCustom(TonalityReferenceFactory.measured(TonalityReferenceKind.SongCapture, name, rate, referenceDb))
    }

    // Library average

    /**
     * Adds a finished song to the library average. [songDb] is the song's spectrum normalized to
     * its own midrange; bands outside [validFromHz, validToHz] (no content, codec cut-off) are
     * left out for this song.
     */
    fun addLibrarySong(songDb: FloatArray, validFromHz: Float, validToHz: Float) = synchronized(LOCK) {
        val frequencies = TonalityReferenceFactory.layoutFor(48_000).centerHz
        if (songDb.size != frequencies.size) return@synchronized
        val library = readLibrary(frequencies.size)
        for (i in songDb.indices) {
            if (frequencies[i] < validFromHz || frequencies[i] > validToHz || !songDb[i].isFinite()) continue
            library.sumDb[i] += songDb[i].toDouble()
            library.count[i]++
        }
        library.songs++
        writeLibrary(library)
    }

    fun librarySongCount(): Int = synchronized(LOCK) { readLibrary(0).songs }

    fun clearLibrary() = synchronized(LOCK) { libraryFile().delete(); Unit }

    /** The library average once it holds [MIN_LIBRARY_SONGS] songs; bands no song covered follow the default reference. */
    fun libraryReference(sampleRate: Int): TonalityReference? = synchronized(LOCK) {
        val fallback = TonalityReferenceFactory.fallback(sampleRate)
        val library = readLibrary(fallback.frequencyHz.size)
        if (library.songs < MIN_LIBRARY_SONGS || library.count.size != fallback.frequencyHz.size) return@synchronized null

        // Where only a few songs reach (deep sub, top octave), blend towards the default curve
        val covered = library.count.indices.filter { library.count[it] >= MIN_LIBRARY_SONGS }
        if (covered.isEmpty()) return@synchronized null
        val offset = covered.map { library.sumDb[it] / library.count[it] - fallback.referenceDb[it] }.average()
        val db = FloatArray(fallback.frequencyHz.size) { i ->
            val weight = (library.count[i].toFloat() / MIN_LIBRARY_SONGS).coerceAtMost(1f)
            val measured = if (library.count[i] > 0) library.sumDb[i] / library.count[i] else 0.0
            (weight * measured + (1 - weight) * (fallback.referenceDb[i] + offset)).toFloat()
        }
        TonalityReferenceFactory.measured(TonalityReferenceKind.Library, "Your library", sampleRate, db)
    }

    private class Library(var songs: Int, val sumDb: DoubleArray, val count: IntArray)

    private fun readLibrary(bands: Int): Library {
        val file = libraryFile()
        return runCatching {
            val json = JSONObject(file.readText())
            val sums = json.getJSONArray("sumDb")
            val counts = json.getJSONArray("count")
            Library(json.getInt("songs"), DoubleArray(sums.length()) { sums.getDouble(it) }, IntArray(counts.length()) { counts.getInt(it) })
        }.getOrNull()?.takeIf { bands == 0 || it.sumDb.size == bands } ?: Library(0, DoubleArray(bands), IntArray(bands))
    }

    private fun writeLibrary(library: Library) {
        val file = libraryFile()
        file.parentFile?.mkdirs()
        val json = JSONObject()
            .put("songs", library.songs)
            .put("sumDb", JSONArray().apply { library.sumDb.forEach { put(it) } })
            .put("count", JSONArray().apply { library.count.forEach { put(it) } })
        file.writeText(json.toString())
    }

    private fun customDirectory() = File(context.filesDir, DIRECTORY)

    private fun customFile(sampleRate: Int) = File(customDirectory(), "$CUSTOM_PREFIX$sampleRate.json")

    private fun libraryFile() = File(customDirectory(), LIBRARY_FILE)

    private fun TonalityReference.toJson(): JSONObject {
        return JSONObject()
            .put("referenceName", referenceName)
            .put("referenceKind", referenceKind.name)
            .put("sourceSha256", sourceSha256 ?: "")
            .put("sourceMd5", sourceMd5 ?: "")
            .put("analysisVersion", analysisVersion)
            .put("sampleRate", sampleRate)
            .put("fftSize", fftSize)
            .put("hopSize", hopSize)
            .put("window", window)
            .put("bandsPerOctave", bandsPerOctave)
            .put("minHz", minHz.toDouble())
            .put("maxHz", maxHz.toDouble())
            .put("frequencyHz", frequencyHz.toJsonArray())
            .put("referenceDb", referenceDb.toJsonArray())
    }

    private fun FloatArray.toJsonArray(): JSONArray {
        val array = JSONArray()
        for (value in this) {
            array.put(value.toDouble())
        }
        return array
    }

    private fun JSONArray.toFloatArray(): FloatArray {
        return FloatArray(length()) { i -> getDouble(i).toFloat() }
    }

    companion object {
        private const val DIRECTORY = "tonality_references"
        // Kept from the first version, when only imported WAVs were stored here
        private const val CUSTOM_PREFIX = "reference_"
        private const val LIBRARY_FILE = "library.json"
        private val LOCK = Any()
        val SAMPLE_RATES = intArrayOf(44_100, 48_000)
        const val MIN_LIBRARY_SONGS = 5

        const val MODE_IEC = "iec"
        const val MODE_MNOISE = "mnoise"
        const val MODE_PINK = "pink"
        const val MODE_TILT = "tilt"
        const val MODE_MUSIC_AVERAGE = "music_average"
        const val MODE_MUSIC_ACOUSTIC = "music_acoustic"
        const val MODE_MUSIC_PERCUSSIVE = "music_percussive"
        const val MODE_LIBRARY = "library"
        const val MODE_CUSTOM = "custom"

        /** Maps values saved by earlier versions: "music" was the built-in default, "imported" the WAV slot. */
        fun normalizeMode(mode: String?): String = when (mode) {
            "music", null -> MODE_IEC
            "imported" -> MODE_CUSTOM
            else -> mode
        }
    }
}
