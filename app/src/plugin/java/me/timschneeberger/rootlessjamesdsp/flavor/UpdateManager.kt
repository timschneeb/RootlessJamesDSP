@file:Suppress("UNUSED_PARAMETER", "RedundantSuspendModifier", "RedundantNullableReturnType")

package me.timschneeberger.rootlessjamesdsp.flavor

import android.content.Context
import kotlinx.coroutines.flow.Flow
import me.timschneeberger.rootlessjamesdsp.utils.Result
import java.lang.IllegalStateException

class UpdateManager(val context: Context) {
    fun getUpdateVersionInfo(): Pair<String, Int>? = throw IllegalStateException()
    suspend fun isUpdateAvailable(): Flow<Result<Boolean>> = throw IllegalStateException()
    fun installUpdate(context: Context): Nothing = throw IllegalStateException()
}
