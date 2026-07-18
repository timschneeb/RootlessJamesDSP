package me.timschneeberger.rootlessjamesdsp.service

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioMixerAttributes
import android.media.AudioPlaybackConfiguration
import android.media.AudioPlaybackConfigurationHidden
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import dev.rikka.tools.refine.Refine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.flavor.CrashlyticsImpl
import me.timschneeberger.rootlessjamesdsp.interop.JamesDspLocalEngine
import me.timschneeberger.rootlessjamesdsp.interop.ProcessorMessageHandler
import me.timschneeberger.rootlessjamesdsp.model.IEffectSession
import me.timschneeberger.rootlessjamesdsp.model.preference.AudioEncoding
import me.timschneeberger.rootlessjamesdsp.model.room.AppBlocklistDatabase
import me.timschneeberger.rootlessjamesdsp.model.room.AppBlocklistRepository
import me.timschneeberger.rootlessjamesdsp.model.room.BlockedApp
import me.timschneeberger.rootlessjamesdsp.model.rootless.SessionRecordingPolicyEntry
import me.timschneeberger.rootlessjamesdsp.session.rootless.OnRootlessSessionChangeListener
import me.timschneeberger.rootlessjamesdsp.session.rootless.RootlessSessionDatabase
import me.timschneeberger.rootlessjamesdsp.session.rootless.RootlessSessionManager
import me.timschneeberger.rootlessjamesdsp.session.rootless.SessionRecordingPolicyManager
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.ActivePlaybackSampleRateDetector
import me.timschneeberger.rootlessjamesdsp.utils.AudioSampleRateDetector
import me.timschneeberger.rootlessjamesdsp.utils.UsbHardwareSampleRateDetector
import me.timschneeberger.rootlessjamesdsp.utils.Constants.ACTION_PREFERENCES_UPDATED
import me.timschneeberger.rootlessjamesdsp.utils.Constants.ACTION_SAMPLE_RATE_UPDATED
import me.timschneeberger.rootlessjamesdsp.utils.Constants.ACTION_SERVICE_HARD_REBOOT_CORE
import me.timschneeberger.rootlessjamesdsp.utils.Constants.ACTION_SERVICE_RELOAD_LIVEPROG
import me.timschneeberger.rootlessjamesdsp.utils.Constants.ACTION_SERVICE_SOFT_REBOOT_CORE
import me.timschneeberger.rootlessjamesdsp.utils.extensions.CompatExtensions.getParcelableAs
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.registerLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.sendLocalBroadcast
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.toast
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.unregisterLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.extensions.PermissionExtensions.hasRecordPermission
import me.timschneeberger.rootlessjamesdsp.utils.notifications.Notifications
import me.timschneeberger.rootlessjamesdsp.utils.notifications.ServiceNotificationHelper
import me.timschneeberger.rootlessjamesdsp.utils.preferences.Preferences
import me.timschneeberger.rootlessjamesdsp.utils.sdkAbove
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder


@RequiresApi(Build.VERSION_CODES.Q)
class RootlessAudioProcessorService : BaseAudioProcessorService() {
    // System services
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var audioManager: AudioManager

    private val sampleRatePlaybackCallback = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>?) {
            if (!isRunning) return

            if (isUsbOutputConnected() && followUsbHardwareRate()) {
                applicationScope.launch(Dispatchers.IO) {
                    val sourceRate = ActivePlaybackSampleRateDetector.detect(
                        this@RootlessAudioProcessorService,
                        Process.myUid(),
                    )
                    mainHandler.post {
                        if (sourceRate > 0 && sourceRate != lastUsbSourceSampleRate) {
                            lastUsbSourceSampleRate = sourceRate
                            scheduleUsbHardwareRateReconfiguration()
                        }
                    }
                }
                return
            }

            val generation = ++sampleRateDetectionGeneration
            val callbackRate = detectActiveContentSampleRate(configs)
            if (callbackRate > 0) {
                applyActiveContentSampleRate(callbackRate)
                return
            }

            applicationScope.launch(Dispatchers.IO) {
                val dumpRate = ActivePlaybackSampleRateDetector.detect(
                    this@RootlessAudioProcessorService,
                    Process.myUid(),
                )
                mainHandler.post {
                    if (generation == sampleRateDetectionGeneration && isRunning) {
                        applyActiveContentSampleRate(dumpRate)
                    }
                }
            }
        }
    }
    private val usbRouteRestart = Runnable {
        if (isRunning && !isProcessorDisposing && !isServiceDisposing) {
            if (isUsbOutputConnected() && followUsbHardwareRate()) {
                Timber.i("USB audio route changed; detecting physical hardware rate")
                scheduleUsbHardwareRateReconfiguration()
            } else {
                Timber.i("USB audio route removed; rebuilding normal output")
                restartRecording()
            }
        }
    }
    private val usbAudioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            if (addedDevices.none(::isUsbAudioDevice)) return
            mainHandler.removeCallbacks(usbRouteRestart)
            // USB profiles are populated asynchronously after the device-added callback.
            mainHandler.postDelayed(usbRouteRestart, USB_ROUTE_SETTLE_DELAY_MS)
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            if (removedDevices.none(::isUsbAudioDevice)) return
            activeUsbHardwareSampleRate = 0
            mainHandler.removeCallbacks(usbRouteRestart)
            mainHandler.post(usbRouteRestart)
        }
    }
    private var preferredMixerAttributesListener:
        AudioManager.OnPreferredMixerAttributesChangedListener? = null

    // Media projection token
    private var mediaProjection: MediaProjection? = null
    private var mediaProjectionStartIntent: Intent? = null

    // Processing
    private var recreateRecorderRequested = false
    private var recorderThread: Thread? = null
    private var activeContentSampleRate = 0
    private var activeUsbHardwareSampleRate = 0
    private var lastUsbSourceSampleRate = 0
    private var usbHardwareRateReconfigurationPending = false
    private var sampleRateDetectionGeneration = 0
    private val mainHandler = Handler(Looper.getMainLooper())
    private var preferredUsbMixerDevice: AudioDeviceInfo? = null
    private var preferredUsbMixerAudioAttributes: AudioAttributes? = null
    private lateinit var engine: JamesDspLocalEngine
    private val isRunning: Boolean
        get() = recorderThread != null

    // Session management
    private lateinit var sessionManager: RootlessSessionManager
    private var sessionLossRetryCount = 0

    // Idle detection
    private var isProcessorIdle = false
    private var suspendOnIdle = false

    // Exclude restricted apps flag
    private var excludeRestrictedSessions = false

    // Termination flags
    private var isProcessorDisposing = false
    private var isServiceDisposing = false

    // Shared preferences
    private val preferences: Preferences.App by inject()
    private val preferencesVar: Preferences.Var by inject()

    // Room databases
    private val applicationScope = CoroutineScope(SupervisorJob())
    private val blockedAppDatabase by lazy { AppBlocklistDatabase.getDatabase(this, applicationScope) }
    private val blockedAppRepository by lazy { AppBlocklistRepository(blockedAppDatabase.appBlocklistDao()) }
    private val blockedApps by lazy { blockedAppRepository.blocklist.asLiveData() }
    private val blockedAppObserver = Observer<List<BlockedApp>?> {
        Timber.d("blockedAppObserver: Database changed; ignored=${!isRunning}")
        if(isRunning)
            recreateRecorderRequested = true
    }

    override fun onCreate() {
        super.onCreate()

        // Get reference to system services
        audioManager = getSystemService<AudioManager>()!!
        mediaProjectionManager = getSystemService<MediaProjectionManager>()!!
        notificationManager = getSystemService<NotificationManager>()!!

        // Setup session manager
        sessionManager = RootlessSessionManager(this)
        sessionManager.sessionDatabase.setOnSessionLossListener(onSessionLossListener)
        sessionManager.sessionDatabase.setOnAppProblemListener(onAppProblemListener)
        sessionManager.sessionDatabase.registerOnSessionChangeListener(onSessionChangeListener)
        sessionManager.sessionPolicyDatabase.registerOnRestrictedSessionChangeListener(onSessionPolicyChangeListener)

        // Setup core engine
        engine = JamesDspLocalEngine(this, ProcessorMessageHandler())
        engine.syncWithPreferences()
        audioManager.registerAudioPlaybackCallback(
            sampleRatePlaybackCallback,
            Handler(Looper.getMainLooper()),
        )
        audioManager.registerAudioDeviceCallback(usbAudioDeviceCallback, mainHandler)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            registerPreferredMixerAttributesListener()
        }

        // Setup general-purpose broadcast receiver
        val filter = IntentFilter()
        filter.addAction(ACTION_PREFERENCES_UPDATED)
        filter.addAction(ACTION_SAMPLE_RATE_UPDATED)
        filter.addAction(ACTION_SERVICE_RELOAD_LIVEPROG)
        filter.addAction(ACTION_SERVICE_HARD_REBOOT_CORE)
        filter.addAction(ACTION_SERVICE_SOFT_REBOOT_CORE)
        registerLocalReceiver(broadcastReceiver, filter)

        // Setup shared preferences
        preferences.registerOnSharedPreferenceChangeListener(preferencesListener)
        loadFromPreferences(getString(R.string.key_powersave_suspend))
        loadFromPreferences(getString(R.string.key_session_exclude_restricted))

        // Setup database observer
        blockedApps.observeForever(blockedAppObserver)

        notificationManager.cancel(Notifications.ID_SERVICE_STARTUP)

        // No need to recreate in this stage
        recreateRecorderRequested = false

        // Launch foreground service
        startForeground(
            Notifications.ID_SERVICE_STATUS,
            ServiceNotificationHelper.createServiceNotification(this, arrayOf()),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        )
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        Timber.d("onStartCommand")

        // Handle intent action
        when (intent.action) {
            null -> {
                Timber.wtf("onStartCommand: intent.action is null")
            }
            ACTION_START -> {
                Timber.d("Starting service")
            }
            ACTION_STOP -> {
                Timber.d("Stopping service")
                stopSelf()
                return START_NOT_STICKY
            }
        }

        if (isRunning) {
            return START_NOT_STICKY
        }

        // Cancel outdated notifications
        notificationManager.cancel(Notifications.ID_SERVICE_SESSION_LOSS)
        notificationManager.cancel(Notifications.ID_SERVICE_APPCOMPAT)

        // Setup media projection
        mediaProjectionStartIntent = intent.extras?.getParcelableAs(EXTRA_MEDIA_PROJECTION_DATA)

        mediaProjection = try {
            mediaProjectionManager.getMediaProjection(
                Activity.RESULT_OK,
                mediaProjectionStartIntent!!
            )
        }
        catch (ex: Exception) {
            Timber.e("Failed to acquire media projection")
            sendLocalBroadcast(Intent(Constants.ACTION_DISCARD_AUTHORIZATION))
            Timber.e(ex)
            null
        }

        mediaProjection?.registerCallback(projectionCallback, Handler(Looper.getMainLooper()))

        if (mediaProjection != null) {
            startRecording()
            sendLocalBroadcast(Intent(Constants.ACTION_SERVICE_STARTED))
        } else {
            Timber.w("Failed to capture audio")
            stopSelf()
        }

        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        isServiceDisposing = true

        // Stop recording and release engine
        stopRecording()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            clearPreferredUsbMixer()
        }
        engine.close()

        // Stop foreground service
        stopForeground(STOP_FOREGROUND_REMOVE)

        // Notify app about service termination
        sendLocalBroadcast(Intent(Constants.ACTION_SERVICE_STOPPED))

        // Unregister database observer
        blockedApps.removeObserver(blockedAppObserver)
        audioManager.unregisterAudioPlaybackCallback(sampleRatePlaybackCallback)
        audioManager.unregisterAudioDeviceCallback(usbAudioDeviceCallback)
        mainHandler.removeCallbacks(usbRouteRestart)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            unregisterPreferredMixerAttributesListener()
        }

        // Unregister receivers and release resources
        unregisterLocalReceiver(broadcastReceiver)
        mediaProjection?.unregisterCallback(projectionCallback)
        mediaProjection = null

        sessionManager.sessionPolicyDatabase.unregisterOnRestrictedSessionChangeListener(onSessionPolicyChangeListener)
        sessionManager.sessionDatabase.unregisterOnSessionChangeListener(onSessionChangeListener)
        sessionManager.destroy()

        preferences.unregisterOnSharedPreferenceChangeListener(preferencesListener)
        notificationManager.cancel(Notifications.ID_SERVICE_STATUS)

        stopSelf()
        super.onDestroy()
    }

    // Preferences listener
    private val preferencesListener = SharedPreferences.OnSharedPreferenceChangeListener {
            _, key ->
        loadFromPreferences(key)
    }

    // Projection termination callback
    private val projectionCallback = object: MediaProjection.Callback() {
        override fun onStop() {
            if(isServiceDisposing) {
                // Planned shutdown
                return
            }

            if(preferencesVar.get<Boolean>(R.string.key_is_activity_active)) {
                // Activity in foreground, toast too disruptive
                return
            }

            Timber.w("Capture permission revoked. Stopping service.")

            sendLocalBroadcast(Intent(Constants.ACTION_DISCARD_AUTHORIZATION))

            this@RootlessAudioProcessorService.toast(getString(R.string.capture_permission_revoked_toast))

            notificationManager.cancel(Notifications.ID_SERVICE_STATUS)
            stopSelf()
        }
    }

    // General purpose broadcast receiver
    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_SAMPLE_RATE_UPDATED -> engine.syncWithPreferences(arrayOf(Constants.PREF_CONVOLVER))
                ACTION_PREFERENCES_UPDATED -> engine.syncWithPreferences()
                ACTION_SERVICE_RELOAD_LIVEPROG -> engine.syncWithPreferences(arrayOf(Constants.PREF_LIVEPROG))
                ACTION_SERVICE_HARD_REBOOT_CORE -> restartRecording()
                ACTION_SERVICE_SOFT_REBOOT_CORE -> requestAudioRecordRecreation()
            }
        }
    }

    // Session loss listener
    private val onSessionLossListener = object: RootlessSessionDatabase.OnSessionLossListener {
        override fun onSessionLost(sid: Int) {
            // Push notification if enabled
            if(!preferences.get<Boolean>(R.string.key_session_loss_ignore)) {
                // Check if retry count exceeded
                if(sessionLossRetryCount < SESSION_LOSS_MAX_RETRIES) {
                    // Retry
                    sessionLossRetryCount++
                    Timber.d("Session lost. Retry count: $sessionLossRetryCount/$SESSION_LOSS_MAX_RETRIES")
                    sessionManager.pollOnce(false)
                    restartRecording()
                    return
                }
                else {
                    sessionLossRetryCount = 0
                    Timber.d("Giving up on saving session. User interaction required.")
                }

                // Request users attention
                notificationManager.cancel(Notifications.ID_SERVICE_STATUS)
                ServiceNotificationHelper.pushSessionLossNotification(this@RootlessAudioProcessorService, mediaProjectionStartIntent)
                this@RootlessAudioProcessorService.toast(getString(R.string.session_control_loss_toast), false)
                Timber.w("Terminating service due to session loss")
                stopSelf()
            }
        }
    }

    // Session change listener
    private val onSessionChangeListener = object : OnRootlessSessionChangeListener {
        override fun onSessionChanged(sessionList: HashMap<Int, IEffectSession>) {
            isProcessorIdle = sessionList.size == 0
            Timber.d("onSessionChanged: isProcessorIdle=$isProcessorIdle")

            ServiceNotificationHelper.pushServiceNotification(
                this@RootlessAudioProcessorService,
                sessionList.map { it.value }.toTypedArray()
            )
        }
    }

    // App problem listener
    private val onAppProblemListener = object : RootlessSessionDatabase.OnAppProblemListener {
        override fun onAppProblemDetected(uid: Int) {
            // Push notification if enabled
            if(!preferences.get<Boolean>(R.string.key_session_app_problem_ignore)) {
                // Request users attention
                notificationManager.cancel(Notifications.ID_SERVICE_STATUS)

                // Determine if we should redirect instantly, or push a non-intrusive notification
                if(preferencesVar.get<Boolean>(R.string.key_is_activity_active) ||
                    preferencesVar.get<Boolean>(R.string.key_is_app_compat_activity_active)) {
                    startActivity(
                        ServiceNotificationHelper.createAppTroubleshootIntent(
                            this@RootlessAudioProcessorService,
                            mediaProjectionStartIntent,
                            uid,
                            directLaunch = true
                        )
                    )
                    notificationManager.cancel(Notifications.ID_SERVICE_APPCOMPAT)
                }
                else
                    ServiceNotificationHelper.pushAppIssueNotification(this@RootlessAudioProcessorService, mediaProjectionStartIntent, uid)

                this@RootlessAudioProcessorService.toast(getString(R.string.session_app_compat_toast), false)
                Timber.w("Terminating service due to app incompatibility; redirect user to troubleshooting options")
                stopSelf()
            }
        }
    }

    // Session policy change listener
    private val onSessionPolicyChangeListener = object : SessionRecordingPolicyManager.OnSessionRecordingPolicyChangeListener {
        override fun onSessionRecordingPolicyChanged(sessionList: HashMap<String, SessionRecordingPolicyEntry>, isMinorUpdate: Boolean) {
            if(!this@RootlessAudioProcessorService.excludeRestrictedSessions) {
                Timber.d("onRestrictedSessionChanged: blocked; excludeRestrictedSessions disabled")
                return
            }

            if(!isMinorUpdate) {
                Timber.d("onRestrictedSessionChanged: major update detected; requesting soft-reboot")
                requestAudioRecordRecreation()
            }
            else {
                Timber.d("onRestrictedSessionChanged: minor update detected")
            }
        }
    }

    private fun loadFromPreferences(key: String?){
        when (key) {
            getString(R.string.key_powersave_suspend) -> {
                suspendOnIdle = preferences.get<Boolean>(R.string.key_powersave_suspend)
                Timber.d("Suspend on idle set to $suspendOnIdle")
            }
            getString(R.string.key_session_exclude_restricted) -> {
                excludeRestrictedSessions = preferences.get<Boolean>(R.string.key_session_exclude_restricted)
                Timber.d("Exclude restricted set to $excludeRestrictedSessions")

                requestAudioRecordRecreation()
            }
        }
    }

    // Request recreation of the AudioRecord object to update AudioPlaybackRecordingConfiguration
    fun requestAudioRecordRecreation() {
        if(isProcessorDisposing || isServiceDisposing) {
            Timber.e("recreateAudioRecorder: service or processor already disposing")
            return
        }

        recreateRecorderRequested = true
    }

    // Start recording thread
    @SuppressLint("BinaryOperationInTimber")
    private fun startRecording() {
        // Sanity check
        if (!hasRecordPermission()) {
            Timber.e("Record audio permission missing. Can't record")
            stopSelf()
            return
        }

        // Load preferences
        val requestedEncoding = AudioEncoding.fromInt(
            preferences.get<String>(R.string.key_audioformat_encoding).toIntOrNull() ?: 1
        )
        val sampleRate = determineSamplingRate()
        val requestedEncodingFormat = when (requestedEncoding) {
            AudioEncoding.PcmShort -> AudioFormat.ENCODING_PCM_16BIT
            else -> AudioFormat.ENCODING_PCM_FLOAT
        }
        val outputFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            configurePreferredUsbMixer(sampleRate, requestedEncodingFormat)
        } else {
            null
        } ?: buildOutputAudioFormat(requestedEncodingFormat, sampleRate)
        val encodingFormat = requestedEncodingFormat
        val encoding = requestedEncoding
        val bufferSize = preferences.get<Float>(R.string.key_audioformat_buffersize).toInt()
        val bufferSizeBytes = when (encoding) {
            AudioEncoding.PcmFloat -> bufferSize * Float.SIZE_BYTES
            else -> bufferSize * Short.SIZE_BYTES
        }
        Timber.i("Sample rate: $sampleRate; Encoding: ${encoding.name}; " +
                "Buffer size: $bufferSize; Buffer size (bytes): $bufferSizeBytes ; " +
                "HAL buffer size (bytes): ${determineBufferSize()}")

        // Create recorder and track
        var recorder: AudioRecord
        val track: AudioTrack
        try {
            recorder = buildAudioRecord(encodingFormat, sampleRate, bufferSizeBytes)
            track = buildAudioTrack(
                outputFormat,
                bufferSize * bytesPerSample(outputFormat.encoding),
            )
        }
        catch(ex: Exception) {
            Timber.e("Failed to create initial audio record/track")
            Timber.e(ex)
            stopSelf()
            return
        }

        if(engine.sampleRate.toInt() != sampleRate) {
            Timber.d("Sampling rate changed to ${sampleRate}Hz")
            engine.sampleRate = sampleRate.toFloat()
        }

        // TODO Move all audio-related code to C++
        recorderThread = Thread {
            try {
                ServiceNotificationHelper.pushServiceNotification(applicationContext, arrayOf())

                val floatBuffer = FloatArray(bufferSize)
                val floatOutBuffer = FloatArray(bufferSize)
                val shortBuffer = ShortArray(bufferSize)
                val shortOutBuffer = ShortArray(bufferSize)
                val packedOutputBuffer = ByteBuffer
                    .allocateDirect(bufferSize * Int.SIZE_BYTES)
                    .order(ByteOrder.nativeOrder())
                while (!isProcessorDisposing) {
                    if(recreateRecorderRequested) {
                        recreateRecorderRequested = false
                        Timber.d("Recreating recorder without stopping thread...")

                        // Suspend track, release recorder
                        recorder.stop()
                        track.stop()
                        recorder.release()


                        if (mediaProjection == null) {
                            Timber.e("Media projection handle is null, stopping service")
                            stopSelf()
                            return@Thread
                        }

                        // Recreate recorder with new AudioPlaybackRecordingConfiguration
                        recorder = buildAudioRecord(encodingFormat, sampleRate, bufferSizeBytes)
                        Timber.d("Recorder recreated")
                    }

                    // Suspend core while idle
                    if(isProcessorIdle && suspendOnIdle)
                    {
                        if(recorder.state == AudioRecord.STATE_INITIALIZED &&
                            recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING)
                            recorder.stop()
                        if(track.state == AudioTrack.STATE_INITIALIZED &&
                            track.playState != AudioTrack.PLAYSTATE_STOPPED)
                            track.stop()

                        try {
                            Thread.sleep(50)
                        }
                        catch(e: InterruptedException) {
                            break
                        }
                        continue
                    }

                    // Resume recorder if suspended
                    if(recorder.recordingState == AudioRecord.RECORDSTATE_STOPPED) {
                        recorder.startRecording()
                    }
                    // Resume track if suspended
                    if(track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                        track.play()
                    }

                    // Choose encoding and process data
                    if(encoding == AudioEncoding.PcmShort) {
                        recorder.read(shortBuffer, 0, shortBuffer.size, AudioRecord.READ_BLOCKING)
                        engine.processInt16(shortBuffer, shortOutBuffer)
                        writeShortOutput(
                            track,
                            outputFormat.encoding,
                            shortOutBuffer,
                            floatOutBuffer,
                            packedOutputBuffer,
                        )
                    }
                    else {
                        recorder.read(floatBuffer, 0, floatBuffer.size, AudioRecord.READ_BLOCKING)
                        engine.processFloat(floatBuffer, floatOutBuffer)
                        writeFloatOutput(
                            track,
                            outputFormat.encoding,
                            floatOutBuffer,
                            shortOutBuffer,
                            packedOutputBuffer,
                        )
                    }
                }
            } catch (e: IOException) {
                Timber.w(e)
                // ignore
            } catch (e: Exception) {
                Timber.e("Exception in recorderThread raised")
                Timber.e(e)
                stopSelf()
            } finally {
                // Clean up recorder and track
                if(recorder.state != AudioRecord.STATE_UNINITIALIZED) {
                    recorder.stop()
                }
                if(track.state != AudioTrack.STATE_UNINITIALIZED) {
                    track.stop()
                }

                recorder.release()
                track.release()
            }
        }
        recorderThread!!.start()
    }

    // Terminate recording thread
    fun stopRecording() {
        if (recorderThread != null) {
            isProcessorDisposing = true
            recorderThread!!.interrupt()
            recorderThread!!.join(500)
            recorderThread = null
        }
    }

    // Hard restart recording thread
    fun restartRecording() {
        if(isProcessorDisposing || isServiceDisposing) {
            Timber.e("restartRecording: service or processor already disposing")
            return
        }

        stopRecording()
        isProcessorDisposing = false
        recreateRecorderRequested = false
        startRecording()
    }

    private fun restartForSampleRateChange(sampleRate: Int) {
        if (!isRunning || engine.sampleRate.toInt() == sampleRate) return

        Timber.i(
            "Output sample rate changed from ${engine.sampleRate.toInt()}Hz to ${sampleRate}Hz"
        )
        restartRecording()
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun registerPreferredMixerAttributesListener() {
        preferredMixerAttributesListener =
            AudioManager.OnPreferredMixerAttributesChangedListener {
                    _: AudioAttributes,
                    _: AudioDeviceInfo,
                    _: AudioMixerAttributes? ->
                restartForSampleRateChange(determineSamplingRate())
            }
        audioManager.addOnPreferredMixerAttributesChangedListener(
            mainExecutor,
            preferredMixerAttributesListener!!,
        )
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun unregisterPreferredMixerAttributesListener() {
        preferredMixerAttributesListener?.let {
            audioManager.removeOnPreferredMixerAttributesChangedListener(it)
        }
        preferredMixerAttributesListener = null
    }

    private fun buildMediaAudioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()
    }

    private fun isUsbAudioDevice(device: AudioDeviceInfo): Boolean {
        return device.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
            device.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
            device.type == AudioDeviceInfo.TYPE_USB_ACCESSORY
    }

    private fun isUsbOutputConnected(): Boolean {
        return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any(::isUsbAudioDevice)
    }

    private fun followUsbHardwareRate(): Boolean {
        return preferences.get<String>(R.string.key_audioformat_rootless_usb_rate_mode) != "0"
    }

    private fun scheduleUsbHardwareRateReconfiguration() {
        if (usbHardwareRateReconfigurationPending || !isRunning || !isUsbOutputConnected()) return
        usbHardwareRateReconfigurationPending = true

        // Release our own USB stream so AudioFlinger exposes the rate selected by the
        // captured source application's remaining (silenced) hardware stream.
        stopRecording()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            clearPreferredUsbMixer()
        }
        isProcessorDisposing = false

        mainHandler.postDelayed({
            applicationScope.launch(Dispatchers.IO) {
                val hardwareRate = UsbHardwareSampleRateDetector.detect(
                    this@RootlessAudioProcessorService
                )
                mainHandler.post {
                    usbHardwareRateReconfigurationPending = false
                    if (isServiceDisposing) return@post
                    activeUsbHardwareSampleRate = hardwareRate
                    startRecording()
                }
            }
        }, USB_HARDWARE_RATE_SETTLE_DELAY_MS)
    }

    private fun buildOutputAudioFormat(encoding: Int, sampleRate: Int): AudioFormat {
        return AudioFormat.Builder()
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .setEncoding(encoding)
            .setSampleRate(sampleRate)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun configurePreferredUsbMixer(
        sampleRate: Int,
        requestedEncoding: Int,
    ): AudioFormat? {
        clearPreferredUsbMixer()

        val attributes = buildMediaAudioAttributes()
        return try {
            audioManager.getAudioDevicesForAttributes(attributes)
                .filter(::isUsbAudioDevice)
                .firstNotNullOfOrNull { device ->
                    val mixer = audioManager.getSupportedMixerAttributes(device)
                        .asSequence()
                        .filter {
                            it.format.sampleRate == sampleRate &&
                                it.format.channelCount == 2 &&
                                it.format.encoding in SUPPORTED_USB_OUTPUT_ENCODINGS
                        }
                        .sortedWith(
                            compareByDescending<AudioMixerAttributes> {
                                it.mixerBehavior ==
                                    AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT
                            }.thenByDescending {
                                it.format.encoding == requestedEncoding
                            }.thenBy {
                                usbEncodingPreference(it.format.encoding)
                            }
                        )
                        .firstOrNull()
                        ?: return@firstNotNullOfOrNull null

                    if (!audioManager.setPreferredMixerAttributes(attributes, device, mixer)) {
                        Timber.w("USB mixer rejected ${sampleRate}Hz")
                        return@firstNotNullOfOrNull null
                    }

                    preferredUsbMixerDevice = device
                    preferredUsbMixerAudioAttributes = attributes
                    Timber.i(
                        "Configured USB mixer at ${mixer.format.sampleRate}Hz, " +
                            "encoding=${mixer.format.encoding}, " +
                            "behavior=${mixer.mixerBehavior}"
                    )
                    mixer.format
                }
        } catch (exception: RuntimeException) {
            Timber.w(exception, "Failed to configure preferred USB mixer")
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun clearPreferredUsbMixer() {
        val device = preferredUsbMixerDevice
        val attributes = preferredUsbMixerAudioAttributes
        preferredUsbMixerDevice = null
        preferredUsbMixerAudioAttributes = null
        if (device == null || attributes == null) return

        try {
            audioManager.clearPreferredMixerAttributes(attributes, device)
        } catch (exception: RuntimeException) {
            Timber.w(exception, "Failed to clear preferred USB mixer")
        }
    }

    private fun buildAudioTrack(format: AudioFormat, bufferSizeBytes: Int): AudioTrack {
        val encoding = format.encoding
        val sampleRate = format.sampleRate
        val frameSizeInBytes = format.channelCount * bytesPerSample(encoding)

        val requestedBufferSize = if (((bufferSizeBytes % frameSizeInBytes) != 0 || bufferSizeBytes < 1)) {
            Timber.e("Invalid audio buffer size $bufferSizeBytes")
            128 * (bufferSizeBytes / 128)
        }
        else bufferSizeBytes
        val minimumBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            format.channelMask,
            encoding,
        ).coerceAtLeast(0)
        val bufferSize = maxOf(requestedBufferSize, minimumBufferSize)
            .let { size ->
                val remainder = size % frameSizeInBytes
                if (remainder == 0) size else size + frameSizeInBytes - remainder
            }

        Timber.d("Using buffer size $bufferSize")

        return AudioTrack.Builder()
            .setAudioFormat(format)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setAudioAttributes(buildMediaAudioAttributes())
            .setBufferSizeInBytes(bufferSize)
            .build()
    }

    private fun writeFloatOutput(
        track: AudioTrack,
        outputEncoding: Int,
        samples: FloatArray,
        shortBuffer: ShortArray,
        packedBuffer: ByteBuffer,
    ) {
        when (outputEncoding) {
            AudioFormat.ENCODING_PCM_FLOAT ->
                track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
            AudioFormat.ENCODING_PCM_16BIT -> {
                samples.indices.forEach { index ->
                    shortBuffer[index] = floatToPcm16(samples[index])
                }
                track.write(shortBuffer, 0, shortBuffer.size, AudioTrack.WRITE_BLOCKING)
            }
            AudioFormat.ENCODING_PCM_24BIT_PACKED,
            AudioFormat.ENCODING_PCM_32BIT -> {
                packedBuffer.clear()
                samples.forEach { sample -> putPcmSample(packedBuffer, outputEncoding, sample) }
                packedBuffer.flip()
                track.write(packedBuffer, packedBuffer.remaining(), AudioTrack.WRITE_BLOCKING)
            }
            else -> error("Unsupported USB output encoding $outputEncoding")
        }
    }

    private fun writeShortOutput(
        track: AudioTrack,
        outputEncoding: Int,
        samples: ShortArray,
        floatBuffer: FloatArray,
        packedBuffer: ByteBuffer,
    ) {
        when (outputEncoding) {
            AudioFormat.ENCODING_PCM_16BIT ->
                track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
            AudioFormat.ENCODING_PCM_FLOAT -> {
                samples.indices.forEach { index ->
                    floatBuffer[index] = samples[index] / 32768f
                }
                track.write(floatBuffer, 0, floatBuffer.size, AudioTrack.WRITE_BLOCKING)
            }
            AudioFormat.ENCODING_PCM_24BIT_PACKED,
            AudioFormat.ENCODING_PCM_32BIT -> {
                packedBuffer.clear()
                samples.forEach { sample ->
                    putPcmSample(packedBuffer, outputEncoding, sample / 32768f)
                }
                packedBuffer.flip()
                track.write(packedBuffer, packedBuffer.remaining(), AudioTrack.WRITE_BLOCKING)
            }
            else -> error("Unsupported USB output encoding $outputEncoding")
        }
    }

    private fun putPcmSample(buffer: ByteBuffer, encoding: Int, value: Float) {
        val normalized = value.coerceIn(-1f, 1f)
        when (encoding) {
            AudioFormat.ENCODING_PCM_32BIT -> {
                val pcm = if (normalized <= -1f) Int.MIN_VALUE
                else (normalized * Int.MAX_VALUE).toInt()
                buffer.putInt(pcm)
            }
            AudioFormat.ENCODING_PCM_24BIT_PACKED -> {
                val pcm = if (normalized <= -1f) -0x800000
                else (normalized * 0x7fffff).toInt()
                if (buffer.order() == ByteOrder.LITTLE_ENDIAN) {
                    buffer.put(pcm.toByte())
                    buffer.put((pcm shr 8).toByte())
                    buffer.put((pcm shr 16).toByte())
                } else {
                    buffer.put((pcm shr 16).toByte())
                    buffer.put((pcm shr 8).toByte())
                    buffer.put(pcm.toByte())
                }
            }
        }
    }

    private fun floatToPcm16(value: Float): Short {
        val normalized = value.coerceIn(-1f, 1f)
        return if (normalized <= -1f) Short.MIN_VALUE
        else (normalized * Short.MAX_VALUE).toInt().toShort()
    }

    private fun bytesPerSample(encoding: Int): Int = when (encoding) {
        AudioFormat.ENCODING_PCM_16BIT -> Short.SIZE_BYTES
        AudioFormat.ENCODING_PCM_24BIT_PACKED -> 3
        AudioFormat.ENCODING_PCM_FLOAT,
        AudioFormat.ENCODING_PCM_32BIT -> Int.SIZE_BYTES
        else -> error("Unsupported USB output encoding $encoding")
    }

    private fun usbEncodingPreference(encoding: Int): Int = when (encoding) {
        AudioFormat.ENCODING_PCM_FLOAT -> 0
        AudioFormat.ENCODING_PCM_32BIT -> 1
        AudioFormat.ENCODING_PCM_24BIT_PACKED -> 2
        AudioFormat.ENCODING_PCM_16BIT -> 3
        else -> Int.MAX_VALUE
    }

    @SuppressLint("MissingPermission")
    private fun buildAudioRecord(encoding: Int, sampleRate: Int, bufferSizeBytes: Int): AudioRecord {
        if (!hasRecordPermission()) {
            Timber.e("buildAudioRecord: RECORD_AUDIO not granted")
            throw RuntimeException("RECORD_AUDIO not granted")
        }

        val format = AudioFormat.Builder()
            .setEncoding(encoding)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
            .build()
        val minimumBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_STEREO,
            encoding,
        ).coerceAtLeast(0)
        val recordBufferSize = maxOf(bufferSizeBytes, minimumBufferSize)

        val configBuilder = AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)

        val excluded = (if(excludeRestrictedSessions)
            sessionManager.sessionPolicyDatabase.getRestrictedUids().toList()
        else {
            sessionManager.pollOnce(false)
            emptyList()
        }).toMutableList()

        blockedApps.value?.map { it.uid }?.let {
            excluded += it
        }
        excluded += Process.myUid()

        excluded.forEach { configBuilder.excludeUid(it) }
        sessionManager.sessionDatabase.setExcludedUids(excluded.toTypedArray())
        sessionManager.pollOnce(false)

        Timber.d("buildAudioRecord: Excluded UIDs: ${excluded.joinToString("; ")}")

        return AudioRecord.Builder()
            .setAudioFormat(format)
            .setBufferSizeInBytes(recordBufferSize)
            .setAudioPlaybackCaptureConfig(configBuilder.build())
            .build()
    }

    // Determine HAL sampling rate
    private fun determineSamplingRate(): Int {
        if (isUsbOutputConnected() && followUsbHardwareRate()) {
            activeUsbHardwareSampleRate
                .takeIf(AudioSampleRateDetector::isSupportedProcessingRate)
                ?.let { hardwareRate ->
                    Timber.i("Using physical USB sampling rate $hardwareRate")
                    return hardwareRate
                }

            UsbHardwareSampleRateDetector.detect(this)
                .takeIf(AudioSampleRateDetector::isSupportedProcessingRate)
                ?.let { hardwareRate ->
                    activeUsbHardwareSampleRate = hardwareRate
                    Timber.i("Using detected physical USB sampling rate $hardwareRate")
                    return hardwareRate
                }
        }

        activeContentSampleRate
            .takeIf(AudioSampleRateDetector::isSupportedProcessingRate)
            ?.let { contentRate ->
                Timber.i("Using active content sampling rate $contentRate")
                return contentRate
            }

        val srate = AudioSampleRateDetector.getActiveOutputSampleRate(audioManager)
        Timber.i("Real HAL sampling rate is $srate")
        return srate
    }

    private fun detectActiveContentSampleRate(
        configs: Collection<AudioPlaybackConfiguration>?,
    ): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return 0

        return configs.orEmpty().mapNotNull { config ->
            try {
                val hidden = Refine.unsafeCast<AudioPlaybackConfigurationHidden>(config)
                val usage = config.audioAttributes.usage
                val capturedUsage = usage == AudioAttributes.USAGE_MEDIA ||
                    usage == AudioAttributes.USAGE_GAME ||
                    usage == AudioAttributes.USAGE_UNKNOWN
                hidden.getSampleRate().takeIf { rate ->
                    hidden.isActive() &&
                        hidden.getClientUid() != Process.myUid() &&
                        capturedUsage &&
                        AudioSampleRateDetector.isSupportedProcessingRate(rate)
                }
            } catch (exception: Throwable) {
                Timber.w(exception, "Failed to read active playback sample rate")
                null
            }
        }.maxOrNull() ?: 0
    }

    private fun applyActiveContentSampleRate(detectedRate: Int) {
        if (activeContentSampleRate != detectedRate) {
            Timber.i(
                "Active content sample rate changed from " +
                    "${activeContentSampleRate}Hz to ${detectedRate}Hz"
            )
            activeContentSampleRate = detectedRate
        }
        restartForSampleRateChange(determineSamplingRate())
    }

    // Determine HAL buffer size
    private fun determineBufferSize(): Int {
        val framesPerBuffer: String? = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
        return framesPerBuffer?.let { str -> Integer.parseInt(str).takeUnless { it == 0 } } ?: 256
    }

    companion object {
        private val SUPPORTED_USB_OUTPUT_ENCODINGS = setOf(
            AudioFormat.ENCODING_PCM_FLOAT,
            AudioFormat.ENCODING_PCM_32BIT,
            AudioFormat.ENCODING_PCM_24BIT_PACKED,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        private const val USB_ROUTE_SETTLE_DELAY_MS = 750L
        private const val USB_HARDWARE_RATE_SETTLE_DELAY_MS = 500L

        const val SESSION_LOSS_MAX_RETRIES = 1

        const val ACTION_START = BuildConfig.APPLICATION_ID + ".rootless.service.START"
        const val ACTION_STOP = BuildConfig.APPLICATION_ID + ".rootless.service.STOP"
        const val EXTRA_MEDIA_PROJECTION_DATA = "mediaProjectionData"
        const val EXTRA_APP_UID = "uid"
        const val EXTRA_APP_COMPAT_INTERNAL_CALL = "appCompatInternalCall"

        fun start(context: Context, data: Intent?) {
            try {
                context.startForegroundService(ServiceNotificationHelper.createStartIntent(context, data))
            }
            catch(ex: Exception) {
                CrashlyticsImpl.recordException(ex)
            }
        }

        fun stop(context: Context) {
            try {
                context.startForegroundService(ServiceNotificationHelper.createStopIntent(context))
            }
            catch(ex: Exception) {
                CrashlyticsImpl.recordException(ex)
            }
        }
    }
}
