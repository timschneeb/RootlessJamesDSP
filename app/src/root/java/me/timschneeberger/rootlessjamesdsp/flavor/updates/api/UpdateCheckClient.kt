package me.timschneeberger.rootlessjamesdsp.flavor.updates.api

import android.content.Context
import com.pluto.plugins.network.PlutoInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.api.UserAgentInterceptor
import me.timschneeberger.rootlessjamesdsp.flavor.updates.model.UpdateCheckResponse
import me.timschneeberger.rootlessjamesdsp.utils.Result
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ApiExtensions
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ApiExtensions.save
import me.timschneeberger.rootlessjamesdsp.utils.storage.Cache
import okhttp3.OkHttpClient
import org.koin.core.component.KoinComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit

class UpdateCheckClient(val context: Context, callTimeout: Long = 10): KoinComponent {

    private val http = OkHttpClient
        .Builder()
        .callTimeout(callTimeout, TimeUnit.SECONDS)
        .addInterceptor(PlutoInterceptor())
        .addInterceptor(UserAgentInterceptor("RootlessJamesDSP v${BuildConfig.VERSION_NAME}"))
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://update.timschneeberger.me")
        .client(http)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service: UpdateCheckService = retrofit.create(UpdateCheckService::class.java)

    suspend fun checkUpdate(): Flow<Result<UpdateCheckResponse?>> {
        return flow<Result<UpdateCheckResponse?>> {
            try {
                val result =
                    service.checkUpdate(BuildConfig.FLAVOR, BuildConfig.VERSION_CODE.toString())
                        .execute()
                if(result.isSuccessful)
                    emit(Result.Success(result.body()))
                else
                    emit(Result.Error(IOException(result.message())))
            }
            catch (ex: IOException) {
                Timber.d(ex)
                emit(Result.Error(ex))
            }
        }
            .flowOn(Dispatchers.IO)
            .distinctUntilChanged()
    }

    suspend fun downloadUpdate(updateInfo: UpdateCheckResponse): Flow<ApiExtensions.DownloadState> {
        val file = Cache.getReleaseFile(context, "${updateInfo.versionCode.toString()}.apk")
        return service.downloadUpdate(BuildConfig.FLAVOR).save(file)
    }
}