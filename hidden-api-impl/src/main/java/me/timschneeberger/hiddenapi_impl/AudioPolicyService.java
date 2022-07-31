package me.timschneeberger.hiddenapi_impl;

import android.media.IAudioPolicyService;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

// Note: There's no Java stub for IAudioPolicyService aidl in AOSP because
//       it is exclusively used in native code. We need to create our own implementation.
class AudioPolicyService implements IAudioPolicyService {
    public static IAudioPolicyService asInterface(IBinder obj) {
        return new AudioPolicyService(obj);
    }

    private final IBinder binder;

    public AudioPolicyService(IBinder binder) {
        this.binder = binder;
    }

    @Override
    public IBinder asBinder() {
        return binder;
    }

    @Override
    public void setAllowedCapturePolicy(int uid, int capturePolicy) throws RemoteException {
        Parcel aidlData = Parcel.obtain();
        Parcel aidlReply = Parcel.obtain();

        aidlData.writeInterfaceToken(IAudioPolicyService.DESCRIPTOR);
        aidlData.writeInt(uid);
        aidlData.writeInt(capturePolicy);

        boolean status = binder.transact(TRANSACTION_setAllowedCapturePolicy, aidlData, aidlReply, 0);
        if(!status) {
            throw new RemoteException("TRANSACTION_setAllowedCapturePolicy failed");
        }

        aidlReply.readException();

        aidlData.recycle();
        aidlReply.recycle();
    }
}