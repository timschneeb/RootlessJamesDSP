package me.timschneeberger.rootlessjamesdsp

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.*
import android.media.audiofx.DynamicsProcessing
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.core.app.ActivityCompat
import java.io.IOException


class AudioProcessorService : Service() {
    private val handler: Handler = StartupHandler(this)
    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var recorderThread: Thread? = null
    private lateinit var dynamicsProcessing: DynamicsProcessing
    private lateinit var volumeContentObserver: VolumeContentObserver

    private var isStreamMuted = false

    override fun onCreate() {
        super.onCreate()

        dynamicsProcessing = DynamicsProcessing(0)
        dynamicsProcessing.setInputGainAllChannelsTo(-200.0f)
        dynamicsProcessing.enabled = true
        dynamicsProcessing.setEnableStatusListener { effect, enabled ->
            if(!enabled) effect.enabled = true
        }
        dynamicsProcessing.setControlStatusListener { _, controlGranted ->
            Log.d("AudioProcessorService", "Dynamics processor control ${if(controlGranted) " returned" else "taken"}" )
        }

        volumeContentObserver = VolumeContentObserver(this)
        volumeContentObserver.setOnVolumeChangeListener {
            isStreamMuted = it <= 0
            Log.d(TAG, "Mute stream internally: $isStreamMuted")
        }

        val notification = createNotification(false)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.app_name),
            NotificationManager.IMPORTANCE_NONE
        )
        notificationManager.createNotificationChannel(channel)
        startForeground(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        )
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val action = intent.action
        if (ACTION_STOP == action) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (isRunning) {
            return START_NOT_STICKY
        }

        this.contentResolver.registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, volumeContentObserver);

        val data = intent.getParcelableExtra<Intent>(EXTRA_MEDIA_PROJECTION_DATA)
        mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mediaProjectionManager!!.getMediaProjection(Activity.RESULT_OK, data!!)

        if (mediaProjection != null) {
            startRecording()
        } else {
            Log.w(TAG, "Failed to capture audio")
            stopSelf()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun createNotification(established: Boolean): Notification {
        val notificationBuilder = Notification.Builder(this, CHANNEL_ID)
        notificationBuilder.setContentTitle(getString(R.string.app_name))
        val textRes: Int =
            if (established) R.string.notification_processing else R.string.notification_waiting
        notificationBuilder.setContentText(getText(textRes))
        notificationBuilder.setSmallIcon(R.drawable.ic_album_24dp)
        notificationBuilder.addAction(createStopAction())
        return notificationBuilder.build()
    }

    private fun createStopIntent(): Intent {
        val intent = Intent(this, AudioProcessorService::class.java)
        intent.action = ACTION_STOP
        return intent
    }

    private fun createStopAction(): Notification.Action {
        val stopIntent = createStopIntent()
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

    private fun startRecording() {
        val recorder = createAudioRecord(mediaProjection)
        if(recorder == null)
        {
            stopSelf()
            return;
        }

        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val track = AudioTrack.Builder().setAudioFormat(
            AudioFormat.Builder()
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .build()
        )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setAudioAttributes(AudioAttributes.Builder()
                .setAllowedCapturePolicy(AudioAttributes.ALLOW_CAPTURE_BY_NONE)
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setFlags(0)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build())
            .setBufferSizeInBytes(minBufferSize)
            .build()
        track.play()

        recorderThread = Thread {
            try {
                handler.sendEmptyMessage(MSG_PROCESSOR_READY)
                recorder.startRecording()
                val BUFFER_MS = 15 // do not buffer more than BUFFER_MS milliseconds
                val buf =
                    ByteArray(SAMPLE_RATE * CHANNELS * BUFFER_MS / 1000)
                while (true) {
                    val r = recorder.read(buf, 0, buf.size)

                    for (i in buf.indices) {
                        buf[i] = (buf[i] * 1.2).toInt().toByte()
                    }

                    if(isStreamMuted)
                    {
                        track.stop()
                    }
                    else
                    {
                        if(track.playState != AudioTrack.PLAYSTATE_PLAYING)
                        {
                            track.play()
                        }
                        track.write(buf, 0, buf.size)
                    }
                }
            } catch (e: IOException) {
                // ignore
            } finally {
                recorder.stop()
                track.stop()
                track.release()
                stopSelf()
            }
        }
        recorderThread!!.start()
    }

    private val isRunning: Boolean
        get() = recorderThread != null

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)

        this.dynamicsProcessing.enabled = false
        this.dynamicsProcessing.release()
        this.contentResolver.unregisterContentObserver(volumeContentObserver)

        if (recorderThread != null) {
            recorderThread!!.interrupt()
            recorderThread = null
        }
    }

    private val notificationManager: NotificationManager
        get() = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

    private class StartupHandler internal constructor(private val service: AudioProcessorService) :
        Handler(Looper.myLooper()!!) {
        override fun handleMessage(message: Message) {
            if (!service.isRunning) {
                // not running anymore, ignore events
                return
            }
            if (message.what == MSG_PROCESSOR_READY) {
                val notification = service.createNotification(true)
                service.notificationManager.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun createAudioRecord(mediaProjection: MediaProjection?): AudioRecord? {
        val builder = AudioRecord.Builder()
        builder.setAudioFormat(createAudioFormat())
        builder.setBufferSizeInBytes(1024 * 1024)
        builder.setAudioPlaybackCaptureConfig(createAudioPlaybackCaptureConfig(mediaProjection))

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Record audio permission missing. Can't record")
            return null;
        }
        return  builder.build()
    }

    companion object {
        private const val TAG = "AudioProcessorService"
        private const val CHANNEL_ID = "JamesDSP"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_START = "me.timschneeberger.rootlessjamesdsp.START"
        private const val ACTION_STOP = "me.timschneeberger.rootlessjamesdsp.STOP"
        private const val EXTRA_MEDIA_PROJECTION_DATA = "mediaProjectionData"
        private const val MSG_PROCESSOR_READY = 1
        private const val SAMPLE_RATE = 48000
        private const val CHANNELS = 2
        fun start(context: Context, data: Intent?) {
            val intent = Intent(context, AudioProcessorService::class.java)
            intent.action = ACTION_START
            intent.putExtra(EXTRA_MEDIA_PROJECTION_DATA, data)
            context.startForegroundService(intent)
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

        private fun createAudioFormat(): AudioFormat {
            val builder = AudioFormat.Builder()
            builder.setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            builder.setSampleRate(SAMPLE_RATE)
            builder.setChannelMask(if (CHANNELS == 2) AudioFormat.CHANNEL_IN_STEREO else AudioFormat.CHANNEL_IN_MONO)
            return builder.build()
        }
    }
}
