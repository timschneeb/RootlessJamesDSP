package me.timschneeberger.hiddenapi_impl;

public class AudioSystem {

    private static final int REFLECTION_ERROR = -999;

    private static final String DEVICE_OUT_SPEAKER = "DEVICE_OUT_SPEAKER";
    private static final String DEVICE_OUT_EARPIECE = "DEVICE_OUT_EARPIECE";
    private static final String DEVICE_OUT_WIRED_HEADPHONE = "DEVICE_OUT_WIRED_HEADPHONE";


    Class<?> mAudioSystem;

    public AudioSystem() {

        try {
            mAudioSystem = Class.forName("android.media.AudioSystem");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    private int getConstantValue(String s) {

        try {
            return ((Integer) mAudioSystem.getDeclaredField(s).get(int.class)).intValue();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return REFLECTION_ERROR;

    }


    public int setAllowedCapturePolicy(int uid, int flags) {

        try {
            return (Integer) mAudioSystem.getMethod("setAllowedCapturePolicy", int.class, int.class).invoke(mAudioSystem, uid, flags);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return REFLECTION_ERROR;
    }

}