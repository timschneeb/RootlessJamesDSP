package me.timschneeberger.rootlessjamesdsp.activity

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.service.RootlessAudioProcessorService
import me.timschneeberger.rootlessjamesdsp.utils.SystemServices

/**
 * Helper activity to launch the rootless foreground service
 * from the TileService
 */
class EngineLauncherActivity : BaseActivity() {
    private var mediaProjectionStartIntent: Intent? = null
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var capturePermissionLauncher: ActivityResultLauncher<Intent>

    override val disableAppTheme: Boolean = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || !BuildConfig.ROOTLESS) {
            finish()
            return
        }

        mediaProjectionManager = SystemServices.get(this, MediaProjectionManager::class.java)

        setFinishOnTouchOutside(false)

        capturePermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK && BuildConfig.ROOTLESS) {
                mediaProjectionStartIntent = result.data

                RootlessAudioProcessorService.start(this, result.data)
            }
            this.finish()
        }

        capturePermissionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }
}