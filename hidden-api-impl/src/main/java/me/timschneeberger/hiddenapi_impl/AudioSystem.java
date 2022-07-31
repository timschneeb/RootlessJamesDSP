package me.timschneeberger.hiddenapi_impl;

import android.annotation.SuppressLint;

public class AudioSystem {
    private static final int REFLECTION_ERROR = -999;
    private Class<?> mAudioSystem;

    @SuppressLint("PrivateApi")
    public AudioSystem() {
        try {
            mAudioSystem = Class.forName("android.media.AudioSystem");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    @SuppressWarnings("ConstantConditions")
    public int setAllowedCapturePolicy(int uid, int flags) {
        try {
            return (Integer) mAudioSystem.getMethod("setAllowedCapturePolicy", int.class, int.class).invoke(mAudioSystem, uid, flags);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }
}