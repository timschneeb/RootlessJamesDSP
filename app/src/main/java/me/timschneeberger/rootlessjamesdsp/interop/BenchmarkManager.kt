package me.timschneeberger.rootlessjamesdsp.interop

import android.content.Context
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.utils.preferences.Preferences
import me.timschneeberger.rootlessjamesdsp.utils.sdkAbove
import me.timschneeberger.rootlessjamesdsp.view.ProgressDialog
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

object BenchmarkManager : KoinComponent {
    private val varPrefs by inject<Preferences.Var>()

    fun hasBenchmarksCached(): Boolean {
        val c0 = varPrefs.get<String>(R.string.key_benchmark_c0)
        val c1 = varPrefs.get<String>(R.string.key_benchmark_c1)
        val expectedSize = JamesDspWrapper.getBenchmarkSize()
        return c0.split(';').size == expectedSize && c1.split(';').size == expectedSize
    }

    fun loadBenchmarksFromCache() {
        Timber.d("Loading benchmarks from cache")

        val c0 = varPrefs.get<String>(R.string.key_benchmark_c0)
            .split(";")
            .mapNotNull { it.toDoubleOrNull() }
            .toDoubleArray()
        val c1 = varPrefs.get<String>(R.string.key_benchmark_c1)
            .split(";")
            .mapNotNull { it.toDoubleOrNull() }
            .toDoubleArray()

        val expectedSize = JamesDspWrapper.getBenchmarkSize()
        if(c0.size != expectedSize || c1.size != expectedSize) {
            Timber.e("Benchmarks missing or malformed")
            return
        }

        JamesDspWrapper.loadBenchmark(c0, c1)
    }

    fun runBenchmarks(context: Context, onFinished: (success: Boolean) -> Unit) {
        sdkAbove(Build.VERSION_CODES.S) {
            assert(context.isUiContext)
        }

        var job: Job? = null
        val dialog = ProgressDialog(context) {
            job?.cancel()
            onFinished(false)
        }.apply {
            isIndeterminate = true
            title = context.getString(R.string.preparing)
        }
        dialog.isIndeterminate = true

        job = CoroutineScope(Dispatchers.Default).launch {
            flow {
                emit(BenchmarkState.Benchmarking)

                val size = JamesDspWrapper.getBenchmarkSize()
                val c0 = DoubleArray(size)
                val c1 = DoubleArray(size)

                JamesDspWrapper.runBenchmark(c0, c1)

                varPrefs.set(R.string.key_benchmark_c0, c0.joinToString(";"))
                varPrefs.set(R.string.key_benchmark_c1, c1.joinToString(";"))

                emit(BenchmarkState.BenchmarkDone)

                Timber.d("benchmark c0: " + c0.joinToString(";"))
                Timber.d("benchmark c1: " + c1.joinToString(";"))

            }
            .cancellable()
            .collect {
                withContext(Dispatchers.Main) {
                    when (it) {
                        BenchmarkState.BenchmarkDone -> dialog.dismiss()
                        BenchmarkState.Benchmarking -> dialog.title = context.getString(R.string.audio_format_optimization_benchmark_ongoing)
                    }
                }
            }
        }

        job.invokeOnCompletion { thr ->
            onFinished(thr == null)
        }
    }

    fun clearBenchmarks() {
        varPrefs.set(R.string.key_benchmark_c0, "")
        varPrefs.set(R.string.key_benchmark_c1, "")
    }

    sealed class BenchmarkState {
        object Benchmarking : BenchmarkState()
        object BenchmarkDone: BenchmarkState()
    }
}
