package me.timschneeberger.rootlessjamesdsp.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.*
import android.view.HapticFeedbackConstants
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.preference.DialogPreference.TargetFragment
import androidx.preference.Preference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.MainApplication
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.databinding.ActivityMainBinding
import me.timschneeberger.rootlessjamesdsp.databinding.ContentMainBinding
import me.timschneeberger.rootlessjamesdsp.fragment.DspFragment
import me.timschneeberger.rootlessjamesdsp.fragment.FileLibraryDialogFragment
import me.timschneeberger.rootlessjamesdsp.fragment.LibraryLoadErrorFragment
import me.timschneeberger.rootlessjamesdsp.interop.JamesDspRemoteEngine
import me.timschneeberger.rootlessjamesdsp.interop.JamesDspWrapper
import me.timschneeberger.rootlessjamesdsp.model.ProcessorMessage
import me.timschneeberger.rootlessjamesdsp.preference.FileLibraryPreference
import me.timschneeberger.rootlessjamesdsp.service.BaseAudioProcessorService
import me.timschneeberger.rootlessjamesdsp.service.RootlessAudioProcessorService
import me.timschneeberger.rootlessjamesdsp.service.RootAudioProcessorService
import me.timschneeberger.rootlessjamesdsp.utils.ApplicationUtils
import me.timschneeberger.rootlessjamesdsp.utils.AssetManagerExtensions.installPrivateAssets
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.check
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.registerLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.requestIgnoreBatteryOptimizations
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.sendLocalBroadcast
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.unregisterLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.SystemServices
import me.timschneeberger.rootlessjamesdsp.view.FloatingToggleButton
import timber.log.Timber
import java.util.*
import kotlin.concurrent.schedule


class MainActivity : BaseActivity() {
    /* UI bindings */
    lateinit var binding: ActivityMainBinding
    private lateinit var bindingContent: ContentMainBinding

    /* Rootless version */
    private var mediaProjectionStartIntent: Intent? = null
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var capturePermissionLauncher: ActivityResultLauncher<Intent>

    /* Root version */
    private var hasLoadFailed = false
    private lateinit var runtimePermissionLauncher: ActivityResultLauncher<Array<String>>

    /* General */
    private val prefsVar by lazy { getSharedPreferences(Constants.PREF_VAR, Context.MODE_PRIVATE) }

    private var processorService: BaseAudioProcessorService? = null
    private var processorServiceBound: Boolean = false

    private val processorServiceConnection by lazy {
        object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, service: IBinder) {
                Timber.d("Service connected")

                processorService = (service as BaseAudioProcessorService.LocalBinder).service
                processorServiceBound = true

                if (BuildConfig.ROOTLESS)
                    binding.powerToggle.isToggled = true
            }

            override fun onServiceDisconnected(arg0: ComponentName) {
                Timber.d("Service disconnected")

                processorService = null
                processorServiceBound = false
            }
        }
    }

    private var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Constants.ACTION_DISCARD_AUTHORIZATION -> {
                    if(BuildConfig.ROOTLESS) {
                        Timber.i("mediaProjectionStartIntent discarded")
                        mediaProjectionStartIntent = null
                    }
                }
                Constants.ACTION_SERVICE_STOPPED -> {
                    if(BuildConfig.ROOTLESS)
                        binding.powerToggle.isToggled = false
                }
            }
        }
    }

    @SuppressLint("BatteryLife")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.let {
            hasLoadFailed = it.getBoolean(STATE_LOAD_FAILED)
        }

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

        // Setup views
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        actionBar?.setDisplayHomeAsUpEnabled(true)
        actionBar?.setHomeButtonEnabled(true)
        actionBar?.setDisplayShowTitleEnabled(true)
        binding.appBarLayout.statusBarForeground = MaterialShapeDrawable.createWithElevationOverlay(this)

        // Load main fragment
        if(!hasLoadFailed)
            supportFragmentManager.beginTransaction()
                .replace(R.id.dsp_fragment_container, DspFragment.newInstance())
                .commit()
        else
            showLibraryLoadError()

        // Rootless: Check permissions and launch onboarding if required
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            BuildConfig.ROOTLESS &&
            (checkSelfPermission(Manifest.permission.DUMP) == PackageManager.PERMISSION_DENIED ||
                    checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED)) {
            Timber.i("Launching onboarding (first boot: $firstBoot)")

            val onboarding = Intent(this, OnboardingActivity::class.java)
            onboarding.putExtra(OnboardingActivity.EXTRA_FIX_PERMS, !firstBoot)
            startActivity(onboarding)
            this.finish()
            return
        }

        // Inflate bottom left menu
        menuInflater.inflate(R.menu.menu_main_bottom_left, binding.leftMenu.menu)
        binding.leftMenu.setOnMenuItemClickListener { arg0 ->
            if (arg0.itemId == R.id.action_settings) {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else
                false
        }

        // Inflate bottom right menu
        binding.bar.inflateMenu(R.menu.menu_main_bottom)

        val actBlocklist = binding.bar.menu.findItem(R.id.action_blocklist)
        val actPresets = binding.bar.menu.findItem(R.id.action_presets)

        if(BuildConfig.ROOTLESS) {
            actBlocklist.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER or MenuItem.SHOW_AS_ACTION_WITH_TEXT)
            actPresets.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER or MenuItem.SHOW_AS_ACTION_WITH_TEXT)
        }

        // TODO root: add support for app exclusion list
        actBlocklist.isVisible = BuildConfig.ROOTLESS

        binding.bar.setOnMenuItemClickListener { arg0 ->
            when (arg0.itemId) {
                R.id.action_blocklist -> {
                    startActivity(Intent(this, BlocklistActivity::class.java))
                    true
                }
                R.id.action_presets -> {
                    val dialogFragment = FileLibraryDialogFragment.newInstance("presets")
                    dialogFragment.setTargetFragment(presetDialogHost, 0)
                    dialogFragment.show(supportFragmentManager, null)
                    true
                }
                else -> false
            }
        }

        val filter = IntentFilter(Constants.ACTION_SERVICE_STOPPED)
        filter.addAction(Constants.ACTION_DISCARD_AUTHORIZATION)
        registerLocalReceiver(broadcastReceiver, filter)
        registerLocalReceiver(processorMessageReceiver, IntentFilter(Constants.ACTION_PROCESSOR_MESSAGE))

        // Rootless: don't toggle on click, we handle that in the onClickListener
        binding.powerToggle.toggleOnClick = false
        binding.powerToggle.setOnToggleClickListener(object : FloatingToggleButton.OnToggleClickListener{
            override fun onClick() {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && BuildConfig.ROOTLESS) {
                    if (binding.powerToggle.isToggled) {
                        // Currently on, let's turn it off
                        RootlessAudioProcessorService.stop(this@MainActivity)
                        binding.powerToggle.isToggled = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            binding.powerToggle.performHapticFeedback(HapticFeedbackConstants.REJECT)
                        }
                    } else {
                        // Currently off, let's turn it on
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            binding.powerToggle.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        }
                        requestCapturePermission()
                    }
                }
                else if (!BuildConfig.ROOTLESS && JamesDspRemoteEngine.isPluginInstalled()) {
                    binding.powerToggle.isToggled = !binding.powerToggle.isToggled
                    appPref
                        .edit()
                        .putBoolean(getString(R.string.key_powered_on), binding.powerToggle.isToggled)
                        .apply()
                }
            }
        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && BuildConfig.ROOTLESS) {
            capturePermissionLauncher = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == RESULT_OK) {
                    mediaProjectionStartIntent = result.data
                    binding.powerToggle.isToggled = true
                    RootlessAudioProcessorService.start(this, result.data)
                } else {
                    binding.powerToggle.isToggled = false
                }
            }
        }

        // Rootless: request capture permission instantly, if redirected from onboarding
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && BuildConfig.ROOTLESS) {
            if (intent.getBooleanExtra(EXTRA_FORCE_SHOW_CAPTURE_PROMPT, false)) {
                requestCapturePermission()
            }
        }

        // Root: show error if plugin unavailable
        if(!BuildConfig.ROOTLESS && !JamesDspRemoteEngine.isPluginInstalled()) {
            showLibraryLoadError()
        }

        /* Root: require battery optimizations turned off when legacy mode is disabled,
           otherwise, the service will be block from launching from background */
        if (!BuildConfig.ROOTLESS && !(application as MainApplication).isLegacyMode) {
            requestIgnoreBatteryOptimizations()
        }

        // Root: request notification permission on Android 13 because the onboarding is not used for root
        if (!BuildConfig.ROOTLESS && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runtimePermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { isGranted ->
                if (isGranted.all { it.value })
                    Timber.d("All requested runtime permissions granted")
            }
            runtimePermissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
        }

        // Load initial preference states
        val initialPrefList = arrayOf(R.string.key_appearance_nav_hide, R.string.key_powered_on)
        for (pref in initialPrefList)
            this.onSharedPreferenceChanged(appPref, getString(pref))

        sendLocalBroadcast(Intent(Constants.ACTION_PREFERENCES_UPDATED))
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        sharedPreferences ?: return

        if(key == getString(R.string.key_appearance_nav_hide)) {
            binding.bar.hideOnScroll = sharedPreferences.getBoolean(key, false)
        }
        else if(key == getString(R.string.key_powered_on) && !hasLoadFailed && !BuildConfig.ROOTLESS) {
            binding.powerToggle.isToggled = sharedPreferences.getBoolean(key, true)
        }
        super.onSharedPreferenceChanged(sharedPreferences, key)
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState.apply {
            putBoolean(STATE_LOAD_FAILED, hasLoadFailed)
        })
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

        if(BuildConfig.ROOTLESS)
            binding.powerToggle.isToggled = processorService != null
    }

    private fun showLibraryLoadError() {
        hasLoadFailed = true

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.dsp_fragment_container, LibraryLoadErrorFragment.newInstance())
            .commit()

        binding.powerToggle.isToggled = false
        binding.toolbar.isVisible = false
    }

    private fun bindProcessorService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && BuildConfig.ROOTLESS) {
            Intent(this, RootlessAudioProcessorService::class.java).also { intent ->
                val ret = bindService(intent, processorServiceConnection, 0)
                // Service not active
                if(!ret)
                    requestCapturePermission()
            }
        }
        else if (!BuildConfig.ROOTLESS) {
            Intent(this, RootAudioProcessorService::class.java).also { intent ->
                bindService(intent, processorServiceConnection, 0)
            }
        }
    }

    private fun unbindProcessorService() {
        try {
            unbindService(processorServiceConnection)
        }
        catch (ex: IllegalArgumentException) {
            Timber.d("Failed to unbind service connection. Not registered?")
            Timber.i(ex)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun requestCapturePermission() {
        if(mediaProjectionStartIntent != null) {
            binding.powerToggle.isToggled = true
            RootlessAudioProcessorService.start(this, mediaProjectionStartIntent)
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
                else -> {}
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

    private val presetDialogHost by lazy {
        @Suppress("UNCHECKED_CAST")
        class FakePresetFragment : Fragment(), TargetFragment {
            val pref by lazy {
                FileLibraryPreference(this@MainActivity, null).apply {
                    this.type = "Presets"
                    this.key = "presets"
                }
            }
            override fun <T : Preference?> findPreference(key: CharSequence): T? {
                return pref as? T
            }
        }

        val fragment = FakePresetFragment()
        supportFragmentManager.beginTransaction()
            .add(R.id.dsp_fragment_container, fragment)
            .commitNow()
        fragment
    }

    companion object {
        const val EXTRA_FORCE_SHOW_CAPTURE_PROMPT = "ForceShowCapturePrompt"

        private const val STATE_LOAD_FAILED = "LoadFailed"
    }
}