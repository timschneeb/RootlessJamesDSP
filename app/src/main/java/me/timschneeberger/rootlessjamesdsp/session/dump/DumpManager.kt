package me.timschneeberger.rootlessjamesdsp.session.dump

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.session.dump.data.ISessionInfoDump
import me.timschneeberger.rootlessjamesdsp.session.dump.data.ISessionPolicyInfoDump
import me.timschneeberger.rootlessjamesdsp.session.dump.provider.AudioFlingerServiceDumpProvider
import me.timschneeberger.rootlessjamesdsp.session.dump.provider.AudioPolicyServiceDumpProvider
import me.timschneeberger.rootlessjamesdsp.session.dump.provider.AudioServiceDumpProvider
import me.timschneeberger.rootlessjamesdsp.utils.preferences.Preferences
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class DumpManager constructor(val context: Context): KoinComponent {
    enum class Method (val value: Int) {
        AudioPolicyService(0),
        AudioService(1),
        AudioFlingerService(2); /* Only used for debugging */

        companion object {
            fun fromInt(value: Int) = values().first { it.value == value }
        }
    }

    private val preferences: Preferences.App by inject()
    private val preferencesListener: SharedPreferences.OnSharedPreferenceChangeListener

    private val dumpChangeCallbacks = mutableListOf<OnDumpMethodChangeListener>()
    private val availableDumpMethods = mapOf(
        Method.AudioPolicyService to AudioPolicyServiceDumpProvider(),
        Method.AudioService to AudioServiceDumpProvider(),
        Method.AudioFlingerService to AudioFlingerServiceDumpProvider()
        )

    private var activeDumpMethod: Method = Method.AudioPolicyService
        set(value) {
            field = value
            dumpChangeCallbacks.forEach { it.onDumpMethodChange(value) }
        }
    private var allowFallback: Boolean = true

    init {
        loadFromPreferences(context.getString(R.string.key_session_detection_method))
        preferencesListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            loadFromPreferences(key)
        }
        preferences.registerOnSharedPreferenceChangeListener(preferencesListener)
    }

    fun dumpSessions(): ISessionInfoDump? {
        val preferred = availableDumpMethods[activeDumpMethod]
        var dump: ISessionInfoDump? = null
        try {
            dump = preferred?.dump(context)
        } catch (ex: Exception) {
            Timber.e("Exception raised while dumping session info using method ${activeDumpMethod.name} (id ${activeDumpMethod})")
            Timber.e(ex)
        }

        if(!allowFallback || (dump != null && dump.sessions.isNotEmpty()))
        {
            return dump
        }

        availableDumpMethods.forEach {
                Timber.d("Falling back to method: ${it.key.name}")

                if(it.key != activeDumpMethod)
                {
                    dump = it.value.dump(context)
                }
                if(dump != null && dump!!.sessions.isNotEmpty())
                {
                    return dump
                }
        }

        Timber.e("Failed to find session info using any method")
        return null
    }

    fun dumpCaptureAllowlistLog(): ISessionPolicyInfoDump? {
        // Only AudioPolicyService contains this data
        var dump: ISessionPolicyInfoDump? = null
        try {
            dump = availableDumpMethods[Method.AudioPolicyService]?.dump(context) as? ISessionPolicyInfoDump
        }
        catch (ex: Exception) {
            Timber.e("Exception raised while dumping allowlist info using method ${Method.AudioPolicyService.name}")
            Timber.e(ex)
        }
        return dump
    }

    private fun loadFromPreferences(key: String?){
        when (key) {
            context.getString(R.string.key_session_detection_method) -> {
                val method = R.string.key_session_detection_method.let {
                    Method.fromInt(preferences.get<String>(it).toIntOrNull()
                        ?: preferences.getDefault<String>(it).toInt())
                }
                activeDumpMethod = method
                Timber.d("Session detection method set to ${method.name}")
            }
        }
    }

    fun collectDebugDumps(): String {
        var exceptionRaised = false
        val sb = StringBuilder("Application version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n")
        sb.append("Package: ${context.packageName}")
        sb.append("Commit: ${BuildConfig.COMMIT_SHA}; commits since release: ${BuildConfig.COMMIT_COUNT}; debug build: ${BuildConfig.DEBUG}\n")
        sb.append("Device model: ${Build.MANUFACTURER}; ${Build.PRODUCT}; ${Build.MODEL}; ${Build.DEVICE}\n")
        sb.append("Device fingerprint: ${Build.FINGERPRINT}\n")
        sb.append("Android version: ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE}; ${Build.VERSION.CODENAME})\n")
        sb.append("\n")
        availableDumpMethods.forEach { (method, provider) ->
            sb.append("==================> ${method.name}\n")
            try {
                sb.append("${provider.dumpString(context)}\n\n\n\n")
            }
            catch (ex: Exception) {
                Timber.e("Failed to collect debug dumps for ${method.name}")
                Timber.e(ex)

                sb.append("========X Exception raised while dumping service\n")
                sb.append(ex)
                sb.append("\n\n\n")
                exceptionRaised = true
            }
        }

        if(exceptionRaised)
        {
            sb.append("NOTE: One or more exceptions has been raised while collecting debug dumps\n")
        }

        return sb.toString()
    }

    fun registerOnDumpMethodChangeListener(changeListener: OnDumpMethodChangeListener) {
        dumpChangeCallbacks.add(changeListener)
    }

    fun unregisterOnDumpMethodChangeListener(changeListener: OnDumpMethodChangeListener) {
        dumpChangeCallbacks.remove(changeListener)
    }

    interface OnDumpMethodChangeListener {
        fun onDumpMethodChange(method: Method)
    }
}