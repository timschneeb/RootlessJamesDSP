package me.timschneeberger.hiddenapi_impl;

import android.os.IBinder;
import android.permission.IPermissionManager;
import android.os.RemoteException;
import rikka.shizuku.ShizukuBinderWrapper;
import rikka.shizuku.SystemServiceHelper;

public class ShizukuSystemServerApi {

    private static final Singleton<IPermissionManager> PERMISSION_MANAGER = new Singleton<IPermissionManager>() {
        @Override
        protected IPermissionManager create() {
            IBinder service = SystemServiceHelper.getSystemService("permission");
            ShizukuBinderWrapper wrapper = new ShizukuBinderWrapper(service);
            return IPermissionManager.Stub.asInterface(wrapper);
        }
    };

    public static void PermissionManager_grantRuntimePermission(String packageName, String permissionName, int userId) throws RemoteException {
        PERMISSION_MANAGER.get().grantRuntimePermission(packageName, permissionName, userId);
    }
}