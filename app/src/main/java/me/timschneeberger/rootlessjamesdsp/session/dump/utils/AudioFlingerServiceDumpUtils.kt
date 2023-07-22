package me.timschneeberger.rootlessjamesdsp.session.dump.utils

import android.content.Context
import timber.log.Timber

/**
 * Used to retrieve complementary data for AudioServiceDumpProvider
 */
object AudioFlingerServiceDumpUtils {
    class Dataset(val sid: Int, val pid: Int, val uid: Int?)
    {
        override fun toString(): String {
            return "sid=$sid; pid=$pid; uid=$uid"
        }
    }

    fun dump(context: Context): List<Dataset>? {
        val dump = DumpUtils.dumpAll(context, TARGET_SERVICE)
        dump ?: return null

        return process(dump)
    }

    private fun process(dump: String): List<Dataset>? {
        // API 29
        val tableHeadRegex = """session\s+pid\s+count""".toRegex()
        val tableBodyRegex = """(\d+)\s+(\d+)\s+(\d+)""".toRegex()
        // API 30+
        val tableHeadRegex30 = """session\s+cnt\s+pid\s+uid\s+name""".toRegex()
        val tableBodyRegex30 = """(\d+)\s+(\d+)\s+(\d+)\s+(\d+)""".toRegex()

        val dataset = mutableListOf<Dataset>()
        var tableHeadApiVersion = 29
        var headerLine = Int.MIN_VALUE
        var lastLineIsTable = false

        dump.lines().forEachIndexed { index, s ->
            if(s.contains("Global session refs"))
                headerLine = index

            // Look for table column titles after header
            if(headerLine + 1 == index)
            {
                // Just to be safe, try to auto-detect rather than relying on platform version
                tableHeadApiVersion = when {
                    tableHeadRegex30.containsMatchIn(s) -> {
                        30
                    }
                    tableHeadRegex.containsMatchIn(s) -> {
                        29
                    }
                    else -> {
                        Timber.e("Failed to determine table version. Table head: $s")
                        return null
                    }
                }
                Timber.d("Table version $tableHeadApiVersion")

                lastLineIsTable = true
            }
            else if(lastLineIsTable)
            {
                try {
                    when(tableHeadApiVersion){
                        29 -> {
                            val match = tableBodyRegex.find(s)
                            if(match != null){

                                val sid = match.groups[1]?.value?.toInt()
                                val pid = match.groups[2]?.value?.toInt()
                                if (sid != null && pid != null) {
                                    dataset.add(Dataset(sid, pid, null))
                                } else {
                                    throw IndexOutOfBoundsException("sid=$sid; pid=$pid")
                                }

                            }
                            else{
                                Timber.i("Unmatched table body pattern. Line: $s")
                                lastLineIsTable = false
                            }
                        }
                        30 -> {
                            val match = tableBodyRegex30.find(s)
                            if(match != null){
                                val sid = match.groups[1]?.value?.toIntOrNull()
                                val pid = match.groups[3]?.value?.toIntOrNull()
                                val uid = match.groups[4]?.value?.toIntOrNull()
                                if(sid != null && pid != null)
                                {
                                    dataset.add(Dataset(sid, pid, uid))
                                }
                                else
                                {
                                    throw IndexOutOfBoundsException("sid=$sid; uid=$uid; pid=$pid")
                                }
                            }
                            else{
                                Timber.i("Unmatched table body pattern. Line: $s")
                                lastLineIsTable = false
                            }
                        }
                    }
                }
                catch (ex: IndexOutOfBoundsException) {
                    Timber.e("Incomplete table body pattern match. Line: $s")
                    Timber.e(ex)
                }
            }
        }

        Timber.d("Dump processed")
        return dataset
    }

    fun dumpString(context: Context): String {
        val dump = DumpUtils.dumpAll(context, TARGET_SERVICE)
        val sb = StringBuilder("=====> $TARGET_SERVICE raw dump\n")
        sb.append(dump)
        sb.append("\n\n")
        sb.append("=====> $TARGET_SERVICE processed dump\n")
        process(dump ?: "")?.forEach {
            sb.append("$it\n")
        }
        return sb.toString()
    }

    const val TARGET_SERVICE = "media.audio_flinger"
}