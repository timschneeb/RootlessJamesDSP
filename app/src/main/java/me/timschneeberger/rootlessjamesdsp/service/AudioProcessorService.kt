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
import androidx.core.app.ActivityCompat
import me.timschneeberger.rootlessjamesdsp.session.AudioSessionManager
import me.timschneeberger.rootlessjamesdsp.session.MutedSessionManager
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.utils.ServiceNotificationHelper
import me.timschneeberger.rootlessjamesdsp.model.AudioEncoding
import me.timschneeberger.rootlessjamesdsp.model.MutedSessionEntry
import me.timschneeberger.rootlessjamesdsp.model.ProcessorMessage
import me.timschneeberger.rootlessjamesdsp.model.SessionRecordingPolicyEntry
import me.timschneeberger.rootlessjamesdsp.native.JamesDspEngine
import me.timschneeberger.rootlessjamesdsp.native.JamesDspWrapper
import me.timschneeberger.rootlessjamesdsp.session.SessionRecordingPolicyManager
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.Constants.ACTION_PROCESSOR_MESSAGE
import me.timschneeberger.rootlessjamesdsp.utils.Constants.ACTION_SERVICE_HARD_REBOOT_CORE
import me.timschneeberger.rootlessjamesdsp.utils.Constants.ACTION_SERVICE_RELOAD_LIVEPROG
import me.timschneeberger.rootlessjamesdsp.utils.Constants.ACTION_SERVICE_SOFT_REBOOT_CORE
import me.timschneeberger.rootlessjamesdsp.utils.Constants.ACTION_UPDATE_PREFERENCES
import me.timschneeberger.rootlessjamesdsp.utils.Constants.CHANNEL_ID_SERVICE
import me.timschneeberger.rootlessjamesdsp.utils.Constants.CHANNEL_ID_SESSION_LOSS
import me.timschneeberger.rootlessjamesdsp.utils.Constants.NOTIFICATION_ID_PERMISSION_PROMPT
import me.timschneeberger.rootlessjamesdsp.utils.Constants.NOTIFICATION_ID_SERVICE
import me.timschneeberger.rootlessjamesdsp.utils.Constants.NOTIFICATION_ID_SESSION_LOSS
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.registerLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.sendLocalBroadcast
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.unregisterLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.SystemServices
import timber.log.Timber
import java.io.IOException
import java.io.Serializable
import java.lang.Exception
import java.lang.RuntimeException


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
        audioSessionManager.sessionDatabase.registerOnSessionChangeListener(onSessionChangeListener)
        audioSessionManager.sessionPolicyDatabase.registerOnRestrictedSessionChangeListener(onSessionPolicyChangeListener)

        // Setup core engine
        engine = JamesDspEngine(this, engineCallbacks)
        engine.syncWithPreferences()

        // Setup general-purpose broadcast receiver
        val filter = IntentFilter()
        filter.addAction(ACTION_UPDATE_PREFERENCES)
        filter.addAction(ACTION_SERVICE_RELOAD_LIVEPROG)
        filter.addAction(ACTION_SERVICE_HARD_REBOOT_CORE)
        filter.addAction(ACTION_SERVICE_SOFT_REBOOT_CORE)
        registerLocalReceiver(broadcastReceiver, filter)

        // Setup shared preferences
        sharedPreferences = getSharedPreferences(Constants.PREF_APP, Context.MODE_PRIVATE)
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferencesListener)
        loadFromPreferences(getString(R.string.key_powersave_suspend))
        loadFromPreferences(getString(R.string.key_session_exclude_restricted))

        // Register notification channels
        val channel = NotificationChannel(
            CHANNEL_ID_SERVICE,
            getString(R.string.app_name),
            NotificationManager.IMPORTANCE_NONE
        )
        val channelSessionLoss = NotificationChannel(
            CHANNEL_ID_SESSION_LOSS,
            getString(R.string.notification_channel_session_loss_alert),
            NotificationManager.IMPORTANCE_HIGH
        )

        notificationManager.createNotificationChannel(channel)
        notificationManager.createNotificationChannel(channelSessionLoss)
        notificationManager.cancel(NOTIFICATION_ID_PERMISSION_PROMPT)

        // No need to recreate in this stage
        recreateRecorderRequested = false

        // Launch foreground service
        val notification = ServiceNotificationHelper.createServiceNotification(this, null)
        startForeground(
            NOTIFICATION_ID_SERVICE,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        )
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        // Handle intent action
        when (intent.action) {
            null -> {
                // TODO recreated service has its intent object null -> mediaProjectionData not persistent
                Timber.tag(TAG).w("onStartCommand: intent.action is null")
            }
            ACTION_START -> {
                Timber.tag(TAG).d("Starting service")
            }
            ACTION_STOP -> {
                Timber.tag(TAG).d("Stopping service")
                stopSelf()
                return START_NOT_STICKY
            }
        }

        if (isRunning) {
            return START_NOT_STICKY
        }

        // Cancel outdated notifications
        notificationManager.cancel(NOTIFICATION_ID_SESSION_LOSS)

        // Setup media projection
        mediaProjectionStartIntent = intent.getParcelableExtra(EXTRA_MEDIA_PROJECTION_DATA)
        mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, mediaProjectionStartIntent!!)
        mediaProjection?.registerCallback(projectionCallback, Handler(Looper.getMainLooper()))

        if (mediaProjection != null) {
            startRecording()
        } else {
            Timber.tag(TAG).w("Failed to capture audio")
            stopSelf()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        isServiceDisposing = true

        // Stop foreground service
        stopForeground(true)

        // Notify app about service termination
        sendLocalBroadcast(Intent(Constants.ACTION_SERVICE_STOPPED))

        // Unregister receivers and release resources
        unregisterLocalReceiver(broadcastReceiver)
        mediaProjection?.unregisterCallback(projectionCallback)

        audioSessionManager.sessionPolicyDatabase.unregisterOnRestrictedSessionChangeListener(onSessionPolicyChangeListener)
        audioSessionManager.sessionDatabase.unregisterOnSessionChangeListener(onSessionChangeListener)
        audioSessionManager.destroy()

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
            Timber.tag(TAG).w("Capture permission revoked. Stopping service.")

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
                ACTION_UPDATE_PREFERENCES -> engine.syncWithPreferences()
                ACTION_SERVICE_RELOAD_LIVEPROG -> engine.syncWithPreferences(arrayOf(Constants.PREF_LIVEPROG))
                ACTION_SERVICE_HARD_REBOOT_CORE -> restartRecording()
                ACTION_SERVICE_SOFT_REBOOT_CORE -> requestAudioRecordRecreation()
            }
        }
    }

    // Session loss listener
    private val onSessionLossListener = object: MutedSessionManager.OnSessionLossListener {
        override fun onSessionLost(sid: Int) {
            // Check if retry count exceeded
            if(sessionLossRetryCount < SESSION_LOSS_MAX_RETRIES) {
                // Retry
                sessionLossRetryCount++
                Timber.tag(TAG).d("Session lost. Retry count: $sessionLossRetryCount/$SESSION_LOSS_MAX_RETRIES")
                audioSessionManager.pollOnce(false)
                restartRecording()
                return
            }
            else {
                Timber.tag(TAG).d("Giving up on saving session. User interaction required.")
            }

            // Push notification if enabled
            val ignore = sharedPreferences.getBoolean(getString(R.string.key_session_loss_ignore), false)
            if(!ignore) {
                // Request users attention
                notificationManager.cancel(NOTIFICATION_ID_SERVICE)
                ServiceNotificationHelper.pushSessionLossNotification(this@AudioProcessorService, mediaProjectionStartIntent)
                Toast.makeText(this@AudioProcessorService, getString(R.string.session_control_loss_toast), Toast.LENGTH_SHORT).show()
                Timber.tag(TAG).w("Terminating service due to session loss")
                stopSelf()
            }
            else {
                // Hint at failure
                Toast.makeText(this@AudioProcessorService, getString(R.string.session_control_loss_toast_short), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Session change listener
    private val onSessionChangeListener = object : MutedSessionManager.OnSessionChangeListener {
        override fun onSessionChanged(sessionList: HashMap<Int, MutedSessionEntry>) {
            isProcessorIdle = sessionList.size == 0
            Timber.tag(TAG).d("onSessionChanged: isProcessorIdle=$isProcessorIdle")

            ServiceNotificationHelper.pushServiceNotification(
                this@AudioProcessorService,
                sessionList.map { it.value.audioSession }.toTypedArray()
            )
        }
    }

    // Session policy change listener
    private val onSessionPolicyChangeListener = object : SessionRecordingPolicyManager.OnSessionRecordingPolicyChangeListener {
        override fun onSessionRecordingPolicyChanged(sessionList: HashMap<String, SessionRecordingPolicyEntry>, isMinorUpdate: Boolean) {
            // TODO add disable setting

            if(!isMinorUpdate) {
                Timber.tag(TAG).d("onRestrictedSessionChanged: major update detected; requesting soft-reboot")
                requestAudioRecordRecreation()
            }
            else {
                Timber.tag(TAG).d("onRestrictedSessionChanged: minor update detected")
            }
        }
    }

    private fun loadFromPreferences(key: String){
        when (key) {
            getString(R.string.key_powersave_suspend) -> {
                suspendOnIdle = sharedPreferences.getBoolean(key, true)
                Timber.tag(TAG).d("Suspend on idle set to $suspendOnIdle")
            }
            getString(R.string.key_session_exclude_restricted) -> {
                excludeRestrictedSessions = sharedPreferences.getBoolean(key, true)
                Timber.tag(TAG).d("Exclude restricted set to $excludeRestrictedSessions")

                requestAudioRecordRecreation()
            }
        }
    }

    // Request recreation of the AudioRecord object to update AudioPlaybackRecordingConfiguration
    fun requestAudioRecordRecreation() {
        if(isProcessorDisposing || isServiceDisposing) {
            Timber.tag(TAG).e("recreateAudioRecorder: service or processor already disposing")
            return
        }

        recreateRecorderRequested = true
    }

    // Start recording thread
    @SuppressLint("BinaryOperationInTimber")
    private fun startRecording() {
        // Sanity check
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Timber.tag(TAG).e("Record audio permission missing. Can't record")
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

        Timber.tag(TAG).i("Sample rate: $sampleRate; Encoding: ${encoding.name}; " +
                "Buffer size: $bufferSize; Buffer size (bytes): $bufferSizeBytes ; " +
                "HAL buffer size (bytes): ${determineBufferSize()}")

        // Create recorder and track
        var recorder = buildAudioRecord(encodingFormat, sampleRate, bufferSizeBytes)
        val track = buildAudioTrack(encodingFormat, sampleRate, bufferSizeBytes)

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
                            Timber.tag(TAG).w(e)
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
                Timber.tag(TAG).e(e)
                // ignore
            } catch (e: Exception) {
                Timber.tag(TAG).e("Exception in recorderThread raised")
                Timber.tag(TAG).e(e)
                stopSelf()
            } finally {
                // Clean up recorder and track
                recorder.stop()
                track.stop()
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
            Timber.tag(TAG).e("restartRecording: service or processor already disposing")
            return
        }

        stopRecording()
        isProcessorDisposing = false
        recreateRecorderRequested = false
        startRecording()
    }

    private fun buildAudioTrack(encoding: Int, sampleRate: Int, bufferSizeBytes: Int): AudioTrack {
        return AudioTrack.Builder().setAudioFormat(
            AudioFormat.Builder()
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .setEncoding(encoding)
                .setSampleRate(sampleRate)
                .build())
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setAudioAttributes(AudioAttributes.Builder()
                .setAllowedCapturePolicy(AudioAttributes.ALLOW_CAPTURE_BY_NONE)
                .setUsage(AudioAttributes.USAGE_UNKNOWN)
                .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                .setFlags(0)
                .build())
            .setBufferSizeInBytes(bufferSizeBytes)
            .build()
    }

    private fun buildAudioRecord(encoding: Int, sampleRate: Int, bufferSizeBytes: Int): AudioRecord {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Timber.tag(TAG).e("buildAudioRecord: RECORD_AUDIO not granted")
            throw RuntimeException("RECORD_AUDIO not granted")
        }

        val format = AudioFormat.Builder()
            .setEncoding(encoding)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
            .build()

        val configBuilder =  AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)

        if(excludeRestrictedSessions) {
            val excluded = audioSessionManager.sessionPolicyDatabase.getRestrictedUids()
            audioSessionManager.sessionDatabase.setExcludedUids(excluded)
            audioSessionManager.pollOnce(false)

            excluded.forEach { configBuilder.excludeUid(it) }
            Timber.d("buildAudioRecord: Excluded UIDs: %s",
                excluded.joinToString("; ") { it.toString() })
        }
        else {
            audioSessionManager.sessionDatabase.setExcludedUids(arrayOf())
            audioSessionManager.pollOnce(false)
        }

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
            Timber.tag(TAG).v("onLiveprogExec: $id")
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
            Timber.tag(TAG).v("onLiveprogResult: $resultCode; message: $errorMessage")
        }

        override fun onVdcParseError() {
            broadcastProcessorMessage(ProcessorMessage.Type.VdcParseError)
            Timber.tag(TAG).v("onVdcParseError")
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
        const val TAG = "AudioProcessorService"
        const val SESSION_LOSS_MAX_RETRIES = 1

        const val ACTION_START = ".service.START"
        const val ACTION_STOP = ".service.STOP"
        const val EXTRA_MEDIA_PROJECTION_DATA = "mediaProjectionData"

        private const val MSG_PROCESSOR_READY = 1

        fun start(context: Context, data: Intent?) {
            context.startForegroundService(ServiceNotificationHelper.createStartIntent(context, data))
        }

        fun stop(context: Context) {
            context.startForegroundService(ServiceNotificationHelper.createStopIntent(context))
        }
    }
}
