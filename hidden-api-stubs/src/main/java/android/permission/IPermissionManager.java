package android.permission;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

import androidx.annotation.RequiresApi;

public interface IPermissionManager extends IInterface {

    void grantRuntimePermission(String packageName, String permissionName, int userId) throws RemoteException;

    @RequiresApi(34)
    void grantRuntimePermission(String packageName, String permissionName, int deviceId, int userId)
            throws RemoteException;

    @RequiresApi(35)
    void grantRuntimePermission(String packageName, String permissionName, String deviceId, int userId)
            throws RemoteException;

    void revokeRuntimePermission(String packageName, String permissionName, int userId,
                                 String reason) throws RemoteException;

    void revokeRuntimePermission(String packageName, String permissionName, int deviceId, int userId, String reason)
            throws RemoteException;

    abstract class Stub extends Binder implements IPermissionManager {

        public static IPermissionManager asInterface(IBinder obj) {
            throw new UnsupportedOperationException();
        }
    }
}
