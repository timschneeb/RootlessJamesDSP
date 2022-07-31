package android.media;

import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

public interface IAudioPolicyService extends IInterface {

    // Descriptor
    public static String DESCRIPTOR = "android.media.IAudioPolicyService";

    // Transaction codes
    // IMPORTANT: Keep in sync with https://cs.android.com/android/platform/superproject/+/master:out/soong/.intermediates/frameworks/av/media/libaudioclient/audiopolicy-aidl-cpp-source/gen/include/android/media/BnAudioPolicyService.h;l=50;drc=55acd2ec51840424e5cc2660747b4d2dc3755b80;bpv=0;bpt=1
    public static int TRANSACTION_setAllowedCapturePolicy = IBinder.FIRST_CALL_TRANSACTION + 40;

    // AudioFlags
    public static int AUDIO_FLAG_NONE                 = 0x0;
    public static int AUDIO_FLAG_NO_MEDIA_PROJECTION  = 0x400;
    public static int AUDIO_FLAG_NO_SYSTEM_CAPTURE    = 0x1000;

    void setAllowedCapturePolicy(int /* uid_t */ uid,
                                 int /* Bitmask of AudioFlags */ capturePolicy) throws RemoteException;
}
