package me.timschneeberger.rootlessjamesdsp.flavor

import android.content.Context
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.flavor.updates.SessionInstaller
import me.timschneeberger.rootlessjamesdsp.flavor.updates.api.UpdateCheckClient
import me.timschneeberger.rootlessjamesdsp.flavor.updates.model.UpdateCheckResponse
import me.timschneeberger.rootlessjamesdsp.utils.ApiExtensions
import me.timschneeberger.rootlessjamesdsp.utils.Cache
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.showAlert
import me.timschneeberger.rootlessjamesdsp.utils.Result
import me.timschneeberger.rootlessjamesdsp.utils.sdkAbove
import me.timschneeberger.rootlessjamesdsp.view.ProgressDialog
import timber.log.Timber

class UpdateManager(val context: Context) {
    private val installer = SessionInstaller(context)
    private val updateClient = UpdateCheckClient(context)
    private var lastUpdateInfo: UpdateCheckResponse? = null

    fun getUpdateVersionInfo(): Pair<String, Int>? {
        return lastUpdateInfo?.let { info ->
            info.versionName?.let { Pair<String, Int>(it, info.versionCode ?: 0)  }
        }
    }

    suspend fun isUpdateAvailable(): Flow<Result<Boolean>> {
        return updateClient.checkUpdate().map {
            when(it) {
                is Result.Error -> Result.Error(it.exception)
                is Result.Success -> Result.Success(it.data != null).also { _ -> lastUpdateInfo = it.data }
                is Result.Loading -> Result.Loading
            }
        }
    }

    fun installUpdate(context: Context) {
        sdkAbove(Build.VERSION_CODES.S) {
            assert(context.isUiContext)
        }

        var job: Job? = null
        val dialog = ProgressDialog(context) { job?.cancel() }
        dialog.isIndeterminate = true

        val handleError = fun(msg: String) {
            dialog.cancel()
            context.showAlert(context.getString(R.string.self_update_install_error), msg)
        }

        job = CoroutineScope(Dispatchers.Default).launch {
            createInstallFlow().collect {
                withContext(Dispatchers.Main) {
                    when (it) {
                        is InstallState.PrepareFailed -> handleError(
                            context.getString(
                                R.string.self_update_download_fail,
                                "Data inconsistency"
                            )
                        )

                        is InstallState.Downloading -> {
                            dialog.apply {
                                isIndeterminate = false
                                title = context.getString(R.string.self_update_state_downloading)
                                unit = "MB"
                                divisor = 1e6
                                currentProgress = it.currentBytes.toInt()
                                maxProgress = it.totalBytes.toInt()
                            }
                        }

                        is InstallState.DownloadFailed -> handleError(
                            context.getString(
                                R.string.self_update_download_fail,
                                it.error?.localizedMessage ?: context.getString(R.string.unknown_error)
                            )
                        )

                        is InstallState.Installing -> {
                            dialog.apply {
                                isCancelable = false
                                isIndeterminate = true
                                title = context.getString(R.string.self_update_state_installing)
                            }
                        }

                        is InstallState.InstallDone -> dialog.dismiss()
                    }
                }
            }
        }

        job.invokeOnCompletion {
            // cleanup if download cancelled
            if(it != null)
                Cache.cleanupNow(context)
        }
    }

    private suspend fun createInstallFlow(): Flow<InstallState> {
        return flow {
            val info = lastUpdateInfo
            if(info == null) {
                Timber.e("lastUpdateInfo is null. Cannot request update download.")
                emit(InstallState.PrepareFailed)
                return@flow
            }

            val targetName = "${info.versionCode.toString()}.apk"
            if(Cache.getReleaseFile(context, targetName).exists()) {
                // Already downloaded
                emit(InstallState.Installing)
                installer.performInstall(targetName)
                emit(InstallState.InstallDone)
                return@flow
            }

            updateClient.downloadUpdate(info).collect {
                val state = when(it) {
                    is ApiExtensions.DownloadState.Downloading -> InstallState.Downloading(it)
                    is ApiExtensions.DownloadState.Failed -> InstallState.DownloadFailed(it.error)
                    else -> InstallState.Installing
                }
                emit(state)

                if(state is InstallState.Installing && it is ApiExtensions.DownloadState.Finished) {
                    installer.performInstall(it.file.name)
                    emit(InstallState.InstallDone)
                }
            }
        }
    }

    sealed class InstallState {
        object PrepareFailed : InstallState()
        data class Downloading(val progress: Int, val currentBytes: Long, val totalBytes: Long) : InstallState() {
            constructor(copy: ApiExtensions.DownloadState.Downloading) : this(copy.progress, copy.currentBytes, copy.totalBytes)
        }
        data class DownloadFailed(val error: Throwable? = null) : InstallState()
        object Installing : InstallState()
        object InstallDone: InstallState()
    }

}