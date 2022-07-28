package me.timschneeberger.rootlessjamesdsp.activity

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.databinding.ActivityMainBinding
import me.timschneeberger.rootlessjamesdsp.databinding.ContentMainBinding
import me.timschneeberger.rootlessjamesdsp.fragment.DspFragment
import me.timschneeberger.rootlessjamesdsp.model.ProcessorMessage
import me.timschneeberger.rootlessjamesdsp.native.JamesDspWrapper
import me.timschneeberger.rootlessjamesdsp.service.AudioProcessorService
import me.timschneeberger.rootlessjamesdsp.utils.ApplicationUtils
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.registerLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.sendLocalBroadcast
import me.timschneeberger.rootlessjamesdsp.utils.SystemServices
import me.timschneeberger.rootlessjamesdsp.view.FloatingToggleButton
import timber.log.Timber


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var bindingContent: ContentMainBinding

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var capturePermissionLauncher: ActivityResultLauncher<Intent>
    private lateinit var prefs: SharedPreferences

    private var processorService: AudioProcessorService? = null
    private var processorServiceBound: Boolean = false

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

    private var serviceStoppedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            binding.powerToggle.isToggled = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val check = ApplicationUtils.check(this)
        if(check != 0) {
            Toast.makeText(
                this,
                "($check) Cannot launch application. Please re-download the latest version from the official GitHub project site.",
                Toast.LENGTH_LONG
            ).show()
            Timber.tag(TAG).wtf(UnsupportedOperationException("Launch error $check; ${ApplicationUtils.describe(this)}"))
            this.finishAndRemoveTask()
            return
        }

        mediaProjectionManager = SystemServices.get(this, MediaProjectionManager::class.java)

        prefs = getSharedPreferences(Constants.PREF_APP, Context.MODE_PRIVATE)
        val crashlytics = prefs.getBoolean(getString(R.string.key_share_crash_reports), true)
        Timber.tag(TAG).d("Crashlytics enabled? $crashlytics")
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(crashlytics)

        binding = ActivityMainBinding.inflate(layoutInflater)
        bindingContent = ContentMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

         // Wait for NavHostFragment to inflate
        bindingContent.dspFragmentContainer.post {
            val navController = findNavController(R.id.dsp_fragment_container)
            appBarConfiguration = AppBarConfiguration(navController.graph)
            setupActionBarWithNavController(navController, appBarConfiguration)
        }

        // Load dsp settings fragment
        val fragmentManager = supportFragmentManager
        fragmentManager.beginTransaction()
            .replace(R.id.dsp_fragment_container, DspFragment.newInstance())
            .commit()

        // Check permissions and launch onboarding if required
        if(checkSelfPermission(Manifest.permission.DUMP) == PackageManager.PERMISSION_DENIED ||
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
            val firstBoot = getSharedPreferences(Constants.PREF_VAR, Context.MODE_PRIVATE)
                                .getBoolean(getString(R.string.key_firstboot), true)
            Timber.tag(TAG).i("Launching onboarding (first boot: $firstBoot)")

            val onboarding = Intent(this, OnboardingActivity::class.java)
            onboarding.putExtra(OnboardingActivity.EXTRA_FIX_PERMS, !firstBoot)
            startActivity(onboarding)
            this.finish()
        }

        binding.bar.inflateMenu(R.menu.menu_main_bottom)
        binding.bar.setOnMenuItemClickListener { arg0 ->
            if (arg0.itemId == R.id.action_settings) {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else
                false
        }

        registerLocalReceiver(serviceStoppedReceiver, IntentFilter(Constants.ACTION_SERVICE_STOPPED))
        registerLocalReceiver(processorMessageReceiver, IntentFilter(AudioProcessorService.ACTION_PROCESSOR_MESSAGE))

        binding.powerToggle.toggleOnClick = false
        binding.powerToggle.setOnToggleClickListener(object : FloatingToggleButton.OnToggleClickListener{
            override fun onClick() {
                if(binding.powerToggle.isToggled) {
                    // Currently on, let's turn it off
                    AudioProcessorService.stop(this@MainActivity)
                    binding.powerToggle.isToggled = false
                }
                else {
                    // Currently off, let's turn it on
                    requestCapturePermission()
                }
            }
        })

        capturePermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
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

        sendLocalBroadcast(Intent(Constants.ACTION_UPDATE_PREFERENCES))

        if(intent.getBooleanExtra(EXTRA_FORCE_SHOW_CAPTURE_PROMPT, false)){
            requestCapturePermission()
        }
    }

    override fun onStart() {
        super.onStart()
        bindProcessorService()
    }

    override fun onStop() {
        super.onStop()
        unbindProcessorService()
    }

    override fun onResume() {
        super.onResume()
        if(processorService != null)
        {
            binding.powerToggle.isToggled = true
        }
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.dsp_fragment_container)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    fun requestCapturePermission() {
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

    companion object {
        const val TAG = "MainActivity"
        const val EXTRA_FORCE_SHOW_CAPTURE_PROMPT = "ForceShowCapturePrompt"
    }
}