package me.timschneeberger.hiddenapi_impl;

import android.media.IAudioPolicyService;
import android.os.Build;
import android.os.IBinder;
import android.permission.IPermissionManager;
import android.os.RemoteException;
import android.util.Log;

import com.android.internal.app.IAppOpsService;

import java.lang.reflect.Method;

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

    public static final Singleton<IAppOpsService> APP_OPS_SERVICE = new Singleton<IAppOpsService>() {
        @Override
        protected IAppOpsService create() {
            IBinder service = SystemServiceHelper.getSystemService("appops");
            ShizukuBinderWrapper wrapper = new ShizukuBinderWrapper(service);
            return IAppOpsService.Stub.asInterface(wrapper);
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

    public static void PermissionManager_grantRuntimePermission(String packageName, String permissionName, int userId) {
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                try {
                    PERMISSION_MANAGER.getOrThrow().grantRuntimePermission(packageName, permissionName, 0, userId);
                }catch (NoSuchMethodError e) {
                    PERMISSION_MANAGER.getOrThrow().grantRuntimePermission(packageName, permissionName, userId);
                }
            } else {
                PERMISSION_MANAGER.getOrThrow().grantRuntimePermission(packageName, permissionName, userId);
            }
        }
        catch(Exception ex) {
            Log.e("ShizukuSystemServerApi", "Failed to call app ops service");
        }
    }

    public static final String APP_OPS_MODE_ALLOW = "allow";
    public static final String APP_OPS_MODE_IGNORE = "ignore";
    public static final String APP_OPS_MODE_DENY = "deny";
    public static final String APP_OPS_MODE_DEFAULT = "default";
    public static final String APP_OPS_MODE_FOREGROUND = "foreground";

    public static final String APP_OPS_OP_PROJECT_MEDIA = "PROJECT_MEDIA";
    public static final String APP_OPS_OP_SYSTEM_ALERT_WINDOW = "SYSTEM_ALERT_WINDOW";

    public static boolean AppOpsService_setMode(String op, int packageUid, String packageName, String mode) throws RemoteException {
        int index = -1;

        try {
            Method method = Class.forName("android.app.AppOpsManager")
                    .getMethod("modeToName", int.class);

            for(int i = 0; i <= 10; i++) {
                if(mode.equals((String)method.invoke(null, i))) {
                    index = i;
                    break;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        int opIndex = -1;
        try {
            Method method = Class.forName("android.app.AppOpsManager")
                    .getMethod("strOpToOp", String.class);

            opIndex = (int) method.invoke(null, op);
        }
        catch(Exception ignored) {}
        try {
            Method method = Class.forName("android.app.AppOpsManager")
                    .getMethod("strDebugOpToOp", String.class);

            opIndex = (int) method.invoke(null, op);
        }
        catch(Exception ignored) {}

        if(index < 0 || opIndex < 0)
            return false;

        try {
            APP_OPS_SERVICE.getOrThrow().setMode(
                    opIndex,
                    packageUid,
                    packageName,
                    index
            );
        }
        catch(NullPointerException ex) {
            Log.e("ShizukuSystemServerApi", "Failed to call app ops service");
            return false;
        }

        return true;
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
            AUDIO_POLICY_SERVICE.getOrThrow().setAllowedCapturePolicy(uid, flags);
        }
        catch (Exception ex) {
            Log.d("ShizukuSystemServerApi", ex.toString());
        }
    }
}