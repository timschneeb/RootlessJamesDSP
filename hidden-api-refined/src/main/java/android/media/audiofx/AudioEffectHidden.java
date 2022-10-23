package android.media.audiofx;

import java.util.UUID;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(AudioEffect.class)
public class AudioEffectHidden {
    /**
     * Class constructor.
     *
     * @param type type of effect engine created. See EFFECT_TYPE_ENV_REVERB,
     *            EFFECT_TYPE_EQUALIZER ... Types corresponding to
     *            built-in effects are defined by AudioEffect class. Other types
     *            can be specified provided they correspond an existing OpenSL
     *            ES interface ID and the corresponsing effect is available on
     *            the platform. If an unspecified effect type is requested, the
     *            constructor with throw the IllegalArgumentException. This
     *            parameter can be set to EFFECT_TYPE_NULL in which
     *            case only the uuid will be used to select the effect.
     * @param uuid unique identifier of a particular effect implementation.
     *            Must be specified if the caller wants to use a particular
     *            implementation of an effect type. This parameter can be set to
     *            EFFECT_TYPE_NULL in which case only the type will
     *            be used to select the effect.
     * @param priority the priority level requested by the application for
     *            controlling the effect engine. As the same effect engine can
     *            be shared by several applications, this parameter indicates
     *            how much the requesting application needs control of effect
     *            parameters. The normal priority is 0, above normal is a
     *            positive number, below normal a negative number.
     * @param audioSession system wide unique audio session identifier.
     *            The effect will be attached to the MediaPlayer or AudioTrack in
     *            the same audio session.
     *
     * @throws java.lang.IllegalArgumentException
     * @throws java.lang.UnsupportedOperationException
     * @throws java.lang.RuntimeException
     */
    public AudioEffectHidden(UUID type, UUID uuid, int priority, int audioSession)
            throws IllegalArgumentException, UnsupportedOperationException,
            RuntimeException {
        throw new RuntimeException("Stub!");
    }

    /**
     * Null effect UUID. See {@link AudioEffectHidden(UUID, UUID, int, int)} for use.
     */
    public static final UUID EFFECT_TYPE_NULL = UUID
            .fromString("ec7178ec-e5e1-4432-a3f4-4657e6795210");

    /**
     * Checks if the device implements the specified effect type.
     * @param type the requested effect type.
     * @return true if the device implements the specified effect type, false otherwise.
     */
    public static boolean isEffectTypeAvailable(UUID type) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Set effect parameter. The setParameter method is provided in several
     * forms addressing most common parameter formats. This form is the most
     * generic one where the parameter and its value are both specified as an
     * array of bytes. The parameter and value type and length are therefore
     * totally free. For standard effect defined by OpenSL ES, the parameter
     * format and values must match the definitions in the corresponding OpenSL
     * ES interface.
     *
     * @param param the identifier of the parameter to set
     * @param value the new value for the specified parameter
     * @return SUCCESS in case of success, ERROR_BAD_VALUE,
     *         ERROR_NO_MEMORY, ERROR_INVALID_OPERATION or
     *         ERROR_DEAD_OBJECT in case of failure
     * @throws IllegalStateException
     */
    public int setParameter(byte[] param, byte[] value)
            throws IllegalStateException {
        throw new RuntimeException("Stub!");
    }

    /**
     * Set effect parameter. The parameter and its value are integers.
     *
     * @see #setParameter(byte[], byte[])
     */
    public int setParameter(int param, int value) throws IllegalStateException {
        throw new RuntimeException("Stub!");
    }

    /**
     * Set effect parameter. The parameter is an integer and the value is an
     * array of bytes.
     *
     * @see #setParameter(byte[], byte[])
     */
    public int setParameter(int param, byte[] value)
            throws IllegalStateException {
        throw new RuntimeException("Stub!");
    }

    /**
     * Set effect parameter. The parameter is an integer and the value is a
     * short integer.
     *
     * @see #setParameter(byte[], byte[])
     */
    public int setParameter(int param, short value)
            throws IllegalStateException {
        throw new RuntimeException("Stub!");
    }


    /**
     * Get effect parameter. The getParameter method is provided in several
     * forms addressing most common parameter formats. This form is the most
     * generic one where the parameter and its value are both specified as an
     * array of bytes. The parameter and value type and length are therefore
     * totally free.
     *
     * @param param the identifier of the parameter to set
     * @param value the new value for the specified parameter
     * @return the number of meaningful bytes in value array in case of success or
     * ERROR_BAD_VALUE, ERROR_NO_MEMORY, ERROR_INVALID_OPERATION
     * or ERROR_DEAD_OBJECT in case of failure.
     * @throws IllegalStateException
     */
    public int getParameter(byte[] param, byte[] value)
            throws IllegalStateException {
        throw new RuntimeException("Stub!");
    }

    /**
     * Get effect parameter. The parameter is an integer and the value is an
     * array of bytes.
     *
     * @see #getParameter(byte[], byte[])
     */
    public int getParameter(int param, byte[] value)
            throws IllegalStateException {
        throw new RuntimeException("Stub!");
    }

    /**
     * Enable or disable the effect.
     * Creating an audio effect does not automatically apply this effect on the audio source. It
     * creates the resources necessary to process this effect but the audio signal is still bypassed
     * through the effect engine. Calling this method will make that the effect is actually applied
     * or not to the audio content being played in the corresponding audio session.
     *
     * @param enabled the requested enable state
     * @return SUCCESS in case of success, ERROR_INVALID_OPERATION
     *         or ERROR_DEAD_OBJECT in case of failure.
     * @throws IllegalStateException
     */
    public int setEnabled(boolean enabled) throws IllegalStateException {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns effect enabled state
     *
     * @return true if the effect is enabled, false otherwise.
     * @throws IllegalStateException
     */
    public boolean getEnabled() throws IllegalStateException {
        throw new RuntimeException("Stub!");
    }

    /**
     * Get the effect descriptor.
     *
     * @see android.media.audiofx.AudioEffect.Descriptor
     * @throws IllegalStateException
     */
    public AudioEffect.Descriptor getDescriptor() throws IllegalStateException {
        throw new RuntimeException("Stub!");
    }

    /**
     * Releases the native AudioEffect resources. It is a good practice to
     * release the effect engine when not in use as control can be returned to
     * other applications or the native resources released.
     */
    public void release() {
        throw new RuntimeException("Stub!");
    }
}
