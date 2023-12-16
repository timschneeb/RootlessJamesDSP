package android.permission;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

public interface IPermissionManager extends IInterface {

    void grantRuntimePermission(String packageName, String permissionName, int userId) throws RemoteException;

    void grantRuntimePermission(String packageName, String permissionName, int deviceId, int userId)
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
