package me.timschneeberger.rootlessjamesdsp.activity

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.HapticFeedbackConstants
import android.view.Menu
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.AppBarConfiguration
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.databinding.ActivityMainBinding
import me.timschneeberger.rootlessjamesdsp.databinding.ContentMainBinding
import me.timschneeberger.rootlessjamesdsp.fragment.DspFragment
import me.timschneeberger.rootlessjamesdsp.model.ProcessorMessage
import me.timschneeberger.rootlessjamesdsp.interop.JamesDspWrapper
import me.timschneeberger.rootlessjamesdsp.service.AudioProcessorService
import me.timschneeberger.rootlessjamesdsp.utils.ApplicationUtils
import me.timschneeberger.rootlessjamesdsp.utils.AssetManagerExtensions.installPrivateAssets
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.check
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.registerLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.sendLocalBroadcast
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.unregisterLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.SystemServices
import me.timschneeberger.rootlessjamesdsp.view.FloatingToggleButton
import timber.log.Timber
import java.util.*
import kotlin.concurrent.schedule


class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bindingContent: ContentMainBinding

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var capturePermissionLauncher: ActivityResultLauncher<Intent>
    private lateinit var prefsVar: SharedPreferences

    private var processorService: AudioProcessorService? = null
    private var processorServiceBound: Boolean = false

    private var mediaProjectionStartIntent: Intent? = null

    private val processorServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as AudioProcessorService.LocalBinder
            processorService = binder.service
            processorServiceBound = true

            binding.powerToggle.isToggled = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            processorService = null
            processorServiceBound = false
        }
    }

    private var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Constants.ACTION_DISCARD_AUTHORIZATION -> {
                    Timber.i("mediaProjectionStartIntent discarded")
                    mediaProjectionStartIntent = null
                }
                Constants.ACTION_SERVICE_STOPPED -> {
                    binding.powerToggle.isToggled = false
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefsVar = getSharedPreferences(Constants.PREF_VAR, Context.MODE_PRIVATE)

        val firstBoot = prefsVar.getBoolean(getString(R.string.key_firstboot), true)
        assets.installPrivateAssets(this, force = firstBoot)

        mediaProjectionManager = SystemServices.get(this, MediaProjectionManager::class.java)
        binding = ActivityMainBinding.inflate(layoutInflater)
        bindingContent = ContentMainBinding.inflate(layoutInflater)

        val check = applicationContext.check()
        if(check != 0) {
            Toast.makeText(
                this,
                "($check) Cannot launch application. Please re-download the latest version from the official GitHub project site.",
                Toast.LENGTH_LONG
            ).show()
            Timber.wtf(UnsupportedOperationException("Launch error $check; ${ApplicationUtils.describe(this)}"))
            quitGracefully()
            return
        }

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.appBarLayout.statusBarForeground = MaterialShapeDrawable.createWithElevationOverlay(this)

        // Wait for NavHostFragment to inflate
        bindingContent.dspFragmentContainer.post {
            //val navController = findNavController(R.id.dsp_fragment_container)
            //appBarConfiguration = AppBarConfiguration(navController.graph)
            //setupActionBarWithNavController(navController, appBarConfiguration)
        }

        // Load dsp settings fragment
        val fragmentManager = supportFragmentManager
        fragmentManager.beginTransaction()
            .replace(R.id.dsp_fragment_container, DspFragment.newInstance())
            .commit()

        // Check permissions and launch onboarding if required
        if(checkSelfPermission(Manifest.permission.DUMP) == PackageManager.PERMISSION_DENIED ||
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
            Timber.i("Launching onboarding (first boot: $firstBoot)")

            val onboarding = Intent(this, OnboardingActivity::class.java)
            onboarding.putExtra(OnboardingActivity.EXTRA_FIX_PERMS, !firstBoot)
            startActivity(onboarding)
            this.finish()
            return
        }

        menuInflater.inflate(R.menu.menu_main_bottom_left, binding.leftMenu.menu)
        binding.leftMenu.setOnMenuItemClickListener { arg0 ->
            if (arg0.itemId == R.id.action_settings) {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else
                false
        }

        binding.bar.inflateMenu(R.menu.menu_main_bottom)
        binding.bar.setOnMenuItemClickListener { arg0 ->
            if (arg0.itemId == R.id.action_blocklist) {
                startActivity(Intent(this, BlocklistActivity::class.java))
                true
            }
            else
                false
        }


        val filter = IntentFilter(Constants.ACTION_SERVICE_STOPPED)
        filter.addAction(Constants.ACTION_DISCARD_AUTHORIZATION)
        registerLocalReceiver(broadcastReceiver, filter)
        registerLocalReceiver(processorMessageReceiver, IntentFilter(Constants.ACTION_PROCESSOR_MESSAGE))

        binding.powerToggle.toggleOnClick = false
        binding.powerToggle.setOnToggleClickListener(object : FloatingToggleButton.OnToggleClickListener{
            override fun onClick() {

                if(binding.powerToggle.isToggled) {
                    // Currently on, let's turn it off
                    AudioProcessorService.stop(this@MainActivity)
                    binding.powerToggle.isToggled = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        binding.powerToggle.performHapticFeedback(HapticFeedbackConstants.REJECT)
                    }
                }
                else {
                    // Currently off, let's turn it on
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        binding.powerToggle.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    }
                    requestCapturePermission()
                }
            }
        })

        capturePermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                mediaProjectionStartIntent = result.data
                binding.powerToggle.isToggled = true
                AudioProcessorService.start(this, result.data)
            } else {
                binding.powerToggle.isToggled = false
            }
        }

        val mActionBar = actionBar
        mActionBar?.setDisplayHomeAsUpEnabled(true)
        mActionBar?.setHomeButtonEnabled(true)
        mActionBar?.setDisplayShowTitleEnabled(true)

        sendLocalBroadcast(Intent(Constants.ACTION_PREFERENCES_UPDATED))

        if(intent.getBooleanExtra(EXTRA_FORCE_SHOW_CAPTURE_PROMPT, false)) {
            requestCapturePermission()
        }

        // Load initial preference states
        val initialPrefList = arrayOf(R.string.key_appearance_nav_hide)
        for (pref in initialPrefList)
            this.onSharedPreferenceChanged(appPref, getString(pref))
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        sharedPreferences ?: return

        if(key == getString(R.string.key_appearance_nav_hide)) {
            binding.bar.hideOnScroll = sharedPreferences.getBoolean(key, false)
        }
        super.onSharedPreferenceChanged(sharedPreferences, key)
    }

    override fun onStart() {
        super.onStart()
        bindProcessorService()
    }

    override fun onStop() {
        super.onStop()
        unbindProcessorService()
    }

    override fun onPause() {
        super.onPause()
        prefsVar
            .edit()
            .putBoolean(getString(R.string.key_is_activity_active), false)
            .apply()
    }

    override fun onDestroy() {
        unregisterLocalReceiver(broadcastReceiver)
        unregisterLocalReceiver(processorMessageReceiver)

        try {
            if (processorService != null && processorServiceBound)
                unbindService(processorServiceConnection)
        }
        catch (_: Exception) {}

        processorService = null
        processorServiceBound = false

        prefsVar.edit().putBoolean(getString(R.string.key_is_activity_active), false)
            .apply()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        prefsVar.edit()
            .putBoolean(getString(R.string.key_is_activity_active), true)
            .apply()
        binding.powerToggle.isToggled = processorService != null
    }

    private fun bindProcessorService() {
        Intent(this, AudioProcessorService::class.java).also { intent ->
            val ret = bindService(intent, processorServiceConnection, 0)
            // Service not active
            if(!ret)
                requestCapturePermission()
        }

    }

    private fun unbindProcessorService() {
        unbindService(processorServiceConnection)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        //val navController = findNavController(R.id.dsp_fragment_container)
        return /*navController.navigateUp(appBarConfiguration)
                ||*/ super.onSupportNavigateUp()
    }

    fun requestCapturePermission() {
        if(mediaProjectionStartIntent != null) {
            binding.powerToggle.isToggled = true
            AudioProcessorService.start(this, mediaProjectionStartIntent)
            return
        }
        capturePermissionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }

    private var processorMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            when (ProcessorMessage.Type.fromInt(intent.getIntExtra(ProcessorMessage.TYPE, 0))) {
                ProcessorMessage.Type.VdcParseError -> {
                    makeSnackbar(getString(R.string.message_vdc_corrupt)).show()
                }
                ProcessorMessage.Type.LiveprogOutput -> {}
                ProcessorMessage.Type.LiveprogExec -> {}
                ProcessorMessage.Type.LiveprogResult -> {
                    val ret = intent.getIntExtra(ProcessorMessage.Param.LiveprogResultCode.name, 1)
                    if(ret <= 0)
                    {
                        val msg = JamesDspWrapper.eelErrorCodeToString(ret)
                        val msgDetail = intent.getStringExtra(ProcessorMessage.Param.LiveprogErrorMessage.name)
                        val snackbar = makeSnackbar(getString(R.string.message_liveprog_compile_fail) + " $msg")
                        if(msgDetail?.isNotBlank() == true){
                            snackbar.setAction(getString(R.string.details)) {
                                val alert = MaterialAlertDialogBuilder(this@MainActivity)
                                alert.setMessage(msgDetail)
                                alert.setTitle(msg)
                                alert.setNegativeButton(android.R.string.ok, null)
                                alert.create().show()
                            }
                        }
                        snackbar.show()
                    }
                }
                ProcessorMessage.Type.Unknown -> {}
            }
        }
    }

    private fun makeSnackbar(text: String, duration: Int = Snackbar.LENGTH_SHORT): Snackbar {
        return Snackbar.make(findViewById(android.R.id.content), text, duration)
    }

    private fun quitGracefully() {
        FirebaseCrashlytics.getInstance().sendUnsentReports()
        Timer().schedule(2000){
            this@MainActivity.finishAndRemoveTask()
        }
    }

    companion object {
        const val EXTRA_FORCE_SHOW_CAPTURE_PROMPT = "ForceShowCapturePrompt"
    }
}