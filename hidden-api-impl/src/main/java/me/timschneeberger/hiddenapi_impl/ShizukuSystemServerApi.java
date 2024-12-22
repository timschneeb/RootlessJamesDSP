package me.timschneeberger.hiddenapi_impl;

import android.media.IAudioPolicyService;
import android.os.Build;
import android.os.IBinder;
import android.permission.IPermissionManager;
import android.os.RemoteException;
import android.util.Log;

import com.android.internal.app.IAppOpsService;

import java.io.OutputStream;
import java.lang.reflect.Method;

import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuBinderWrapper;
import rikka.shizuku.ShizukuRemoteProcess;
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
            if (Build.VERSION.SDK_INT >= 35) {
                PERMISSION_MANAGER.getOrThrow().grantRuntimePermission(packageName, permissionName, "default:0", userId);
            }
            else if (Build.VERSION.SDK_INT == 34) {
                try {
                    PERMISSION_MANAGER.getOrThrow().grantRuntimePermission(packageName, permissionName, 0, userId);
                    return;
                }
                catch (NoSuchMethodError ignored) {}
                // Retry with old method
                PERMISSION_MANAGER.getOrThrow().grantRuntimePermission(packageName, permissionName, userId);
            } else {
                PERMISSION_MANAGER.getOrThrow().grantRuntimePermission(packageName, permissionName, userId);
            }
        }
        catch(Exception ex) {
            Log.e("ShizukuSystemServerApi", "Failed to call app ops service");
            exec("pm grant " + packageName + " " + permissionName);
        }
    }

    public static synchronized void exec(String cmd) {
        try {
            Method newProcess = Shizuku.class.getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
            newProcess.setAccessible(true);
            ShizukuRemoteProcess process = (ShizukuRemoteProcess) newProcess.invoke(null, new String[]{"sh"}, null, null);
            assert process != null;
            OutputStream outputStream = process.getOutputStream();
            outputStream.write((cmd + "\nexit\n").getBytes());
            outputStream.flush();
            outputStream.close();
            process.waitFor();
        } catch (Exception e) {
            Log.e("ShizukuSystemServerApi", "Failed to call cmd via exec");
        }
    }


    public static final String APP_OPS_MODE_ALLOW = "allow";
    public static final String APP_OPS_MODE_IGNORE = "ignore";
    public static final String APP_OPS_MODE_DENY = "deny";
    public static final String APP_OPS_MODE_DEFAULT = "default";
    public static final String APP_OPS_MODE_FOREGROUND = "foreground";

    public static final String APP_OPS_OP_PROJECT_MEDIA = "PROJECT_MEDIA";
    public static final String APP_OPS_OP_SYSTEM_ALERT_WINDOW = "SYSTEM_ALERT_WINDOW";

    public static void AppOpsService_setMode(String op, int packageUid, String packageName, String mode) throws RemoteException {
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
        } catch (Exception e) {
            Log.e("ShizukuSystemServerApi", "Failed to get op index via strOpToOp");

            try {
                Method methodDbg = Class.forName("android.app.AppOpsManager")
                        .getMethod("strDebugOpToOp", String.class);

                opIndex = (int) methodDbg.invoke(null, op);
            }
            catch (Exception ex) {
                Log.e("ShizukuSystemServerApi", "Failed to get op index via strDebugOpToOp");
                throw new RuntimeException(e);
            }
        }

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
            throw new RuntimeException(ex);
        }
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