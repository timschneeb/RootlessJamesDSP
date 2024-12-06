package me.timschneeberger.rootlessjamesdsp.activity

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.getSystemService
import me.timschneeberger.rootlessjamesdsp.service.RootAudioProcessorService
import me.timschneeberger.rootlessjamesdsp.service.RootlessAudioProcessorService
import me.timschneeberger.rootlessjamesdsp.utils.SdkCheck
import me.timschneeberger.rootlessjamesdsp.utils.isRoot
import me.timschneeberger.rootlessjamesdsp.utils.sdkAbove
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

        if (isRoot()) {
            // Root
            RootAudioProcessorService.startServiceEnhanced(this)
            finish()
            return
        }

        sdkAbove(Build.VERSION_CODES.Q) {
            // If projection token available, start immediately
            // Note: Android >=14 doesn't allow token reuse
            if(app.mediaProjectionStartIntent != null && !SdkCheck.isUpsideDownCake) {
                Timber.d("Reusing old projection token to start service")
                RootlessAudioProcessorService.start(this, app.mediaProjectionStartIntent)
                finish()
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

            getSystemService<MediaProjectionManager>()
                ?.createScreenCaptureIntent()
                ?.let(capturePermissionLauncher::launch)
        }
    }
}