package me.timschneeberger.rootlessjamesdsp.utils.extensions

import android.media.audiofx.AudioEffect
import android.media.audiofx.AudioEffectHidden
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import kotlin.math.ceil


object AudioEffectExtensions {
    fun AudioEffectHidden?.setParameterCharArray(parameter: Int, value: String): Int {
        this ?: return AudioEffect.ERROR_NO_INIT

        var result = value.toByteArray(Charset.forName("US-ASCII"))
        if (result.size < 256) {
            val zeroPad = 256 - result.size
            result += ByteArray(zeroPad)
        }
        return safeAccess { this.setParameter(parameter, result) }
    }

    fun AudioEffectHidden?.setParameterCharBuffer(parameterSend: Int, parameterCommit: Int, string: String): Int{
        this ?: return AudioEffect.ERROR_NO_INIT

        val partitionCount = ceil(string.length.toDouble() / MAX_CHAR_PARTITION_SIZE).toInt()

        // Send buffer info for module to allocate memory
        setParameterIntArray(PARAM_CHAR_BUFFER_INFO, intArrayOf(partitionCount, MAX_CHAR_PARTITION_SIZE))

        for (i in 0 until partitionCount)
            setParameterCharArray(
                parameterSend,
                string.substring(
                    MAX_CHAR_PARTITION_SIZE * i,
                    (MAX_CHAR_PARTITION_SIZE * i + MAX_CHAR_PARTITION_SIZE).coerceAtMost(string.length)
                )
            )

        // Commit buffer
        return safeAccess { this.setParameter(parameterCommit, 1.toShort()) }
    }

    fun AudioEffectHidden?.setParameterImpulseResponseBuffer(
        parameterSend: Int,
        parameterCommit: Int,
        impulseResponse: FloatArray,
        channels: Int
    ) : Int {
        this ?: return AudioEffect.ERROR_NO_INIT

        val frames = impulseResponse.size
        val sendArray = FloatArray(MAX_IR_PARTITION_SIZE)
        val partitionCount = ceil(frames.toDouble() / MAX_IR_PARTITION_SIZE).toInt()

        // Send buffer info for module to allocate memory
        setParameterIntArray(PARAM_FLOAT_BUFFER_INFO, intArrayOf(frames, channels, 0, partitionCount))

        // Fill final array with zero padding
        val finalArray = FloatArray(partitionCount * MAX_IR_PARTITION_SIZE)

        System.arraycopy(impulseResponse, 0, finalArray, 0, frames)
        for (i in 0 until partitionCount) {
            System.arraycopy(finalArray, MAX_IR_PARTITION_SIZE * i, sendArray, 0, MAX_IR_PARTITION_SIZE)
            setParameterFloatArray(parameterSend, sendArray)
        }

        // Commit buffer
        return safeAccess { this.setParameter(parameterCommit, 1.toShort()) }
    }

    fun AudioEffectHidden?.setParameterFloatArray(parameter: Int, value: FloatArray): Int {
        this ?: return AudioEffect.ERROR_NO_INIT

        val result = ByteArray(value.size * 4)
        val byteDataBuffer = ByteBuffer.wrap(result)
        byteDataBuffer.order(ByteOrder.nativeOrder())
        for (i in value.indices)
            byteDataBuffer.putFloat(value[i])

        return safeAccess { this.setParameter(parameter, result) }
    }

    fun AudioEffectHidden?.setParameterIntArray(parameter: Int, value: IntArray): Int {
        this ?: return AudioEffect.ERROR_NO_INIT

        val result = ByteArray(value.size * 4)
        val byteDataBuffer = ByteBuffer.wrap(result)
        byteDataBuffer.order(ByteOrder.nativeOrder())
        for (i in value.indices)
            byteDataBuffer.putInt(value[i])

        return safeAccess { this.setParameter(parameter, result) }
    }

    fun AudioEffectHidden?.setParameter(param: ByteArray, value: ByteArray): Int {
        this ?: return AudioEffect.ERROR_NO_INIT
        return safeAccess { this.setParameter(param, value) }
    }

    fun AudioEffectHidden?.setParameter(param: Int, value: Int): Int {
        this ?: return AudioEffect.ERROR_NO_INIT
        return safeAccess { this.setParameter(param, value) }
    }

    fun AudioEffectHidden?.setParameter(param: Int, value: ByteArray): Int {
        this ?: return AudioEffect.ERROR_NO_INIT
        return safeAccess { this.setParameter(param, value) }
    }

    fun AudioEffectHidden?.setParameter(param: Int, value: Short): Int {
        this ?: return AudioEffect.ERROR_NO_INIT
        return safeAccess { this.setParameter(param, value) }
    }

    fun AudioEffectHidden?.getParameterInt(parameter: Int): Int? {
        this ?: return null

        val bytes = ByteArray(4)
        val ret = safeAccess { this.getParameter(parameter, bytes) }
        if(ret < 0) {
            Timber.e("getParameterInt: failed to get parameter $parameter; error code: $ret")
            return null
        }
        return ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder()).int
    }

    private fun safeAccess(onTry: () -> Int): Int {
        return try { onTry.invoke() } catch (ex: IllegalStateException) {
            Timber.e("AudioEffect is in invalid state")
            Timber.d(ex)
            AudioEffect.ERROR_NO_INIT
        }
    }

    private const val MAX_CHAR_PARTITION_SIZE = 256
    private const val MAX_IR_PARTITION_SIZE = 4096
    private const val PARAM_CHAR_BUFFER_INFO = 8888
    private const val PARAM_FLOAT_BUFFER_INFO = 9999
}