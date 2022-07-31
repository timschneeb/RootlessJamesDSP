package android.os;

import android.annotation.NonNull;
import android.annotation.Nullable;

import java.io.FileDescriptor;

public interface IBinder {

    public static int FIRST_CALL_TRANSACTION = 0x00000001;
    public static int LAST_CALL_TRANSACTION = 0x00ffffff;

    boolean transact(int code, @NonNull Parcel data, @Nullable Parcel reply, int flags);

    @Nullable
    String getInterfaceDescriptor();

    boolean pingBinder();

    boolean isBinderAlive();

    @Nullable
    IInterface queryLocalInterface(@NonNull String descriptor);

    void dump(@NonNull FileDescriptor fd, @Nullable String[] args);

    void dumpAsync(@NonNull FileDescriptor fd, @Nullable String[] args);

    void linkToDeath(@NonNull DeathRecipient recipient, int flags);

    boolean unlinkToDeath(@NonNull DeathRecipient recipient, int flags);

    interface DeathRecipient {
        void binderDied();
    }
}
