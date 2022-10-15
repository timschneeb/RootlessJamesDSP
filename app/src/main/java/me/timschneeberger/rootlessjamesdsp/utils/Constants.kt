package me.timschneeberger.rootlessjamesdsp.utils

import me.timschneeberger.rootlessjamesdsp.BuildConfig

object Constants {
    // App-relevant preference namespaces
    const val PREF_APP = "application"
    const val PREF_VAR = "variable"

    // DSP-relevant preference namespaces
    const val PREF_BASS = "dsp_bass"
    const val PREF_COMPRESSOR = "dsp_compressor"
    const val PREF_CONVOLVER = "dsp_convolver"
    const val PREF_CROSSFEED = "dsp_crossfeed"
    const val PREF_DDC = "dsp_ddc"
    const val PREF_EQ = "dsp_equalizer"
    const val PREF_GEQ = "dsp_graphiceq"
    const val PREF_LIVEPROG = "dsp_liveprog"
    const val PREF_OUTPUT = "dsp_output_control"
    const val PREF_REVERB = "dsp_reverb"
    const val PREF_STEREOWIDE = "dsp_stereowide"
    const val PREF_TUBE = "dsp_tube"

    // Default string values
    const val DEFAULT_CONVOLVER_ADVIMP = "-80;-100;23;12;17;28"
    const val DEFAULT_GEQ = "GraphicEQ: "
    const val DEFAULT_GEQ_INTERNAL = "GraphicEQ: 0.0 0.0;"
    const val DEFAULT_EQ = "25.0;40.0;63.0;100.0;160.0;250.0;400.0;630.0;1000.0;1600.0;2500.0;4000.0;6300.0;10000.0;16000.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0"

    // Intent actions
    const val ACTION_PREFERENCES_UPDATED = BuildConfig.APPLICATION_ID + ".action.preferences.UPDATED"
    const val ACTION_GRAPHIC_EQ_CHANGED = BuildConfig.APPLICATION_ID + ".action.preferences.graphiceq.CHANGED"
    const val ACTION_SESSION_CHANGED = BuildConfig.APPLICATION_ID + ".action.session.CHANGED"
    const val ACTION_SERVICE_STOPPED = BuildConfig.APPLICATION_ID + ".action.service.STOPPED"
    const val ACTION_SERVICE_RELOAD_LIVEPROG = BuildConfig.APPLICATION_ID + ".action.service.RELOAD_LIVEPROG"
    const val ACTION_SERVICE_HARD_REBOOT_CORE = BuildConfig.APPLICATION_ID + ".action.service.HARD_REBOOT_CORE"
    const val ACTION_SERVICE_SOFT_REBOOT_CORE = BuildConfig.APPLICATION_ID + ".action.service.SOFT_REBOOT_CORE"
    const val ACTION_PROCESSOR_MESSAGE = BuildConfig.APPLICATION_ID + ".action.service.PROCESSOR_MESSAGE"
    const val ACTION_DISCARD_AUTHORIZATION = BuildConfig.APPLICATION_ID + ".action.service.DISCARD_AUTHORIZATION"

    // Notifications
    const val CHANNEL_ID_SERVICE = "JamesDSP"
    const val CHANNEL_ID_SESSION_LOSS = "Session loss alert"
    const val CHANNEL_ID_PERMISSION_PROMPT = "Permission prompt"
    const val CHANNEL_ID_APP_INCOMPATIBILITY = "App incompatibility alert"

    const val NOTIFICATION_ID_SERVICE = 1
    const val NOTIFICATION_ID_SESSION_LOSS = 2
    const val NOTIFICATION_ID_PERMISSION_PROMPT = 3
    const val NOTIFICATION_ID_APP_INCOMPATIBILITY = 4
}