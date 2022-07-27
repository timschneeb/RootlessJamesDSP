package me.timschneeberger.rootlessjamesdsp.service

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.*
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import me.timschneeberger.rootlessjamesdsp.*
import me.timschneeberger.rootlessjamesdsp.MainActivity
import me.timschneeberger.rootlessjamesdsp.model.MutedSessionEntry
import me.timschneeberger.rootlessjamesdsp.model.ProcessorMessage
import me.timschneeberger.rootlessjamesdsp.native.JamesDspEngine
import me.timschneeberger.rootlessjamesdsp.native.JamesDspWrapper
import me.timschneeberger.rootlessjamesdsp.utils.Constants
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


class AudioProcessorService : Service() {
    private val handler: Handler = StartupHandler(this)
    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var recorderThread: Thread? = null

    private lateinit var volumeContentObserver: VolumeContentObserver
    private lateinit var engine: JamesDspEngine
    private lateinit var audioSessionManager: AudioSessionManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var audioManager: AudioManager

    private val engineCallbacks = EngineCallbacks()
    private var mediaProjectionStartIntent: Intent? = null
    private var isStreamMuted = false
    private var isProcessorIdle = false
    private var suspendOnIdle = false

    private lateinit var sharedPreferences: SharedPreferences
    private val preferencesListener: SharedPreferences.OnSharedPreferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener {
            _, key ->
        loadFromPreferences(key)
    }

    private val mBinder: IBinder = LocalBinder()

    var isDisposing = false
        private set

    inner class LocalBinder : Binder() {
        val service: AudioProcessorService
            get() = this@AudioProcessorService
    }

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    override fun onCreate() {
        super.onCreate()

        sharedPreferences = getSharedPreferences(Constants.PREF_APP, Context.MODE_PRIVATE)

        audioManager = SystemServices.get(this, AudioManager::class.java)
        notificationManager = SystemServices.get(this, NotificationManager::class.java)

        audioSessionManager = AudioSessionManager(this)
        audioSessionManager.sessionDatabase.setOnSessionLossListener(object: MutedSessionManager.OnSessionLossListener {
            override fun onSessionLost(sid: Int) {
                val ignore = sharedPreferences.getBoolean(getString(R.string.key_session_loss_ignore), false)
                if(!ignore) {
                    notificationManager.cancel(NOTIFICATION_ID_SERVICE)
                    notifySessionLoss()
                    Toast.makeText(this@AudioProcessorService, getString(R.string.session_control_loss_toast), Toast.LENGTH_SHORT).show()
                    Timber.tag(TAG).w("Terminating service due to session loss")
                    stopSelf()
                }
                else {
                    Toast.makeText(this@AudioProcessorService, getString(R.string.session_control_loss_toast_short), Toast.LENGTH_SHORT).show()
                }
            }
        })

        audioSessionManager.sessionDatabase.registerOnSessionChangeListener(onSessionChangeListener)

        // TODO use this & add unregister call
        audioManager.registerAudioRecordingCallback(object: AudioManager.AudioRecordingCallback() {
            override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>?) {
                super.onRecordingConfigChanged(configs)
                configs?.forEach { it ->
                    Timber.i("================ onRecordingConfigChanged: csid=${it.clientAudioSessionId}; src=${it.audioSource}")
                    Timber.i("onRecordingConfigChanged: Is client silenced? ${it.isClientSilenced}")
                }
            }
        }, Handler(Looper.getMainLooper()))


        engine = JamesDspEngine(this, engineCallbacks)
        engine.syncWithPreferences()

        registerLocalReceiver(mPreferenceUpdateReceiver, IntentFilter(ACTION_UPDATE_PREFERENCES))

        sharedPreferences.registerOnSharedPreferenceChangeListener(preferencesListener)
        loadFromPreferences(getString(R.string.key_powersave_suspend))

        volumeContentObserver = VolumeContentObserver(this)
        volumeContentObserver.setOnVolumeChangeListener {
            isStreamMuted = it <= 0
            Timber.tag(TAG).d("Mute stream internally: $isStreamMuted")
        }

        val notification = createNotification(false)
        val channel = NotificationChannel(
            CHANNEL_ID_SERVICE,
            getString(R.string.app_name),
            NotificationManager.IMPORTANCE_NONE
        )
        notificationManager.createNotificationChannel(channel)

        val channelSessionLoss = NotificationChannel(
            CHANNEL_ID_SESSION_LOSS,
            getString(R.string.notification_channel_session_loss_alert),
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channelSessionLoss)

        notificationManager.cancel(NOTIFICATION_ID_PERMISSION_PROMPT)

        startForeground(
            NOTIFICATION_ID_SERVICE,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        )
    }

    private fun loadFromPreferences(key: String){
        when (key) {
            getString(R.string.key_powersave_suspend) -> {
                suspendOnIdle = sharedPreferences.getBoolean(key, true)
                Timber.tag(TAG).d("Suspend on idle set to $suspendOnIdle")
            }
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if(intent.action == null) {
            Timber.tag(TAG).w("onStartCommand: intent.action is null")
        }
        else if (intent.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (isRunning) {
            return START_NOT_STICKY
        }

        notificationManager.cancel(NOTIFICATION_ID_SESSION_LOSS)

        contentResolver.registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, volumeContentObserver)

        mediaProjectionStartIntent = intent.getParcelableExtra(EXTRA_MEDIA_PROJECTION_DATA)

        mediaProjectionManager = SystemServices.get(this, MediaProjectionManager::class.java)
        mediaProjection = mediaProjectionManager!!.getMediaProjection(Activity.RESULT_OK, mediaProjectionStartIntent!!)

        if (mediaProjection != null) {
            startRecording()
        } else {
            Timber.tag(TAG).w("Failed to capture audio")
            stopSelf()
        }

        return START_STICKY
    }

    private val mPreferenceUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            engine.syncWithPreferences()
        }
    }

    private val onSessionChangeListener = object : MutedSessionManager.OnSessionChangeListener {
        override fun onSessionChanged(sessionList: HashMap<Int, MutedSessionEntry>) {
            isProcessorIdle = sessionList.size == 0
            Timber.tag(TAG).d("onSessionChanged: isProcessorIdle=$isProcessorIdle")
        }
    }

    private fun determineSamplingRate(): Int {
        val sampleRateStr: String? = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        return sampleRateStr?.let { str ->
            Integer.parseInt(str).takeUnless { it == 0 }
        } ?: 48000
    }

    private fun determineBufferSize(): Int {
        val framesPerBuffer: String? = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
        return framesPerBuffer?.let { str ->
            Integer.parseInt(str).takeUnless { it == 0 }
        } ?: 256
    }

    private fun startRecording() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Timber.tag(TAG).e("Record audio permission missing. Can't record")
            stopSelf()
            return
        }

        // TODO
        val preferredBufferSize = determineBufferSize()
        val sampleRate = determineSamplingRate()

        var actualBufferSize = preferredBufferSize
        val recordMinBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val trackMinBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        // TODO get rid of this
        if(actualBufferSize < recordMinBuffer)
            actualBufferSize = recordMinBuffer
        if(actualBufferSize < trackMinBuffer)
            actualBufferSize = trackMinBuffer

        Timber.e("HAL buffer size: $preferredBufferSize; recordMinBuf: $recordMinBuffer; trackMinBuf: $trackMinBuffer --> $actualBufferSize") //TODO ch sev

        val recordBuilder = AudioRecord.Builder()
        val recordFormatBuilder = AudioFormat.Builder()
        recordFormatBuilder.setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        recordFormatBuilder.setSampleRate(sampleRate)
        recordFormatBuilder.setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
        recordBuilder.setAudioFormat(recordFormatBuilder.build())
        recordBuilder.setBufferSizeInBytes(actualBufferSize)
        recordBuilder.setAudioPlaybackCaptureConfig(createAudioPlaybackCaptureConfig(mediaProjection))
        val recorder = recordBuilder.build()

        val track = AudioTrack.Builder().setAudioFormat(
            AudioFormat.Builder()
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .build()
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setAudioAttributes(AudioAttributes.Builder()
                .setAllowedCapturePolicy(AudioAttributes.ALLOW_CAPTURE_BY_NONE)
                .setUsage(AudioAttributes.USAGE_UNKNOWN)
                .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                .setFlags(0)
                .build())
            .setBufferSizeInBytes(actualBufferSize)
            .build()

        track.setAuxEffectSendLevel(0.0f)
        track.play()

        // TODO Move all audio-related code to C++
        recorderThread = Thread {
            try {
                handler.sendEmptyMessage(MSG_PROCESSOR_READY)
                recorder.startRecording()
                val buf = ShortArray(actualBufferSize)
                while (!isDisposing) {
                    if(isProcessorIdle && suspendOnIdle)
                    {
                        recorder.stop()
                        track.stop()
                        Thread.sleep(50)
                        continue
                    }

                    if(recorder.state == AudioRecord.RECORDSTATE_STOPPED) {
                        recorder.startRecording()
                    }

                    recorder.read(buf, 0, buf.size)

                    val output = engine.processInt16(buf)
                    if(track.playState != AudioTrack.PLAYSTATE_PLAYING)
                    {
                        track.play()
                    }
                    track.write(output, 0, output.size)

                }
            } catch (e: IOException) {
                Timber.e(e)
                // ignore
            } finally {
                recorder.stop()
                track.stop()
                recorder.release()
                track.release()
                stopSelf()
            }
        }
        recorderThread!!.start()
    }

    private inner class EngineCallbacks : JamesDspWrapper.JamesDspCallbacks
    {
        override fun onLiveprogOutput(message: String) {
            sendProcessorMessage(ProcessorMessage.Type.LiveprogOutput, mapOf(
                ProcessorMessage.Param.LiveprogStdout to message
            ))
        }

        override fun onLiveprogExec(id: String) {
            sendProcessorMessage(ProcessorMessage.Type.LiveprogExec, mapOf(
                ProcessorMessage.Param.LiveprogFileId to id
            ))
            Timber.tag(TAG).v("onLiveprogExec: $id")
        }

        override fun onLiveprogResult(
            resultCode: Int,
            id: String,
            errorMessage: String?
        ) {
            sendProcessorMessage(ProcessorMessage.Type.LiveprogResult, mapOf(
                ProcessorMessage.Param.LiveprogResultCode to resultCode,
                ProcessorMessage.Param.LiveprogFileId to id,
                ProcessorMessage.Param.LiveprogErrorMessage to (errorMessage ?: "")
            ))
            Timber.tag(TAG).v("onLiveprogResult: $resultCode; message: $errorMessage")
        }

        override fun onVdcParseError() {
            sendProcessorMessage(ProcessorMessage.Type.VdcParseError)
            Timber.tag(TAG).v("onVdcParseError")
        }
    }

    private val isRunning: Boolean
        get() = recorderThread != null

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)

        sendLocalBroadcast(Intent(Constants.ACTION_SERVICE_STOPPED))

        audioSessionManager.sessionDatabase.unregisterOnSessionChangeListener(onSessionChangeListener)
        audioSessionManager.destroy()

        contentResolver.unregisterContentObserver(volumeContentObserver)

        if (recorderThread != null) {
            isDisposing = true
            recorderThread!!.interrupt()
            recorderThread!!.join(500)

            recorderThread = null
        }

        this.engine.close()

        unregisterLocalReceiver(mPreferenceUpdateReceiver)
    }

    private fun sendProcessorMessage(type: ProcessorMessage.Type, params: Map<ProcessorMessage.Param, Serializable>? = null){
        val intent = Intent(ACTION_PROCESSOR_MESSAGE)
        intent.putExtra(ProcessorMessage.TYPE, type.value)
        params?.forEach { (k, v) ->
            intent.putExtra(k.name, v)
        }
        sendLocalBroadcast(intent)
    }

    private class StartupHandler(private val service: AudioProcessorService) :
        Handler(Looper.myLooper()!!) {
        override fun handleMessage(message: Message) {
            if (!service.isRunning) {
                // not running anymore, ignore events
                return
            }
            if (message.what == MSG_PROCESSOR_READY) {
                val notification = service.createNotification(true)
                service.notificationManager.notify(NOTIFICATION_ID_SERVICE, notification)
            }
        }
    }

    private fun createNotification(established: Boolean): Notification {
        val intent = Intent(applicationContext, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        val contentIntent =
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = Notification.Builder(this, CHANNEL_ID_SERVICE)
        builder.setContentTitle(getString(R.string.app_name))
        val textRes: Int =
            if (established) R.string.notification_processing else R.string.notification_waiting
        builder.setContentText(getText(textRes))
        builder.setSmallIcon(R.drawable.ic_tune_vertical_variant_24dp)
        builder.addAction(createStopAction())
        builder.setContentIntent(contentIntent)
        return builder.build()
    }

    private fun notifySessionLoss() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        val contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = Notification.Builder(this, CHANNEL_ID_SESSION_LOSS)
        builder.setContentTitle(getString(R.string.session_control_loss_notification_title))
        builder.setContentText(getString(R.string.session_control_loss_notification))
        builder.setSmallIcon(R.drawable.ic_baseline_warning_24dp)
        builder.addAction(createRetryAction())
        builder.setAutoCancel(true)
        builder.setContentIntent(contentIntent)

        notificationManager.notify(NOTIFICATION_ID_SESSION_LOSS, builder.build())
    }
    private fun createStopAction(): Notification.Action {
        val stopIntent = createStopIntent(this)
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIcon = Icon.createWithResource(this, R.drawable.ic_close_24dp)
        val stopString = getString(R.string.action_stop)
        val actionBuilder = Notification.Action.Builder(stopIcon, stopString, stopPendingIntent)
        return actionBuilder.build()
    }

    private fun createRetryAction(): Notification.Action? {
        mediaProjectionStartIntent ?: return null
        val retryIntent = createStartIntent(this, mediaProjectionStartIntent!!)
        val retryPendingIntent = PendingIntent.getService(
            this,
            0,
            retryIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val retryIcon = Icon.createWithResource(this, R.drawable.ic_baseline_refresh_24dp)
        val retryString = getString(R.string.action_retry)
        val actionBuilder = Notification.Action.Builder(retryIcon, retryString, retryPendingIntent)
        return actionBuilder.build()
    }

    companion object {
        const val ACTION_PROCESSOR_MESSAGE = "me.timschneeberger.rootlessjamesdsp.PROCESSOR_MESSAGE"

        private const val TAG = "AudioProcessorService"

        private const val ACTION_START = "me.timschneeberger.rootlessjamesdsp.START"
        private const val ACTION_STOP = "me.timschneeberger.rootlessjamesdsp.STOP"
        private const val EXTRA_MEDIA_PROJECTION_DATA = "mediaProjectionData"
        private const val MSG_PROCESSOR_READY = 1

        fun start(context: Context, data: Intent?) {
            val intent = Intent(context, AudioProcessorService::class.java)
            intent.action = ACTION_START
            intent.putExtra(EXTRA_MEDIA_PROJECTION_DATA, data)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.startForegroundService(createStopIntent(context))
        }

        fun createStopIntent(ctx: Context): Intent {
            val intent = Intent(ctx, AudioProcessorService::class.java)
            intent.action = ACTION_STOP
            return intent
        }

        fun createStartIntent(ctx: Context, mediaProjectionData: Intent): Intent {
            val intent = Intent(ctx, AudioProcessorService::class.java)
            intent.action = ACTION_START
            intent.putExtra(EXTRA_MEDIA_PROJECTION_DATA, mediaProjectionData)
            return intent
        }

        private fun createAudioPlaybackCaptureConfig(mediaProjection: MediaProjection?): AudioPlaybackCaptureConfiguration {
            val confBuilder = AudioPlaybackCaptureConfiguration.Builder(
                mediaProjection!!
            )
            confBuilder.addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            confBuilder.addMatchingUsage(AudioAttributes.USAGE_GAME)
            confBuilder.addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            return confBuilder.build()
        }
    }
}
