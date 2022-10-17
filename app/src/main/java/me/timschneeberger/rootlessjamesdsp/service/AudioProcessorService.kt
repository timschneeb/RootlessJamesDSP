package me.timschneeberger.rootlessjamesdsp.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.*
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.session.AudioSessionManager
import me.timschneeberger.rootlessjamesdsp.session.MutedSessionManager
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.utils.ServiceNotificationHelper
import me.timschneeberger.rootlessjamesdsp.model.preference.AudioEncoding
import me.timschneeberger.rootlessjamesdsp.model.MutedSessionEntry
import me.timschneeberger.rootlessjamesdsp.model.ProcessorMessage
import me.timschneeberger.rootlessjamesdsp.model.SessionRecordingPolicyEntry
import me.timschneeberger.rootlessjamesdsp.interop.JamesDspEngine
import me.timschneeberger.rootlessjamesdsp.interop.JamesDspWrapper
import me.timschneeberger.rootlessjamesdsp.model.AudioSessionEntry
import me.timschneeberger.rootlessjamesdsp.model.room.AppBlocklistDatabase
import me.timschneeberger.rootlessjamesdsp.model.room.AppBlocklistRepository
import me.timschneeberger.rootlessjamesdsp.model.room.BlockedApp
import me.timschneeberger.rootlessjamesdsp.session.SessionRecordingPolicyManager
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.Constants.ACTION_PROCESSOR_MESSAGE
import me.timschneeberger.rootlessjamesdsp.utils.Constants.ACTION_SERVICE_HARD_REBOOT_CORE
import me.timschneeberger.rootlessjamesdsp.utils.Constants.ACTION_SERVICE_RELOAD_LIVEPROG
import me.timschneeberger.rootlessjamesdsp.utils.Constants.ACTION_SERVICE_SOFT_REBOOT_CORE
import me.timschneeberger.rootlessjamesdsp.utils.Constants.ACTION_PREFERENCES_UPDATED
import me.timschneeberger.rootlessjamesdsp.utils.Constants.CHANNEL_ID_APP_INCOMPATIBILITY
import me.timschneeberger.rootlessjamesdsp.utils.Constants.CHANNEL_ID_SERVICE
import me.timschneeberger.rootlessjamesdsp.utils.Constants.CHANNEL_ID_SESSION_LOSS
import me.timschneeberger.rootlessjamesdsp.utils.Constants.NOTIFICATION_ID_APP_INCOMPATIBILITY
import me.timschneeberger.rootlessjamesdsp.utils.Constants.NOTIFICATION_ID_PERMISSION_PROMPT
import me.timschneeberger.rootlessjamesdsp.utils.Constants.NOTIFICATION_ID_SERVICE
import me.timschneeberger.rootlessjamesdsp.utils.Constants.NOTIFICATION_ID_SESSION_LOSS
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.registerLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.sendLocalBroadcast
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.unregisterLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.SystemServices
import me.timschneeberger.rootlessjamesdsp.utils.concatenate
import timber.log.Timber
import java.io.IOException
import java.io.Serializable
import java.lang.Exception
import java.lang.RuntimeException


@RequiresApi(Build.VERSION_CODES.Q)
class AudioProcessorService : Service() {
    // System services
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var audioManager: AudioManager

    // Media projection token
    private var mediaProjection: MediaProjection? = null
    private var mediaProjectionStartIntent: Intent? = null

    // Processing
    private var recreateRecorderRequested = false
    private var recorderThread: Thread? = null
    private val engineCallbacks = EngineCallbacks()
    private val handler: Handler = StartupHandler(this)
    private lateinit var engine: JamesDspEngine
    private val isRunning: Boolean
        get() = recorderThread != null

    // Session management
    private lateinit var audioSessionManager: AudioSessionManager
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
    private lateinit var sharedPreferences: SharedPreferences

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

    // Remote binder access
    private val binder: IBinder = LocalBinder()

    inner class LocalBinder : Binder() {
        val service: AudioProcessorService
            get() = this@AudioProcessorService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()

        // Get reference to system services
        audioManager = SystemServices.get(this, AudioManager::class.java)
        mediaProjectionManager = SystemServices.get(this, MediaProjectionManager::class.java)
        notificationManager = SystemServices.get(this, NotificationManager::class.java)

        // Setup session manager
        audioSessionManager = AudioSessionManager(this)
        audioSessionManager.sessionDatabase.setOnSessionLossListener(onSessionLossListener)
        audioSessionManager.sessionDatabase.setOnAppProblemListener(onAppProblemListener)
        audioSessionManager.sessionDatabase.registerOnSessionChangeListener(onSessionChangeListener)
        audioSessionManager.sessionPolicyDatabase.registerOnRestrictedSessionChangeListener(onSessionPolicyChangeListener)

        // Setup core engine
        engine = JamesDspEngine(this, engineCallbacks)
        engine.syncWithPreferences()

        // Setup general-purpose broadcast receiver
        val filter = IntentFilter()
        filter.addAction(ACTION_PREFERENCES_UPDATED)
        filter.addAction(ACTION_SERVICE_RELOAD_LIVEPROG)
        filter.addAction(ACTION_SERVICE_HARD_REBOOT_CORE)
        filter.addAction(ACTION_SERVICE_SOFT_REBOOT_CORE)
        registerLocalReceiver(broadcastReceiver, filter)

        // Setup shared preferences
        sharedPreferences = getSharedPreferences(Constants.PREF_APP, Context.MODE_PRIVATE)
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferencesListener)
        loadFromPreferences(getString(R.string.key_powersave_suspend))
        loadFromPreferences(getString(R.string.key_session_exclude_restricted))

        // Setup database observer
        blockedApps.observeForever(blockedAppObserver)

        // Register notification channels
        val channel = NotificationChannel(
            CHANNEL_ID_SERVICE,
            getString(R.string.notification_channel_service),
            NotificationManager.IMPORTANCE_NONE
        )
        val channelSessionLoss = NotificationChannel(
            CHANNEL_ID_SESSION_LOSS,
            getString(R.string.notification_channel_session_loss_alert),
            NotificationManager.IMPORTANCE_HIGH
        )
        val channelAppCompatIssue = NotificationChannel(
            CHANNEL_ID_APP_INCOMPATIBILITY,
            getString(R.string.notification_channel_app_compat_alert),
            NotificationManager.IMPORTANCE_HIGH
        )

        notificationManager.createNotificationChannel(channel)
        notificationManager.createNotificationChannel(channelSessionLoss)
        notificationManager.createNotificationChannel(channelAppCompatIssue)
        notificationManager.cancel(NOTIFICATION_ID_PERMISSION_PROMPT)

        // No need to recreate in this stage
        recreateRecorderRequested = false

        // Launch foreground service
        val notification = ServiceNotificationHelper.createServiceNotification(this, null)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID_SERVICE,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            throw NotImplementedError() // TODO
            startForeground(
                NOTIFICATION_ID_SERVICE,
                notification
            )
        }
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
        notificationManager.cancel(NOTIFICATION_ID_SESSION_LOSS)
        notificationManager.cancel(NOTIFICATION_ID_APP_INCOMPATIBILITY)

        // Setup media projection
        mediaProjectionStartIntent = intent.getParcelableExtra(EXTRA_MEDIA_PROJECTION_DATA)

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
        } else {
            Timber.w("Failed to capture audio")
            stopSelf()
        }

        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        super.onDestroy()

        isServiceDisposing = true

        // Stop foreground service
        stopForeground(true)

        // Notify app about service termination
        sendLocalBroadcast(Intent(Constants.ACTION_SERVICE_STOPPED))

        // Unregister database observer
        blockedApps.removeObserver(blockedAppObserver)

        // Unregister receivers and release resources
        unregisterLocalReceiver(broadcastReceiver)
        mediaProjection?.unregisterCallback(projectionCallback)
        mediaProjection = null

        audioSessionManager.sessionPolicyDatabase.unregisterOnRestrictedSessionChangeListener(onSessionPolicyChangeListener)
        audioSessionManager.sessionDatabase.unregisterOnSessionChangeListener(onSessionChangeListener)
        audioSessionManager.destroy()

        notificationManager.cancel(NOTIFICATION_ID_SERVICE)

        // Stop recording and release resources
        stopRecording()
        engine.close()
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

            if(getSharedPreferences(Constants.PREF_VAR, Context.MODE_PRIVATE)
                    .getBoolean(getString(R.string.key_is_activity_active), false)) {
                // Activity in foreground, toast too disruptive
                return
            }

            Timber.w("Capture permission revoked. Stopping service.")

            sendLocalBroadcast(Intent(Constants.ACTION_DISCARD_AUTHORIZATION))

            Toast.makeText(this@AudioProcessorService,
                getString(R.string.capture_permission_revoked_toast), Toast.LENGTH_LONG).show()
            notificationManager.cancel(NOTIFICATION_ID_SERVICE)
            stopSelf()
        }
    }

    // General purpose broadcast receiver
    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_PREFERENCES_UPDATED -> engine.syncWithPreferences()
                ACTION_SERVICE_RELOAD_LIVEPROG -> engine.syncWithPreferences(arrayOf(Constants.PREF_LIVEPROG))
                ACTION_SERVICE_HARD_REBOOT_CORE -> restartRecording()
                ACTION_SERVICE_SOFT_REBOOT_CORE -> requestAudioRecordRecreation()
            }
        }
    }

    // Session loss listener
    private val onSessionLossListener = object: MutedSessionManager.OnSessionLossListener {
        override fun onSessionLost(sid: Int) {
            // Push notification if enabled
            val ignore = sharedPreferences.getBoolean(getString(R.string.key_session_loss_ignore), false)
            if(!ignore) {
                // Check if retry count exceeded
                if(sessionLossRetryCount < SESSION_LOSS_MAX_RETRIES) {
                    // Retry
                    sessionLossRetryCount++
                    Timber.d("Session lost. Retry count: $sessionLossRetryCount/$SESSION_LOSS_MAX_RETRIES")
                    audioSessionManager.pollOnce(false)
                    restartRecording()
                    return
                }
                else {
                    sessionLossRetryCount = 0
                    Timber.d("Giving up on saving session. User interaction required.")
                }

                // Request users attention
                notificationManager.cancel(NOTIFICATION_ID_SERVICE)
                ServiceNotificationHelper.pushSessionLossNotification(this@AudioProcessorService, mediaProjectionStartIntent)
                Toast.makeText(this@AudioProcessorService, getString(R.string.session_control_loss_toast), Toast.LENGTH_SHORT).show()
                Timber.w("Terminating service due to session loss")
                stopSelf()
            }
        }
    }

    // Session change listener
    private val onSessionChangeListener = object : MutedSessionManager.OnSessionChangeListener {
        override fun onSessionChanged(sessionList: HashMap<Int, MutedSessionEntry>) {
            isProcessorIdle = sessionList.size == 0
            Timber.d("onSessionChanged: isProcessorIdle=$isProcessorIdle")

            ServiceNotificationHelper.pushServiceNotification(
                this@AudioProcessorService,
                sessionList.map { it.value.audioSession }.toTypedArray()
            )
        }
    }

    // App problem listener
    private val onAppProblemListener = object : MutedSessionManager.OnAppProblemListener {
        override fun onAppProblemDetected(data: AudioSessionEntry) {
            // Push notification if enabled
            val ignore = sharedPreferences.getBoolean(getString(R.string.key_session_app_problem_ignore), false)
            if(!ignore) {
                // Request users attention
                notificationManager.cancel(NOTIFICATION_ID_SERVICE)

                // Determine if we should redirect instantly, or push a non-intrusive notification
                val prefsVar = this@AudioProcessorService.getSharedPreferences(Constants.PREF_VAR, Context.MODE_PRIVATE)
                if(prefsVar.getBoolean(getString(R.string.key_is_activity_active), false) ||
                    prefsVar.getBoolean(getString(R.string.key_is_app_compat_activity_active), false)) {
                    startActivity(
                        ServiceNotificationHelper.createAppTroubleshootIntent(
                            this@AudioProcessorService,
                            mediaProjectionStartIntent,
                            data,
                            directLaunch = true
                        )
                    )
                    notificationManager.cancel(NOTIFICATION_ID_APP_INCOMPATIBILITY)
                }
                else
                    ServiceNotificationHelper.pushAppIssueNotification(this@AudioProcessorService, mediaProjectionStartIntent, data)

                Toast.makeText(this@AudioProcessorService, getString(R.string.session_app_compat_toast), Toast.LENGTH_SHORT).show()
                Timber.w("Terminating service due to app incompatibility; redirect user to troubleshooting options")
                stopSelf()
            }
        }
    }

    // Session policy change listener
    private val onSessionPolicyChangeListener = object : SessionRecordingPolicyManager.OnSessionRecordingPolicyChangeListener {
        override fun onSessionRecordingPolicyChanged(sessionList: HashMap<String, SessionRecordingPolicyEntry>, isMinorUpdate: Boolean) {
            if(!this@AudioProcessorService.excludeRestrictedSessions) {
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

    private fun loadFromPreferences(key: String){
        when (key) {
            getString(R.string.key_powersave_suspend) -> {
                suspendOnIdle = sharedPreferences.getBoolean(key, true)
                Timber.d("Suspend on idle set to $suspendOnIdle")
            }
            getString(R.string.key_session_exclude_restricted) -> {
                excludeRestrictedSessions = sharedPreferences.getBoolean(key, true)
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Timber.e("Record audio permission missing. Can't record")
            stopSelf()
            return
        }

        // Load preferences
        val encoding = AudioEncoding.fromInt(
            sharedPreferences.getString(getString(R.string.key_audioformat_encoding), "1")
                ?.toIntOrNull() ?: 1
        )
        val bufferSize = sharedPreferences.getFloat(getString(R.string.key_audioformat_buffersize), 2048f).toInt()
        val bufferSizeBytes = when (encoding) {
            AudioEncoding.PcmFloat -> bufferSize * Float.SIZE_BYTES
            else -> bufferSize * Short.SIZE_BYTES
        }
        val encodingFormat = when (encoding) {
            AudioEncoding.PcmShort -> AudioFormat.ENCODING_PCM_16BIT
            else -> AudioFormat.ENCODING_PCM_FLOAT
        }
        val sampleRate = determineSamplingRate()

        Timber.i("Sample rate: $sampleRate; Encoding: ${encoding.name}; " +
                "Buffer size: $bufferSize; Buffer size (bytes): $bufferSizeBytes ; " +
                "HAL buffer size (bytes): ${determineBufferSize()}")

        // Create recorder and track
        var recorder = buildAudioRecord(encodingFormat, sampleRate, bufferSizeBytes)
        val track = buildAudioTrack(encodingFormat, sampleRate, bufferSizeBytes)

        if(engine.sampleRate.toInt() != sampleRate) {
            Timber.d("Sampling rate changed to ${sampleRate}Hz")
            engine.setSamplingRate(sampleRate.toFloat())
        }

        // TODO Move all audio-related code to C++
        recorderThread = Thread {
            try {
                handler.sendEmptyMessage(MSG_PROCESSOR_READY)

                val floatBuffer = FloatArray(bufferSize)
                val shortBuffer = ShortArray(bufferSize)
                while (!isProcessorDisposing) {
                    if(recreateRecorderRequested) {
                        recreateRecorderRequested = false
                        Timber.d("Recreating recorder without stopping thread...")

                        // Suspend track, release recorder
                        recorder.stop()
                        track.stop()
                        recorder.release()

                        // Recreate recorder with new AudioPlaybackRecordingConfiguration
                        recorder = buildAudioRecord(encodingFormat, sampleRate, bufferSizeBytes)
                        Timber.d("Recorder recreated")
                    }

                    // Suspend core while idle
                    if(isProcessorIdle && suspendOnIdle)
                    {
                        recorder.stop()
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
                    if(recorder.state == AudioRecord.RECORDSTATE_STOPPED) {
                        recorder.startRecording()
                    }
                    // Resume track if suspended
                    if(track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                        track.play()
                    }

                    // Choose encoding and process data
                    if(encoding == AudioEncoding.PcmShort) {
                        recorder.read(shortBuffer, 0, shortBuffer.size, AudioRecord.READ_BLOCKING)
                        val output = engine.processInt16(shortBuffer)
                        track.write(output, 0, output.size, AudioTrack.WRITE_BLOCKING)
                    }
                    else {
                        recorder.read(floatBuffer, 0, floatBuffer.size, AudioRecord.READ_BLOCKING)
                        val output = engine.processFloat(floatBuffer)
                        track.write(output, 0, output.size, AudioTrack.WRITE_BLOCKING)
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

    private fun buildAudioTrack(encoding: Int, sampleRate: Int, bufferSizeBytes: Int): AudioTrack {
        val attributesBuilder = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_UNKNOWN)
                .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                .setFlags(0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            attributesBuilder.setAllowedCapturePolicy(AudioAttributes.ALLOW_CAPTURE_BY_NONE)
        }

        return AudioTrack.Builder().setAudioFormat(
            AudioFormat.Builder()
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .setEncoding(encoding)
                .setSampleRate(sampleRate)
                .build())
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setAudioAttributes(attributesBuilder.build())
            .setBufferSizeInBytes(bufferSizeBytes)
            .build()
    }

    private fun buildAudioRecord(encoding: Int, sampleRate: Int, bufferSizeBytes: Int): AudioRecord {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Timber.e("buildAudioRecord: RECORD_AUDIO not granted")
            throw RuntimeException("RECORD_AUDIO not granted")
        }

        val format = AudioFormat.Builder()
            .setEncoding(encoding)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
            .build()

        val configBuilder = AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)

        var excluded = if(excludeRestrictedSessions)
            audioSessionManager.sessionPolicyDatabase.getRestrictedUids().toList()
        else {
            audioSessionManager.pollOnce(false)
            emptyList()
        }

        blockedApps.value?.map { it.uid }?.let {
            excluded = concatenate(excluded, it)
        }

        excluded.forEach { configBuilder.excludeUid(it) }
        audioSessionManager.sessionDatabase.setExcludedUids(excluded.toTypedArray())
        audioSessionManager.pollOnce(false)

        Timber.d("buildAudioRecord: Excluded UIDs: ${excluded.joinToString("; ")}")

        return AudioRecord.Builder()
            .setAudioFormat(format)
            .setBufferSizeInBytes(bufferSizeBytes)
            .setAudioPlaybackCaptureConfig(configBuilder.build())
            .build()
    }


    // Broadcast processor message
    private fun broadcastProcessorMessage(type: ProcessorMessage.Type, params: Map<ProcessorMessage.Param, Serializable>? = null){
        val intent = Intent(ACTION_PROCESSOR_MESSAGE)
        intent.putExtra(ProcessorMessage.TYPE, type.value)
        params?.forEach { (k, v) ->
            intent.putExtra(k.name, v)
        }
        sendLocalBroadcast(intent)
    }

    // Engine callback connector
    private inner class EngineCallbacks : JamesDspWrapper.JamesDspCallbacks
    {
        override fun onLiveprogOutput(message: String) {
            broadcastProcessorMessage(ProcessorMessage.Type.LiveprogOutput, mapOf(
                ProcessorMessage.Param.LiveprogStdout to message
            ))
        }

        override fun onLiveprogExec(id: String) {
            broadcastProcessorMessage(ProcessorMessage.Type.LiveprogExec, mapOf(
                ProcessorMessage.Param.LiveprogFileId to id
            ))
            Timber.v("onLiveprogExec: $id")
        }

        override fun onLiveprogResult(
            resultCode: Int,
            id: String,
            errorMessage: String?
        ) {
            broadcastProcessorMessage(ProcessorMessage.Type.LiveprogResult, mapOf(
                ProcessorMessage.Param.LiveprogResultCode to resultCode,
                ProcessorMessage.Param.LiveprogFileId to id,
                ProcessorMessage.Param.LiveprogErrorMessage to (errorMessage ?: "")
            ))
            Timber.v("onLiveprogResult: $resultCode; message: $errorMessage")
        }

        override fun onVdcParseError() {
            broadcastProcessorMessage(ProcessorMessage.Type.VdcParseError)
            Timber.v("onVdcParseError")
        }
    }

    // Startup message handler
    private class StartupHandler(private val service: AudioProcessorService) :
        Handler(Looper.myLooper()!!) {
        override fun handleMessage(message: Message) {
            if (!service.isRunning) {
                // not running anymore, ignore events
                return
            }
            if (message.what == MSG_PROCESSOR_READY) {
                ServiceNotificationHelper.pushServiceNotification(service, arrayOf())
            }
        }
    }

    // Determine HAL sampling rate
    private fun determineSamplingRate(): Int {
        val sampleRateStr: String? = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        return sampleRateStr?.let { str -> Integer.parseInt(str).takeUnless { it == 0 } } ?: 48000
    }

    // Determine HAL buffer size
    private fun determineBufferSize(): Int {
        val framesPerBuffer: String? = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
        return framesPerBuffer?.let { str -> Integer.parseInt(str).takeUnless { it == 0 } } ?: 256
    }

    companion object {
        const val SESSION_LOSS_MAX_RETRIES = 1

        const val ACTION_START = BuildConfig.APPLICATION_ID + ".service.START"
        const val ACTION_STOP = BuildConfig.APPLICATION_ID + ".service.STOP"
        const val EXTRA_MEDIA_PROJECTION_DATA = "mediaProjectionData"
        const val EXTRA_APP_UID = "uid"
        const val EXTRA_APP_COMPAT_INTERNAL_CALL = "appCompatInternalCall"

        private const val MSG_PROCESSOR_READY = 1

        fun start(context: Context, data: Intent?) {
            context.startForegroundService(ServiceNotificationHelper.createStartIntent(context, data))
        }

        fun stop(context: Context) {
            context.startForegroundService(ServiceNotificationHelper.createStopIntent(context))
        }
    }
}
