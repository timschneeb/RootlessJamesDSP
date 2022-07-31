package android.os;

import android.annotation.NonNull;

public class Parcel {

    /**
     * Retrieve a new Parcel object from the pool for use with a specific binder.
     *
     * Associate this parcel with a binder object. This marks the parcel as being prepared for a
     * transaction on this specific binder object. Based on this, the format of the wire binder
     * protocol may change. For future compatibility, it is recommended to use this for all
     * Parcels.
     */
    @NonNull
    public static Parcel obtain(@NonNull IBinder binder) {
        throw new RuntimeException("STUB");
    }

    /**
     * Store or read an IBinder interface token in the parcel at the current
     * dataPosition. This is used to validate that the marshalled
     * transaction is intended for the target interface. This is typically written
     * at the beginning of transactions as a header.
     */
    public final void writeInterfaceToken(@NonNull String interfaceName) {
        throw new RuntimeException("STUB");
    }

    /**
     * Write an integer value into the parcel at the current dataPosition(),
     * growing dataCapacity() if needed.
     */
    public final void writeInt(int val) {
        throw new RuntimeException("STUB");
    }
}
