package android.media.session;

import android.os.Parcel;
import android.os.Parcelable;

import dev.rikka.tools.refine.RefineAs;

/**
 * Allows interaction with media controllers, volume keys, media buttons, and
 * transport controls.
 * <p>
 * A MediaSession should be created when an app wants to publish media playback
 * information or handle media keys. In general an app only needs one session
 * for all playback, though multiple sessions can be created to provide finer
 * grain controls of media.
 */
@RefineAs(MediaSession.class)
public final class MediaSessionHidden {

    /**
     * Represents an ongoing session. This may be passed to apps by the session
     * owner to allow them to create a {@link MediaController} to communicate with
     * the session.
     */
    @RefineAs(MediaSession.Token.class)
    public static final class TokenHidden implements Parcelable {

        @Override
        public int describeContents() {
            throw new RuntimeException("Stub!");
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Gets the UID of the application that created the media session.
         */
        public int getUid() {
            throw new RuntimeException("Stub!");
        }


        public static final Parcelable.Creator<TokenHidden> CREATOR =
                new Parcelable.Creator<TokenHidden>() {
                    @Override
                    public TokenHidden createFromParcel(Parcel in) {
                        throw new RuntimeException("Stub!");

                    }

                    @Override
                    public TokenHidden[] newArray(int size) {
                        throw new RuntimeException("Stub!");
                    }
                };
    }
}
