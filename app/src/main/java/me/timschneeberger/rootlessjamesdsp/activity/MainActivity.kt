package me.timschneeberger.rootlessjamesdsp.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PersistableBundle
import android.view.HapticFeedbackConstants
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.preference.DialogPreference.TargetFragment
import androidx.preference.Preference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.MainApplication
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.databinding.ActivityDspMainBinding
import me.timschneeberger.rootlessjamesdsp.databinding.ContentMainBinding
import me.timschneeberger.rootlessjamesdsp.flavor.CrashlyticsImpl
import me.timschneeberger.rootlessjamesdsp.flavor.UpdateManager
import me.timschneeberger.rootlessjamesdsp.fragment.DspFragment
import me.timschneeberger.rootlessjamesdsp.fragment.FileLibraryDialogFragment
import me.timschneeberger.rootlessjamesdsp.fragment.LibraryLoadErrorFragment
import me.timschneeberger.rootlessjamesdsp.interop.JamesDspRemoteEngine
import me.timschneeberger.rootlessjamesdsp.interop.JamesDspWrapper
import me.timschneeberger.rootlessjamesdsp.model.ProcessorMessage
import me.timschneeberger.rootlessjamesdsp.model.preset.Preset
import me.timschneeberger.rootlessjamesdsp.preference.FileLibraryPreference
import me.timschneeberger.rootlessjamesdsp.service.BaseAudioProcessorService
import me.timschneeberger.rootlessjamesdsp.service.RootAudioProcessorService
import me.timschneeberger.rootlessjamesdsp.service.RootlessAudioProcessorService
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.Result
import me.timschneeberger.rootlessjamesdsp.utils.SdkCheck
import me.timschneeberger.rootlessjamesdsp.utils.extensions.AssetManagerExtensions.installPrivateAssets
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.broadcastPresetLoadEvent
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.check
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.getAppName
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.registerLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.requestIgnoreBatteryOptimizations
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.restoreDspSettings
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.sendLocalBroadcast
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.showAlert
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.showSingleChoiceAlert
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.showYesNoAlert
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.toast
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.unregisterLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.extensions.PermissionExtensions.hasDumpPermission
import me.timschneeberger.rootlessjamesdsp.utils.extensions.PermissionExtensions.hasProjectMediaAppOp
import me.timschneeberger.rootlessjamesdsp.utils.extensions.PermissionExtensions.hasRecordPermission
import me.timschneeberger.rootlessjamesdsp.utils.isPlugin
import me.timschneeberger.rootlessjamesdsp.utils.isRoot
import me.timschneeberger.rootlessjamesdsp.utils.isRootless
import me.timschneeberger.rootlessjamesdsp.utils.sdkAbove
import me.timschneeberger.rootlessjamesdsp.utils.storage.StorageUtils
import me.timschneeberger.rootlessjamesdsp.view.FloatingToggleButton
import org.koin.core.component.inject
import timber.log.Timber
import java.io.File
import java.util.Timer
import kotlin.concurrent.schedule


class MainActivity : BaseActivity() {
    /* UI bindings */
    lateinit var binding: ActivityDspMainBinding
    private lateinit var bindingContent: ContentMainBinding
    private lateinit var dspFragment: DspFragment

    /* Rootless version */
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var capturePermissionLauncher: ActivityResultLauncher<Intent>

    /* Root version */
    private var hasLoadFailed = false
    private lateinit var runtimePermissionLauncher: ActivityResultLauncher<Array<String>>
    private val updateManager: UpdateManager by inject()

    private var processorService: BaseAudioProcessorService? = null
    private var processorServiceBound: Boolean = false

    private val processorServiceConnection by lazy {
        object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, service: IBinder) {
                Timber.d("Service connected")

                processorService = (service as BaseAudioProcessorService.LocalBinder).service
                processorServiceBound = true

                if (isRootless())
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
                Constants.ACTION_SERVICE_STOPPED -> {
                    if(isRootless())
                        binding.powerToggle.isToggled = false
                }
                Constants.ACTION_SERVICE_STARTED -> {
                    if(isRootless())
                        binding.powerToggle.isToggled = true
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

        val firstBoot = prefsVar.get<Boolean>(R.string.key_first_boot)
        assets.installPrivateAssets(this, force = firstBoot)

        mediaProjectionManager = getSystemService<MediaProjectionManager>()!!
        binding = ActivityDspMainBinding.inflate(layoutInflater)
        bindingContent = ContentMainBinding.inflate(layoutInflater)

        val check = applicationContext.check()
        if(check != 0) {
            toast("($check) Cannot launch application. Please re-download the latest version from the Google Play or the official GitHub project site.")
            Timber.e(UnsupportedOperationException("Launch error $check; package=${packageName}; app_name=${getAppName()}"))
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

        if(firstBoot) {
            // Set timer for translation notice (+30m)
            prefsVar.set(R.string.key_snooze_translation_notice, (System.currentTimeMillis() / 1000L) + 1800L)
        }

        // Load main fragment
        dspFragment = DspFragment.newInstance()
        if(!hasLoadFailed)
            supportFragmentManager.beginTransaction()
                .replace(R.id.dsp_fragment_container, dspFragment)
                .commit()
        else
            showLibraryLoadError()

        // Rootless: Check permissions and launch onboarding if required
        if(SdkCheck.isQ && isRootless() && (!hasDumpPermission() || !hasRecordPermission())) {
            Timber.i("Launching onboarding (first boot: $firstBoot)")

            startActivity(Intent(this, OnboardingActivity::class.java).apply {
                putExtra(OnboardingActivity.EXTRA_FIX_PERMS, !firstBoot)
            })
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

        if(isPlugin() || (isRoot() && !app.isEnhancedProcessing))
            binding.bar.menu.removeItem(R.id.action_blocklist)

        binding.bar.setOnMenuItemClickListener { arg0 ->
            when (arg0.itemId) {
                R.id.action_blocklist -> {
                    if(!app.isEnhancedProcessing && isRoot()) {
                        showAlert(
                            R.string.enhanced_processing_feature_unavailable,
                            R.string.enhanced_processing_feature_unavailable_content
                        )
                    }
                    else
                        startActivity(Intent(this, BlocklistActivity::class.java))
                    true
                }
                R.id.action_presets -> {
                    if (presetDialogHost == null) {
                        Timber.d("Initialize preset dialog host")
                        presetDialogHost = FakePresetFragment.newInstance()
                        supportFragmentManager.beginTransaction()
                            .add(R.id.dsp_fragment_container, presetDialogHost!!)
                            .commitNow()
                    }
                    presetDialogHost?.pref?.refresh()

                    val dialogFragment = FileLibraryDialogFragment.newInstance("presets")
                    @Suppress("DEPRECATION")
                    dialogFragment.setTargetFragment(presetDialogHost, 0)
                    dialogFragment.show(supportFragmentManager, null)
                    true
                }
                R.id.action_revert -> {
                    this.showYesNoAlert(
                        R.string.revert_confirmation_title,
                        R.string.revert_confirmation
                    ) {
                        if(it)
                            restoreDspSettings()
                    }
                    true
                }
                else -> false
            }
        }

        IntentFilter(Constants.ACTION_SERVICE_STOPPED).apply {
            addAction(Constants.ACTION_SERVICE_STARTED)
            addAction(Constants.ACTION_PRESET_LOADED)
            registerLocalReceiver(broadcastReceiver, this)
        }
        registerLocalReceiver(processorMessageReceiver, IntentFilter(Constants.ACTION_PROCESSOR_MESSAGE))

        // Rootless: don't toggle on click, we handle that in the onClickListener
        binding.powerToggle.toggleOnClick = false
        binding.powerToggle.setOnToggleClickListener(object : FloatingToggleButton.OnToggleClickListener{
            override fun onClick() {
                sdkAbove(Build.VERSION_CODES.R) {
                    binding.powerToggle.performHapticFeedback(
                        if(binding.powerToggle.isToggled)
                            HapticFeedbackConstants.CONFIRM
                        else
                            HapticFeedbackConstants.REJECT
                    )
                }

                if(SdkCheck.isQ && isRootless()) {
                    if (binding.powerToggle.isToggled) {
                        // Currently on, let's turn it off
                        RootlessAudioProcessorService.stop(this@MainActivity)
                        binding.powerToggle.isToggled = false
                    } else {
                        // Currently off, let's turn it on
                        requestCapturePermission()
                    }
                }
                else if (isRoot()) {
                    when(JamesDspRemoteEngine.isPluginInstalled()) {
                        JamesDspRemoteEngine.PluginState.Available -> {
                            binding.powerToggle.isToggled = !binding.powerToggle.isToggled
                            prefsApp.set(R.string.key_powered_on, binding.powerToggle.isToggled)
                        }
                        JamesDspRemoteEngine.PluginState.Unsupported -> {
                            toast(getString(R.string.version_mismatch_root_toast))
                        }
                        JamesDspRemoteEngine.PluginState.Unavailable -> {
                            toast(getString(R.string.load_fail_header))
                        }
                    }
                }
                else if(isPlugin()) {
                    binding.powerToggle.isToggled = !binding.powerToggle.isToggled
                    prefsApp.set(R.string.key_powered_on, binding.powerToggle.isToggled)
                }
            }
        })

        if (SdkCheck.isQ && isRootless()) {
            capturePermissionLauncher = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == RESULT_OK && isRootless()) {
                    app.mediaProjectionStartIntent = result.data
                    binding.powerToggle.isToggled = true
                    RootlessAudioProcessorService.start(this, result.data)
                } else {
                    binding.powerToggle.isToggled = false
                }
            }
        }

        // Rootless: request capture permission instantly, if redirected from onboarding
        if (SdkCheck.isQ && isRootless()) {
            if (intent.getBooleanExtra(EXTRA_FORCE_SHOW_CAPTURE_PROMPT, false)) {
                requestCapturePermission()
            }
        }

        // Root: show error if plugin unavailable
        if(isRoot()) {
            when(JamesDspRemoteEngine.isPluginInstalled()) {
                JamesDspRemoteEngine.PluginState.Unavailable -> showLibraryLoadError()
                JamesDspRemoteEngine.PluginState.Unsupported -> {
                    prefsApp.set(R.string.key_powered_on, false)
                    showYesNoAlert(
                        getString(R.string.version_mismatch_root),
                        getString(R.string.version_mismatch_root_description)
                    ) {
                        if(it) {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://zackptg5.com/android.php")))
                        }
                    }
                }
                else -> {}
            }
        }

        /* Root: require battery optimizations turned off when legacy mode is disabled,
           otherwise, the service will be block from launching from background */
        if (isRoot() && !(application as MainApplication).isLegacyMode) {
            requestIgnoreBatteryOptimizations()
        }

        // Root: request notification permission on Android 13 because the onboarding is not used for root
        if (isRoot() && SdkCheck.isTiramisu) {
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
            this.onSharedPreferenceChanged(prefsApp.preferences, getString(pref))

        sendLocalBroadcast(Intent(Constants.ACTION_PREFERENCES_UPDATED))

        if(isRoot() && app.isEnhancedProcessing) {
            if(!hasDumpPermission()) {
                Timber.e("Dump permission for enhanced processing lost")
                toast(getString(R.string.enhanced_processing_missing_perm))
                prefsApp.set(R.string.key_audioformat_enhanced_processing, false)
            }
            else {
                Timber.d("Launching service due to enhanced processing")
                RootAudioProcessorService.startServiceEnhanced(this)
            }
        }

        showAndroid15Alert()

        dspFragment.setUpdateCardOnClick { updateManager.installUpdate(this) }
        dspFragment.setUpdateCardOnCloseClick(::dismissUpdate)
        checkForUpdates()

        // Handle potential incoming file intent
        if(intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let { handleFileIntent(it) }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if(key == getString(R.string.key_appearance_nav_hide)) {
            binding.bar.hideOnScroll = prefsApp.get(R.string.key_appearance_nav_hide)
        }
        else if(key == getString(R.string.key_powered_on) && !hasLoadFailed && !isRootless()) {
            binding.powerToggle.isToggled = prefsApp.get(R.string.key_powered_on)
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
        prefsVar.set(R.string.key_is_activity_active, false)
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

        prefsVar.set(R.string.key_is_activity_active, false)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        prefsVar.set(R.string.key_is_activity_active, true)

        if(isRootless())
            binding.powerToggle.isToggled = processorService != null
    }

    private fun showAndroid15Alert() {
        if(!isRootless() || !SdkCheck.isVanillaIceCream)
            return

        if(!hasProjectMediaAppOp()) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.android_15_screenshare_warning_title)
                .setMessage(R.string.android_15_screenshare_keyguard_warning)
                .setCancelable(false)
                .setPositiveButton(R.string.continue_action) { _, _ ->
                    startActivity(Intent(this, OnboardingActivity::class.java).apply {
                        putExtra(OnboardingActivity.EXTRA_FIX_PERMS, false)
                    })
                    this.finish()
                }
                .show()
            return
        }

        if(prefsVar.get<Boolean>(R.string.key_android15_screenrecord_restriction_seen))
            return

        val checkBox = CheckBox(this).apply {
            text = getString(R.string.never_show_warning_again)
            isChecked = false
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.android_15_screenshare_warning_title)
            .setMessage(R.string.android_15_screenshare_warning)
            .setView(
                LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(48, 32, 48, 16)
                    addView(checkBox)
                }
            )
            .setPositiveButton(R.string.tutorial) { dialog, _ ->
                prefsVar.set(R.string.key_android15_screenrecord_restriction_seen, checkBox.isChecked)
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://youtu.be/rVM13aY2rwU?t=31")))
            }
            .setNegativeButton(R.string.close) { dialog, _ ->
                prefsVar.set(R.string.key_android15_screenrecord_restriction_seen, checkBox.isChecked)
                dialog.dismiss()
            }
            .show()
    }

    private fun checkForUpdates() {
        if(!isRoot() ||
            prefsVar.get<Long>(R.string.key_update_check_timeout) > (System.currentTimeMillis() / 1000L)) {
            Timber.d("Update check rejected due to flavor or timeout")
            return
        }

        CoroutineScope(Dispatchers.Default).launch {
            updateManager.isUpdateAvailable().collect {
                when(it) {
                    is Result.Error -> {
                        Timber.e("Update check failed")
                        Timber.d(it.exception)
                        // Set timeout to +30min
                        prefsVar.set(R.string.key_update_check_timeout, (System.currentTimeMillis() / 1000L) + 1800L)
                        false
                    }
                    is Result.Success -> {
                        Timber.d("Is update available? ${it.data}")
                        if(!it.data) {
                            // Set timeout to +4h
                            prefsVar.set(R.string.key_update_check_timeout, (System.currentTimeMillis() / 1000L) + 14400L)
                        }
                        it.data
                    }
                    else -> false
                }.let {
                    withContext(Dispatchers.Main) {
                        val info = updateManager.getUpdateVersionInfo()
                        val skipUpdate = info?.second == prefsVar.get<Int>(R.string.key_update_check_skip)
                        Timber.d("Should skip update ${info?.second}?: $skipUpdate")
                        if(skipUpdate) {
                            // Set timeout to +4h
                            prefsVar.set(R.string.key_update_check_timeout, (System.currentTimeMillis() / 1000L) + 14400L)
                        }
                        dspFragment.setUpdateCardTitle(getString(R.string.self_update_notice, info?.first ?: "..."))
                        dspFragment.setUpdateCardVisible(it && !skipUpdate)
                    }
                }
            }
        }
    }

    private fun dismissUpdate() {
        if(!isRoot())
            return

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.actions))
            .setItems(R.array.update_dismiss_dialog) { dialogInterface, i ->
                when(i) {
                    0 -> updateManager.installUpdate(this)
                    1 -> {
                        prefsVar.set(R.string.key_update_check_skip, updateManager.getUpdateVersionInfo()?.second ?: 0)
                        dspFragment.setUpdateCardVisible(false)
                    }
                    2 -> {
                        prefsVar.set(
                            R.string.key_update_check_timeout,
                            (System.currentTimeMillis() / 1000L) + 43200L /* +12h snooze */
                        )
                        dspFragment.setUpdateCardVisible(false)
                    }
                }
                dialogInterface.dismiss()
            }
            .setNegativeButton(getString(android.R.string.cancel)){ _, _ -> }
            .create()
            .show()
    }

    private fun showLibraryLoadError() {
        if(DEBUG_IGNORE_MISSING_LIBRARY)
            return

        hasLoadFailed = true

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.dsp_fragment_container, LibraryLoadErrorFragment.newInstance())
            .commit()

        binding.powerToggle.isToggled = false
        binding.toolbar.isVisible = false
    }

    private fun bindProcessorService() {
        if (isRootless()) {
            sdkAbove(Build.VERSION_CODES.Q) {
                Intent(this, RootlessAudioProcessorService::class.java).also { intent ->
                    val ret = bindService(intent, processorServiceConnection, 0)
                    // Service not active
                    if (!ret)
                        requestCapturePermission()
                }
            }
        }
        else if (isRoot()) {
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

    @RequiresApi(Build.VERSION_CODES.Q)
    fun requestCapturePermission() {
        if(app.mediaProjectionStartIntent != null && isRootless() && !SdkCheck.isVanillaIceCream) {
            binding.powerToggle.isToggled = true
            RootlessAudioProcessorService.start(this, app.mediaProjectionStartIntent)
            return
        }
        try {
            capturePermissionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
        }
        catch (ex: ActivityNotFoundException) {
            toast(getString(R.string.error_projection_api_missing))
            Timber.e(ex)
        }
    }

    private var processorMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            when (ProcessorMessage.Type.fromInt(intent.getIntExtra(ProcessorMessage.TYPE, 0))) {
                ProcessorMessage.Type.VdcParseError -> {
                    makeSnackbar(getString(R.string.message_vdc_corrupt)).show()
                }
                ProcessorMessage.Type.ConvolverParseError -> {
                    when(ProcessorMessage.ConvolverErrorCode.fromInt(
                        intent.getIntExtra(ProcessorMessage.Param.ConvolverErrorCode.name, 0)
                    )) {
                        ProcessorMessage.ConvolverErrorCode.Corrupted -> R.string.message_irs_corrupt
                        ProcessorMessage.ConvolverErrorCode.AdvParamsInvalid -> R.string.message_convolver_advimp_invalid
                        else -> null
                    }?.let {
                        makeSnackbar(getString(it)).show()
                    }
                }
                ProcessorMessage.Type.LiveprogOutput -> {}
                ProcessorMessage.Type.LiveprogExec -> {}
                ProcessorMessage.Type.LiveprogResult -> {
                    val ret = intent.getIntExtra(ProcessorMessage.Param.LiveprogResultCode.name, 1)
                    if(ret <= 0)
                    {
                        val msg = JamesDspWrapper.eelErrorCodeToString(ret)
                        makeSnackbar(getString(R.string.message_liveprog_compile_fail) + " ($msg)").show()
                    }
                }
                else -> {}
            }
        }
    }

    private fun handleFileIntent(uri: Uri) {
        val name = StorageUtils.queryName(this, uri)
        if (name == null) {
            toast(getString(R.string.intent_import_error_file_uri))
            return
        }

        val choices = arrayOf<CharSequence>(
            getString(R.string.intent_import_mode_add),
            getString(R.string.intent_import_mode_select)
        )

        val titleRes: Int?
        val subDir: String?
        var namespace: String? = null
        var key: Int? = null
        var keyEnable: Int? = null
        when {
            name.endsWith(".tar") -> {
                // Validate presets
                StorageUtils.openInputStreamSafe(this, uri)?.use {
                    if (!Preset.validate(it)) {
                        Timber.e("File rejected due to invalid content")
                        showAlert(R.string.filelibrary_corrupted_title,
                            R.string.filelibrary_corrupted)
                        return
                    }
                }

                titleRes = R.string.intent_import_preset
                subDir = "Presets"
            }
            name.endsWith(".eel") -> {
                titleRes = R.string.intent_import_liveprog
                subDir = "Liveprog"
                namespace = Constants.PREF_LIVEPROG
                key = R.string.key_liveprog_file
                keyEnable = R.string.key_liveprog_enable
            }
            name.endsWith(".vdc") -> {
                titleRes = R.string.intent_import_vdc
                subDir = "DDC"
                namespace = Constants.PREF_DDC
                key = R.string.key_ddc_file
                keyEnable = R.string.key_ddc_enable
            }
            name.endsWith(".irs") || name.endsWith(".wav") -> {
                titleRes = R.string.intent_import_irs
                subDir = "Convolver"
                namespace = Constants.PREF_CONVOLVER
                key = R.string.key_convolver_file
                keyEnable = R.string.key_convolver_enable
            }
            else -> return
        }

        showSingleChoiceAlert(titleRes, choices, -1) { idx ->
            idx ?: return@showSingleChoiceAlert
            if (idx < 0 || idx > 1)
                return@showSingleChoiceAlert

            val file = StorageUtils.importFile(
                this,
                File(getExternalFilesDir(null), subDir).absolutePath,
                uri
            )

            if (file == null) {
                Timber.w("Failed to import file '$uri'")
                makeSnackbar(getString(R.string.intent_import_fail, name)).show()
                return@showSingleChoiceAlert
            }

            when (idx) {
                0 -> makeSnackbar(getString(R.string.intent_import_success, name)).show()
                1 -> {
                    CoroutineScope(Dispatchers.Default).launch {
                        delay(250L)

                        if (name.endsWith(".tar")) {
                            try {
                                StorageUtils.openInputStreamSafe(this@MainActivity, uri)?.use {
                                    Preset.load(this@MainActivity, it)
                                }
                            }
                            catch (ex: Exception) {
                                showAlert(getString(R.string.filelibrary_corrupted_title), ex.localizedMessage ?: "")
                            }
                        }
                        else if (namespace != null && key != null && keyEnable != null)
                            @Suppress("DEPRECATION")
                            getSharedPreferences(namespace, MODE_MULTI_PROCESS)
                                .edit()
                                .putBoolean(getString(keyEnable), true)
                                .putString(getString(key), file.absolutePath)
                                .apply()

                        delay(250L)
                        broadcastPresetLoadEvent()

                        makeSnackbar(getString(R.string.intent_import_select_success, name)).show()
                    }
                }
            }
        }
    }

    private fun makeSnackbar(text: String, duration: Int = Snackbar.LENGTH_SHORT): Snackbar {
        return Snackbar.make(findViewById(android.R.id.content), text, duration)
    }

    private fun quitGracefully() {
        CrashlyticsImpl.sendUnsentReports()
        Timer().schedule(2000){
            this@MainActivity.finishAndRemoveTask()
        }
    }

    class FakePresetFragment : Fragment(), TargetFragment {
        val pref by lazy {
            FileLibraryPreference(requireContext(), null).apply {
                this.type = "Presets"
                this.key = "presets"
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : Preference?> findPreference(key: CharSequence): T? {
            return pref as? T
        }

        companion object {
            fun newInstance() = FakePresetFragment()
        }
    }

    private var presetDialogHost: FakePresetFragment? = null

    companion object {
        const val EXTRA_FORCE_SHOW_CAPTURE_PROMPT = "ForceShowCapturePrompt"

        private val DEBUG_IGNORE_MISSING_LIBRARY = BuildConfig.DEBUG
        private const val STATE_LOAD_FAILED = "LoadFailed"
    }
}