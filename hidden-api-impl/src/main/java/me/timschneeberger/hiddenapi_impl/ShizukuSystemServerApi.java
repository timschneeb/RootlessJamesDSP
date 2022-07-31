package me.timschneeberger.hiddenapi_impl;

import android.media.IAudioPolicyService;
import android.os.IBinder;
import android.permission.IPermissionManager;
import android.os.RemoteException;
import android.util.Log;

import rikka.shizuku.ShizukuBinderWrapper;
import rikka.shizuku.SystemServiceHelper;

public class ShizukuSystemServerApi {

    private static final Singleton<IPermissionManager> PERMISSION_MANAGER = new Singleton<IPermissionManager>() {
        @Override
        protected IPermissionManager create() {
            IBinder service = SystemServiceHelper.getSystemService("permissionmgr");
            ShizukuBinderWrapper wrapper = new ShizukuBinderWrapper(service);
            return IPermissionManager.Stub.asInterface(wrapper);
        }
    };

    public static final Singleton<IAudioPolicyService> AUDIO_POLICY_SERVICE = new Singleton<IAudioPolicyService>() {
        @Override
        protected IAudioPolicyService create() {
            IBinder service = SystemServiceHelper.getSystemService("media.audio_policy");
            ShizukuBinderWrapper wrapper = new ShizukuBinderWrapper(service);
            return new AudioPolicyService(wrapper);
        }
    };

    public static void PermissionManager_grantRuntimePermission(String packageName, String permissionName, int userId) throws RemoteException {
        PERMISSION_MANAGER.get().grantRuntimePermission(packageName, permissionName, userId);
    }

    public static void AudioPolicyService_setAllowedCapturePolicy(int uid, CapturePolicy capturePolicy) {
        int flags;
        switch (capturePolicy) {
            case ALLOW_CAPTURE_BY_ALL:
                flags = IAudioPolicyService.AUDIO_FLAG_NONE;
                break;
            case ALLOW_CAPTURE_BY_SYSTEM:
                flags = IAudioPolicyService.AUDIO_FLAG_NO_MEDIA_PROJECTION;
                break;
            case ALLOW_CAPTURE_BY_NONE:
                flags = IAudioPolicyService.AUDIO_FLAG_NO_MEDIA_PROJECTION |
                        IAudioPolicyService.AUDIO_FLAG_NO_SYSTEM_CAPTURE;
                break;
            default:
                throw new IllegalArgumentException();
        }

        try {
            Log.d("ShizukuSystemServerApi", "AudioPolicyService_setAllowedCapturePolicy flags=" + flags);
            AUDIO_POLICY_SERVICE.get().setAllowedCapturePolicy(uid, flags);
        }
        catch (RemoteException ex) {
            Log.d("ShizukuSystemServerApi", ex.toString());
        }
    }
}