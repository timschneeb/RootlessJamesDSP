package me.timschneeberger.rootlessjamesdsp

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Build
import android.os.StrictMode
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.pluto.Pluto
import com.pluto.plugins.exceptions.PlutoExceptions
import com.pluto.plugins.exceptions.PlutoExceptionsPlugin
import com.pluto.plugins.logger.PlutoLoggerPlugin
import com.pluto.plugins.logger.PlutoTimberTree
import com.pluto.plugins.network.PlutoNetworkPlugin
import com.pluto.plugins.preferences.PlutoSharePreferencesPlugin
import com.pluto.plugins.rooms.db.PlutoRoomsDBWatcher
import com.pluto.plugins.rooms.db.PlutoRoomsDatabasePlugin
import fr.bipi.tressence.file.FileLoggerTree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import me.timschneeberger.rootlessjamesdsp.flavor.CrashlyticsImpl
import me.timschneeberger.rootlessjamesdsp.flavor.UpdateManager
import me.timschneeberger.rootlessjamesdsp.model.preference.ThemeMode
import me.timschneeberger.rootlessjamesdsp.model.room.AppBlocklistDatabase
import me.timschneeberger.rootlessjamesdsp.model.room.AppBlocklistRepository
import me.timschneeberger.rootlessjamesdsp.service.RootAudioProcessorService
import me.timschneeberger.rootlessjamesdsp.session.dump.DumpManager
import me.timschneeberger.rootlessjamesdsp.session.root.RootSessionDatabase
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.ProfileManager
import me.timschneeberger.rootlessjamesdsp.utils.RoutingObserver
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.registerLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.isRoot
import me.timschneeberger.rootlessjamesdsp.utils.isRootless
import me.timschneeberger.rootlessjamesdsp.utils.notifications.Notifications
import me.timschneeberger.rootlessjamesdsp.utils.preferences.Preferences
import me.timschneeberger.rootlessjamesdsp.utils.sdkAbove
import me.timschneeberger.rootlessjamesdsp.utils.storage.Cache
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module
import org.lsposed.hiddenapibypass.HiddenApiBypass
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


open class MainApplication : Application(), SharedPreferences.OnSharedPreferenceChangeListener {
    init {
        sdkAbove(Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions("L")
        }
    }

    private val prefs: Preferences.App by inject()
    lateinit var profileManager: ProfileManager

    val rootSessionDatabase by lazy { RootSessionDatabase(this) }
    val isLegacyMode
        get() = prefs.get<Boolean>(R.string.key_audioformat_processing)
    val isEnhancedProcessing
        get() = !isLegacyMode &&
                prefs.get<Boolean>(R.string.key_audioformat_enhanced_processing)

    private val applicationScope = CoroutineScope(SupervisorJob())
    private val blockedAppDatabase by lazy { AppBlocklistDatabase.getDatabase(this, applicationScope) }
    val blockedAppRepository by lazy { AppBlocklistRepository(blockedAppDatabase.appBlocklistDao()) }

    /* Rootless: Media projection auth token */
    var mediaProjectionStartIntent: Intent? = null

    var engineSampleRate = 0f
        private set

    private val receiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when(intent?.action) {
                    Constants.ACTION_REPORT_SAMPLE_RATE -> {
                        engineSampleRate = intent.getFloatExtra(Constants.EXTRA_SAMPLE_RATE, 0f)
                    }
                    Constants.ACTION_DISCARD_AUTHORIZATION -> {
                        if(isRootless()) {
                            Timber.i("mediaProjectionStartIntent discarded")
                            mediaProjectionStartIntent = null
                        }
                    }
                }
            }
        }
    }

    override fun onCreate() {
        Timber.plant(DebugTree())

        if(BuildConfig.DEBUG) {
            Timber.plant(PlutoTimberTree())
            enableDebugTools()
        }
        if(!BuildConfig.FOSS_ONLY)
            Timber.plant(CrashReportingTree())

        // Clean up
        Cache.cleanup(this)

        Timber.plant(FileLoggerTree.Builder()
            .withFileName("application.log")
            .withDirName(this.cacheDir.absolutePath)
            .withMinPriority(Log.VERBOSE)
            .withSizeLimit(2 * 1000000)
            .withFileLimit(1)
            .appendToFile(false)
            .build())
        Timber.i("====> Application starting up")

        val dumpFile = File(filesDir, "dump.txt")
        if(dumpFile.exists()) {
            dumpFile.delete()
        }

        Notifications.createChannels(this)

        val appModule = module {
            single { RoutingObserver(androidContext()) }
            single { UpdateManager(androidContext()) }
            single { DumpManager(androidContext()) }
            single { Preferences(androidContext()).App() }
            single { Preferences(androidContext()).Var() }
        }

        startKoin {
            androidLogger()
            androidContext(this@MainApplication)
            modules(appModule)
        }

        // Depends on Koin
        profileManager = ProfileManager()

        if(!BuildConfig.FOSS_ONLY) {
            // Soft-disable crashlytics in debug mode by default on each launch
            if (BuildConfig.DEBUG) {
                prefs.set(R.string.key_share_crash_reports, false)
            }

            val crashlytics = prefs.get<Boolean>(R.string.key_share_crash_reports)
            Timber.d("Crashlytics enabled? $crashlytics")
            CrashlyticsImpl.setCollectionEnabled(crashlytics)

            CrashlyticsImpl.setCustomKey("buildType", BuildConfig.BUILD_TYPE)
            CrashlyticsImpl.setCustomKey("buildCommit", BuildConfig.COMMIT_SHA)
            CrashlyticsImpl.setCustomKey("flavor", BuildConfig.FLAVOR)
            try {
                CrashlyticsImpl.setCustomKey(
                    "language",
                    resources.configuration.locales.get(0).language
                )
            }
            catch (ex: Exception) {
                // Just in case the locale array is empty
                Timber.e(ex)
            }
        }

        /**
         * Fix for existing users: reset mode selection to AudioService dump once.
         *
         * Reason: AudioPolicyService dumping (previously the default mode) cannot distinguish
         *         between input and output streams and causes false compat alerts when attaching
         *         to a input SID.
         */
        if(!prefs.get<Boolean>(R.string.key_reset_proc_mode_fix_applied)) {
            Timber.i("Applying service dump mode fix once")

            prefs.set(R.string.key_session_detection_method, DumpManager.Method.AudioService.value.toString())
            prefs.set(R.string.key_reset_proc_mode_fix_applied, true)
        }

        onSharedPreferenceChanged(prefs.preferences, getString(R.string.key_appearance_theme_mode))
        prefs.registerOnSharedPreferenceChangeListener(this)

        registerLocalReceiver(receiver, IntentFilter().apply {
            addAction(Constants.ACTION_REPORT_SAMPLE_RATE)
            addAction(Constants.ACTION_DISCARD_AUTHORIZATION)
        })

        try {
            if (isRoot() && isLegacyMode)
                RootAudioProcessorService.updateLegacyMode(applicationContext, true)
        }
        catch(ex: Exception) {
            /* Throws ForegroundServiceStartNotAllowedException on Android 13 if
             * the service cannot be started from this point and battery optimizations were not
             * disabled. BootCompletedReceiver handles auto-start after boot.
             */
            Timber.e("Failed to launch service in legacy mode on startup")
            Timber.i(ex)
        }
        super.onCreate()
    }

    override fun onTerminate() {
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        unregisterReceiver(receiver)
        super.onTerminate()
    }

    override fun onLowMemory() {
        Timber.w("onLowMemory: Running low on memory")
        CrashlyticsImpl.setCustomKey("last_low_memory_event", SimpleDateFormat("yyyyMMdd HHmmss z", Locale.US).format(Date()))
        super.onLowMemory()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if(key == getString(R.string.key_appearance_theme_mode)) {
            AppCompatDelegate.setDefaultNightMode(
                when (ThemeMode.fromInt(prefs.get<String>(R.string.key_appearance_theme_mode).toIntOrNull() ?: 0)) {
                    ThemeMode.Light -> AppCompatDelegate.MODE_NIGHT_NO
                    ThemeMode.Dark -> AppCompatDelegate.MODE_NIGHT_YES
                    ThemeMode.FollowSystem -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            )
        }
    }

    private fun enableDebugTools() {
        // Setup strict mode with death penalty
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .apply {
                    detectCustomSlowCalls()
                    detectNetwork()
                    detectResourceMismatches()
                    penaltyLog()
                    penaltyDeath()
                }
                .build()
        )

        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .apply {
                    detectLeakedRegistrationObjects()
                    detectCleartextNetwork()
                    detectActivityLeaks()
                    detectLeakedClosableObjects()
                    detectLeakedSqlLiteObjects()
                    detectContentUriWithoutPermission()
                    penaltyLog()
                    // penaltyDeath()
                }
                .build()
        )

        Pluto.Installer(this)
            .addPlugin(PlutoNetworkPlugin("network"))
            .addPlugin(PlutoExceptionsPlugin("exceptions"))
            .addPlugin(PlutoLoggerPlugin("logger"))
            .addPlugin(PlutoSharePreferencesPlugin("sharedPref"))
            .addPlugin(PlutoRoomsDatabasePlugin("rooms-db"))
            .install()
        Pluto.showNotch(true)

        PlutoExceptions.setANRHandler { thread, exception ->
            Timber.e("unhandled ANR handled on thread: " + thread.name, exception)
        }

        PlutoRoomsDBWatcher.watch("blocked_apps.db", AppBlocklistDatabase::class.java)
    }

    /** A tree which logs important information for crash reporting.  */
    private class CrashReportingTree : DebugTree() {
        private fun priorityAsString(priority: Int): String {
            return when(priority){
                Log.VERBOSE -> "V"
                Log.DEBUG -> "D"
                Log.INFO -> "I"
                Log.WARN -> "W"
                Log.ERROR -> "E"
                Log.ASSERT -> "A"
                else -> "?"
            }
        }

        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            CrashlyticsImpl.log("[${priorityAsString(priority)}] ${tag ?: "???"}: $message")
            t?.takeIf { priority >= Log.WARN }?.let(CrashlyticsImpl::recordException)
        }
    }
}