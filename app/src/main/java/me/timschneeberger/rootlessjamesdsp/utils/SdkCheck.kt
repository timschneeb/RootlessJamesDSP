package me.timschneeberger.rootlessjamesdsp.utils

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

class SdkCheckElseBranch<T>(private val result: T?) {
    fun valueOrNull(): T? = result
    fun below(onFailure: () -> T): T = result ?: onFailure()
}

@ChecksSdkIntAtLeast(parameter = 0, lambda = 1)
inline fun <T> sdkAbove(sdk: Int, onSuccessful: () -> T): SdkCheckElseBranch<T> {
    (Build.VERSION.SDK_INT >= sdk).let {
        return SdkCheckElseBranch<T>(if(it) onSuccessful() else null)
    }
}

object SdkCheck {
    private val sdk: Int
        get() = Build.VERSION.SDK_INT

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    val isVanillaIceCream: Boolean
        get() = sdk >= Build.VERSION_CODES.VANILLA_ICE_CREAM

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    val isUpsideDownCake: Boolean
        get() = sdk >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    val isTiramisu: Boolean
        get() = sdk >= Build.VERSION_CODES.TIRAMISU

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
    val isQ: Boolean
        get() = sdk >= Build.VERSION_CODES.Q

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.P)
    val isPie: Boolean
        get() = sdk >= Build.VERSION_CODES.P

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    val isSnowCake: Boolean
        get() = sdk >= Build.VERSION_CODES.S
}