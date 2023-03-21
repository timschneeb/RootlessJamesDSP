package me.timschneeberger.rootlessjamesdsp.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.content.getSystemService
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.utils.RoutingObserver.DeviceGroup.Companion.usesSingleProfile
import timber.log.Timber
import kotlin.properties.Delegates


class RoutingObserver(val context: Context) : MediaRouter.Callback() {
    private val callbacks = mutableListOf<RoutingChangedCallback>()
    private val router by lazy { MediaRouter.getInstance(context) }

    private var currentDevice by Delegates.observable<Device?>(null) { _, old, new ->
        if (old != new)
            callbacks.forEach { it.onRoutingDeviceChanged(new) }
    }

    private val AudioDeviceInfo.deviceGroup: DeviceGroup
        get() = DeviceGroup.from(type)

    private fun MediaRouter.RouteInfo.findOutputDevice(): Device? {
        return context.getSystemService<AudioManager>()!!
            .getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .filter(AudioDeviceInfo::isSink)
            .find { info ->
                info.deviceGroup.let {
                    when {
                        isDeviceSpeaker -> it == DeviceGroup.SPEAKER
                        isBluetooth -> it == DeviceGroup.BLUETOOTH
                        isUsb -> it == DeviceGroup.USB
                        isHeadphone -> it == DeviceGroup.ANALOG
                        isHdmi -> it == DeviceGroup.HDMI
                        else -> it == DeviceGroup.OTHER
                    }
                }
            }?.let { device ->
                Device(
                    device.productName.toString(),
                    device.address,
                    device.deviceGroup
                )
            }
    }

    private val MediaRouter.RouteInfo.isHdmi: Boolean
        @SuppressLint("DiscouragedApi")
        /*
            Breaking changes in Android 14:
            - 'HDMI' was replaced with 'External Device'
            - resource 'default_audio_route_name_hdmi' was renamed to 'default_audio_route_name_external_device'
            https://cs.android.com/android/_/android/platform/frameworks/base/+/376bae463a9e46437047efbd082c7ac56e1ced3f
         */
        get() = Resources.getSystem().getText(
            Resources.getSystem().getIdentifier(
                sdkAbove(34 /* FIXME Build.VERSION_CODES.UPSIDEDOWNCAKE */) {
                    "default_audio_route_name_external_device"
                }.below {
                    "default_audio_route_name_hdmi"
                },
                "string", "android"
            )
        ).let { hdmiName ->
            router.defaultRoute == this && hdmiName == name
        }


    private val MediaRouter.RouteInfo.isUsb: Boolean
        @SuppressLint("DiscouragedApi")
        get() = sdkAbove(Build.VERSION_CODES.P) {
            Resources.getSystem().getText(
                Resources.getSystem().getIdentifier(
                    "default_audio_route_name_usb", "string", "android"
                )
            )
        }.below {
            ""
        }.let { usbName ->
            router.defaultRoute == this && usbName == name
        }

    private val MediaRouter.RouteInfo.isHeadphone: Boolean
        @SuppressLint("DiscouragedApi")
        get() = Resources.getSystem().getText(
            Resources.getSystem().getIdentifier(
                "default_audio_route_name_headphones", "string", "android"
            )
        ).let { hpName ->
            router.defaultRoute == this && hpName == name
        }

    override fun onRouteSelected(router: MediaRouter, route: MediaRouter.RouteInfo, reason: Int) {
        currentDevice = route.findOutputDevice()
    }

    override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) {
        currentDevice = router.selectedRoute.findOutputDevice()
    }

    override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) {
        router.unselect(MediaRouter.UNSELECT_REASON_DISCONNECTED)
    }

    fun registerOnRoutingChangeListener(callback: RoutingChangedCallback) {
        if (callbacks.isEmpty()) {
            router.addCallback(
                MediaRouteSelector.Builder()
                    .addControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)
                    .build(),
                this,
                0
            )
        }
        callbacks.add(callback)
        currentDevice?.let(callback::onRoutingDeviceChanged)
    }

    fun unregisterOnRoutingChangeListener(callback: RoutingChangedCallback) {
        callbacks.remove(callback)
        if (callbacks.isEmpty())
            router.removeCallback(this)
    }

    interface RoutingChangedCallback {
        fun onRoutingDeviceChanged(device: Device?)
    }

    inner class Device(
        private val productName: String,
        private val address: String,
        val group: DeviceGroup,
    ) {
        init {
            // Log unexpected events
            if(!group.usesSingleProfile() && !hasProductName()) {
                Timber.e(IllegalStateException("Device has no valid product name: $this"))
            }
        }

        val id: String
            get() = if(group.usesSingleProfile())
                group.name.lowercase()
            else
                "${group.name.lowercase()}_${(if(group == DeviceGroup.BLUETOOTH) address else productName).lowercase().sanitize()}"
        val name: String
            get() = if(group.usesSingleProfile())
                context.getString(group.nameRes)
            else
                "${if(hasProductName()) productName else address} (${context.getString(group.nameRes)})"

        override fun toString() = "Device(id=$id, group=$group, name='$name', productName='$productName', address='$address')"
        private fun hasProductName() = productName != Build.MODEL && productName.isNotEmpty()
        private fun String.sanitize() = this.replace("[ \\;/:*?\"<>|&']".toRegex(),"_")
    }

    enum class DeviceGroup(@StringRes val nameRes: Int) {
        /* WARNING: Names of existing enum members are used as stable ids and must not be changed */
        ANALOG(R.string.group_wired_headphones),
        BLUETOOTH(R.string.group_bluetooth),
        HDMI(R.string.group_hdmi),
        SPEAKER(R.string.group_speaker),
        USB(R.string.group_usb),
        OTHER(R.string.group_unknown);

        companion object {
            fun DeviceGroup.usesSingleProfile() = this == ANALOG || this == SPEAKER

            fun from(type: Int) = when (type) {
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> SPEAKER
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_LINE_ANALOG,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> ANALOG
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, // FIXME what about BLE broadcast groups?
                AudioDeviceInfo.TYPE_BLE_HEADSET,
                AudioDeviceInfo.TYPE_BLE_SPEAKER -> BLUETOOTH
                AudioDeviceInfo.TYPE_USB_DEVICE,
                AudioDeviceInfo.TYPE_USB_ACCESSORY,
                AudioDeviceInfo.TYPE_USB_HEADSET -> USB
                AudioDeviceInfo.TYPE_HDMI_ARC,
                AudioDeviceInfo.TYPE_HDMI_EARC,
                AudioDeviceInfo.TYPE_HDMI -> HDMI
                else -> OTHER
            }
        }
    }
}