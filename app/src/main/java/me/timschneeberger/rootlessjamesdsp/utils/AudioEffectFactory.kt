package me.timschneeberger.rootlessjamesdsp.utils

import android.media.audiofx.AudioEffect
import android.media.audiofx.AudioEffectHidden
import android.media.audiofx.DynamicsProcessing
import dev.rikka.tools.refine.Refine
import me.timschneeberger.rootlessjamesdsp.model.AudioSessionEntry
import timber.log.Timber
import java.util.*

class AudioEffectFactory {
    var sessionLossListener: ((sid: Int, data: AudioSessionEntry) -> Unit)? = null

    // TODO allow automatic fallback to other methods on session loss
    fun make(sid: Int, data: AudioSessionEntry): AudioEffect? {
        if(!isDeviceCompatible())
            return null

        for ((name, uuid) in supportedTypeUuids) {
            if(!AudioEffectHidden.isEffectTypeAvailable(uuid))
                continue

            try {
                return make(name, sid, data)
            }
            catch (ex: Exception) {
                Timber.e("Failed to attach $name effect to session $sid (data: $data; message: ${ex.message})")
            }
        }
        return null
    }

    fun make(type: MuteEffects, sid: Int, data: AudioSessionEntry): AudioEffect? {
        val name = type.name
        Timber.d("make: Creating $name effect instance")

        val muteEffect: AudioEffect
        try {
            muteEffect = when (type) {
                MuteEffects.DynamicsProcessing -> {
                    with(DynamicsProcessing(Int.MAX_VALUE, sid, null)) {
                        setInputGainAllChannelsTo(-200f)
                        this
                    }
                }
                MuteEffects.Volume -> {
                    with(AudioEffectHidden(
                        volumeHiddenTypeUuid,
                        AudioEffectHidden.EFFECT_TYPE_NULL,
                        Int.MAX_VALUE,
                        sid)
                    )
                    {
                        setParameter(VolumeParams.MUTE.ordinal, 1)
                        setParameter(VolumeParams.LEVEL.ordinal, -96)
                        Refine.unsafeCast(this)
                    }
                }
            }
        }
        catch (ex: IllegalArgumentException) {
            Timber.e("make: Effect not supported")
            Timber.i(ex)
            throw ex
        }
        catch (ex: UnsupportedOperationException) {
            Timber.e("make: Effect library not loaded")
            Timber.i(ex)
            throw ex
        }
        catch (ex: RuntimeException) {
            Timber.e("make: Runtime exception")
            Timber.i(ex)
            throw ex
        }

        muteEffect.enabled = true
        muteEffect.setEnableStatusListener { effect, enabled ->
            if (!enabled) {
                try {
                    if(effect is DynamicsProcessing)
                        effect.setInputGainAllChannelsTo(-200f)
                    effect.enabled = true
                    Timber.d("$name effect re-enabled (session $sid)")
                }
                catch(ex: Exception)
                {
                    /* Triggered if another app takes full control over effect */
                    Timber.w("Failed to re-enable $name effect (session $sid)")
                    Timber.w(ex)
                    sessionLossListener?.invoke(sid, data)
                }
            }
        }
        muteEffect.setControlStatusListener { effect, controlGranted ->
            if(!controlGranted)
            {
                sessionLossListener?.invoke(sid, data)
            }
            else {
                try {
                    if(effect is DynamicsProcessing)
                        effect.setInputGainAllChannelsTo(-200f)
                    effect.enabled = true
                    Timber.d("$name effect regained control (session $sid)")
                }
                catch(ex: Exception)
                {
                    Timber.w("Failed to regain control over $name effect (session $sid)")
                    Timber.w(ex)
                    sessionLossListener?.invoke(sid, data)
                }
            }
            Timber.d(
                "$name effect control %s",
                if (controlGranted) " returned" else "taken (session $sid)"
            )
        }

        return muteEffect
    }

    companion object {
        enum class MuteEffects {
            DynamicsProcessing,
            Volume
        }

        enum class VolumeParams {
            LEVEL,                 // type SLmillibel = typedef SLuint16 (set & get)
            MAXLEVEL,              // type SLmillibel = typedef SLuint16 (get)
            MUTE,                  // type SLboolean  = typedef SLuint32 (set & get)
            ENABLESTEREOPOSITION,  // type SLboolean  = typedef SLuint32 (set & get)
            STEREOPOSITION,        // type SLpermille = typedef SLuint16 (set & get)
        }

        private val volumeHiddenTypeUuid = UUID.fromString("09e8ede0-ddde-11db-b4f6-0002a5d5c51b")
        private val supportedTypeUuids = mapOf(
            MuteEffects.DynamicsProcessing to AudioEffect.EFFECT_TYPE_DYNAMICS_PROCESSING,
            MuteEffects.Volume to volumeHiddenTypeUuid,
        )

        fun isDeviceCompatible(): Boolean {
            var isCompatible = false
            supportedTypeUuids.forEach {
                if(AudioEffectHidden.isEffectTypeAvailable(it.value)) {
                    isCompatible = true
                    Timber.i("isDeviceCompatible: device supports ${it.key}")
                }
            }
            return isCompatible
        }
    }
}