package me.timschneeberger.rootlessjamesdsp.activity

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.service.RootAudioProcessorService
import me.timschneeberger.rootlessjamesdsp.service.RootlessAudioProcessorService
import me.timschneeberger.rootlessjamesdsp.utils.SystemServices
import timber.log.Timber

/**
 * Helper activity to launch the rootless foreground service
 * from the TileService
 */
class EngineLauncherActivity : BaseActivity() {
    private lateinit var capturePermissionLauncher: ActivityResultLauncher<Intent>

    override val disableAppTheme: Boolean = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!BuildConfig.ROOTLESS) {
            // Root
            RootAudioProcessorService.startServiceEnhanced(this)
            finish()
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            return

        // If projection token available, start immediately
        if(app.mediaProjectionStartIntent != null) {
            Timber.d("Reusing old projection token to start service")
            RootlessAudioProcessorService.start(this, app.mediaProjectionStartIntent)
            return
        }

        setFinishOnTouchOutside(false)

        capturePermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                app.mediaProjectionStartIntent = result.data
                Timber.d("Using new projection token to start service")

                RootlessAudioProcessorService.start(this, result.data)
            }
            finish()
        }

        SystemServices.get(this, MediaProjectionManager::class.java)
            .createScreenCaptureIntent()
            .let(capturePermissionLauncher::launch)
    }
}