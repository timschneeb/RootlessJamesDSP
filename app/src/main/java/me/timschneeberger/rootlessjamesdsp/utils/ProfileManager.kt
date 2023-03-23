package me.timschneeberger.rootlessjamesdsp.utils

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.model.preset.Preset
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.restoreDspSettings
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ensureIsDirectory
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ensureIsFile
import me.timschneeberger.rootlessjamesdsp.utils.preferences.Preferences
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException

class ProfileManager : RoutingObserver.RoutingChangedCallback, KoinComponent {
    private val context: Context by inject()
    private val prefs: Preferences.App by inject()
    private val routingObserver: RoutingObserver by inject()
    private val lock = Any()

    private var activeProfile: Profile?

    val allProfiles: Array<Profile>
        get() = getProfileDirectory(null)
            .ensureIsDirectory()
            ?.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { dir ->
                // Transform directory list to parsed profile list
                File(dir.absoluteFile, FILE_PROFILE)
                    .ensureIsFile()?.let(Profile::from)
            }
            ?.toMutableList()
            ?.apply {
                // Active profile may not yet be stored; add if missing
                activeProfile?.let {
                    if(!contains(it))
                        add(it)
                }
            }
            ?.sortedBy { it.name }
            ?.toTypedArray() ?: arrayOf()

    init {
        activeProfile = getProfileDirectory(null).ensureIsDirectory()?.let {
            File(it, FILE_PROFILE).ensureIsFile()?.let { file -> Profile.from(file) }
        }
        routingObserver.registerOnRoutingChangeListener(this)
    }

    protected fun finalize() {
        routingObserver.unregisterOnRoutingChangeListener(this)
    }

    override fun onRoutingDeviceChanged(device: RoutingObserver.Device?) {
        if(!prefs.get<Boolean>(R.string.key_device_profiles_enable))
            return

        Timber.d("onRoutingDeviceChanged: $device")
        rotate(device ?: return)
    }

    fun rotate(newDevice: RoutingObserver.Device) {
        synchronized(lock) {
            Timber.d("Rotating profile from '${activeProfile?.id}' to '${newDevice.id}'")
            val newProfile = Profile.from(newDevice)
            if(newProfile.id == activeProfile?.id) {
                Timber.w("Profile already loaded. No action taken.")
                return
            }

            storeClearCurrent()
            activeProfile = newProfile.also(::load)
        }
    }

    fun copy(source: Profile, destinations: Array<Profile>) {
        synchronized(lock) {
            if (source == activeProfile) {
                // If source is active, store current settings in destination
                destinations.forEach(::store)
            } else {
                // If source is inactive, load from storage
                val srcDir = getProfileDirectory(source.id)
                destinations.forEach { dest ->
                    // Copy within storage
                    Timber.d("Copying '${source.id}' to '${dest.id}'")

                    val destDir = getProfileDirectory(dest.id).apply { mkdirs() }
                    File(srcDir, FILE_PROFILE_PRESET).copyTo(File(destDir, FILE_PROFILE_PRESET), true)
                    dest.save(File(destDir, FILE_PROFILE))

                    if (dest == activeProfile) {
                        Timber.d("Destination is active")

                        // If destination is active, overwrite & load current settings
                        try {
                            Preset(
                                FILE_PROFILE_PRESET,
                                destDir
                            ).load()
                        } catch (ex: FileNotFoundException) {
                            Timber.e("Illegal state: preset does not exist")
                            Timber.i(ex)
                        }
                    }
                }
            }
        }
    }

    fun delete(profiles: Array<Profile>) {
        synchronized(lock) {
            profiles.forEach { profile ->
                Timber.d("Deleting profile '${profile.id}'")

                // Remove profile directory
                getProfileDirectory(profile.id).ensureIsDirectory()?.deleteRecursively()

                if (profile.id == activeProfile?.id) {
                    // If profile is active, we need to reset current config to defaults
                    context.restoreDspSettings()
                }
            }
        }
    }

    private fun load(profile: Profile) {
        getProfileDirectory(profile.id).apply { mkdirs() }.let { srcDir ->
            try {
                Preset(FILE_PROFILE_PRESET, srcDir).load()
            }
            catch (ex: FileNotFoundException) {
                Timber.w("load: preset does not exist yet")
                Timber.i(ex)
                context.restoreDspSettings()
            }

            profile.save(File(getProfileDirectory(null), FILE_PROFILE))
        }
    }

    private fun storeClearCurrent() {
        // Store current configuration
        val profile = activeProfile ?: routingObserver.currentDevice?.let { Profile.from(it) }
        profile ?: return

        store(profile)

        // Clear old current files
        getPrefsDirectory().listFiles()
            ?.filter { it.name.startsWith("dsp_") }
            ?.filter { it.extension == "xml" }
            ?.forEach { it.delete() }
    }

    private fun store(profile: Profile) {
        Timber.d("Storing current to profile: ${profile.id}")

        val targetDir = getProfileDirectory(profile.id).also { it.mkdirs() }
        Preset(FILE_PROFILE_PRESET, targetDir).save()
        profile.save(File(targetDir, FILE_PROFILE))
    }

    @Serializable
    data class Profile(val name: String, val id: String, val group: String) {
        fun save(json: File) = json.writeText(Json.encodeToString<Profile>(this))

        companion object {
            fun from(device: RoutingObserver.Device) = Profile(device.name, device.id, device.group.name)
            fun from(json: File): Profile? {
                return try {
                    Json.decodeFromString<Profile>(json.readText())
                } catch (ex: Exception) {
                    Timber.e(ex)
                    null
                }
            }
        }
    }

    private fun getProfileDirectory(id: String?): File {
        return File(context.applicationInfo.dataDir + "/files/profiles", (id ?: ""))
    }

    private fun getPrefsDirectory(): File {
        return File(context.applicationInfo.dataDir, "shared_prefs")
    }

    companion object {
        const val FILE_PROFILE = "profile.json"
        const val FILE_PROFILE_PRESET = "profile.tar"
    }

}