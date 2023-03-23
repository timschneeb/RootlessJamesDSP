package me.timschneeberger.rootlessjamesdsp.fragment

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import androidx.recyclerview.widget.RecyclerView
import me.timschneeberger.rootlessjamesdsp.MainApplication
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.adapter.RoundedRipplePreferenceGroupAdapter
import me.timschneeberger.rootlessjamesdsp.preference.DropDownPreference
import me.timschneeberger.rootlessjamesdsp.utils.RoutingObserver
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.showChoiceAlert
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.showMultipleChoiceAlert
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.toast
import org.koin.android.ext.android.inject

class DeviceProfilesCardFragment : PreferenceFragmentCompat(), RoutingObserver.RoutingChangedCallback {
    private val routingObserver: RoutingObserver by inject()
    private val profileActive get() = findPreference<DropDownPreference>(getString(R.string.key_profile_active))
    private val app
        get() = requireActivity().application as MainApplication

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.device_profile_preferences)
        profileActive?.onMenuItemClick = {
            when(it) {
                0 -> openCopyDialog()
                1 -> openDeleteDialog()
            }
        }

        routingObserver.registerOnRoutingChangeListener(this)
    }

    override fun onDestroy() {
        routingObserver.unregisterOnRoutingChangeListener(this)
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        onRoutingDeviceChanged(routingObserver.currentDevice)
        super.onConfigurationChanged(newConfig)
    }

    override fun onRoutingDeviceChanged(device: RoutingObserver.Device?) {
        profileActive?.summary = device?.name ?: getString(R.string.unknown_error)
        profileActive?.icon = context?.let { ctx ->
            ContextCompat.getDrawable(ctx, when(device?.group) {
                RoutingObserver.DeviceGroup.ANALOG -> R.drawable.ic_twotone_headphones_24dp
                RoutingObserver.DeviceGroup.BLUETOOTH -> R.drawable.ic_twotone_bluetooth_24dp
                RoutingObserver.DeviceGroup.HDMI -> R.drawable.ic_twotone_settings_input_hdmi_24dp
                RoutingObserver.DeviceGroup.SPEAKER -> R.drawable.ic_twotone_speaker_24dp
                RoutingObserver.DeviceGroup.USB -> R.drawable.ic_twotone_usb_24dp
                else -> R.drawable.ic_twotone_device_unknown_24dp
            })
        }
    }

    override fun onCreateRecyclerView(
        inflater: LayoutInflater,
        parent: ViewGroup,
        savedInstanceState: Bundle?,
    ): RecyclerView {
        return super.onCreateRecyclerView(inflater, parent, savedInstanceState).apply {
            itemAnimator = null // Fix to prevent RecyclerView crash if group is toggled rapidly
            isNestedScrollingEnabled = false
        }
    }

    override fun onCreateAdapter(preferenceScreen: PreferenceScreen): RecyclerView.Adapter<*> {
        return RoundedRipplePreferenceGroupAdapter(preferenceScreen)
    }

    private fun openCopyDialog() {
        val profiles = app.profileManager.allProfiles
        if(profiles.size <= 1) {
            context?.toast(R.string.device_profile_manage_copy_select_no_target)
            return
        }

        context?.showChoiceAlert(
            profiles.map { it.name }.toTypedArray(),
            R.string.device_profile_manage_copy_select,
            R.string.copy
        ) { srcIndex ->
            val srcProfile = profiles[srcIndex]
            val remaining = profiles.filter { it.id != srcProfile.id }

            context?.showMultipleChoiceAlert(
                remaining.map { it.name }.toTypedArray(),
                remaining.toTypedArray(),
                R.string.device_profile_manage_paste_select,
                R.string.paste
            ) { destProfiles ->
                 app.profileManager.copy(srcProfile, destProfiles.toTypedArray())
            }
        }
    }

    private fun openDeleteDialog() {
        app.profileManager.allProfiles.let { profiles ->
            context?.showMultipleChoiceAlert(
                profiles.map { it.name }.toTypedArray(),
                profiles,
                R.string.device_profile_manage_delete,
                R.string.delete
            ) {
                app.profileManager.delete(it.toTypedArray())
            }
        }
    }

    companion object {
        fun newInstance(): DeviceProfilesCardFragment {
            return DeviceProfilesCardFragment()
        }
    }
}