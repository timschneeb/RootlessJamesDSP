package me.timschneeberger.hiddenapi_impl;

public enum CapturePolicy {
    /**
     * Indicates that the audio may be captured by any app.
     *
     * For privacy, the following usages can not be recorded: VOICE_COMMUNICATION*,
     * USAGE_NOTIFICATION*, USAGE_ASSISTANCE* and USAGE_ASSISTANT.
     *
     * On <a href="/reference/android/os/Build.VERSION_CODES#Q">Build.VERSION_CODES</a>,
     * this means only USAGE_MEDIA and USAGE_GAME may be captured.
     *
     * See <a href="/reference/android/media/AudioAttributes.html#ALLOW_CAPTURE_BY_ALL">
     * ALLOW_CAPTURE_BY_ALL</a>.
     */
    ALLOW_CAPTURE_BY_ALL,
    /**
     * Indicates that the audio may only be captured by system apps.
     *
     * System apps can capture for many purposes like accessibility, user guidance...
     * but have strong restriction. See
     * <a href="/reference/android/media/AudioAttributes.html#ALLOW_CAPTURE_BY_SYSTEM">
     * ALLOW_CAPTURE_BY_SYSTEM</a>
     * for what the system apps can do with the capture audio.
     */
    ALLOW_CAPTURE_BY_SYSTEM,
    /**
     * Indicates that the audio may not be recorded by any app, even if it is a system app.
     *
     * It is encouraged to use {@link #ALLOW_CAPTURE_BY_SYSTEM} instead of this value as system apps
     * provide significant and useful features for the user (eg. accessibility).
     * See <a href="/reference/android/media/AudioAttributes.html#ALLOW_CAPTURE_BY_NONE">
     * ALLOW_CAPTURE_BY_NONE</a>.
     */
    ALLOW_CAPTURE_BY_NONE,
}
