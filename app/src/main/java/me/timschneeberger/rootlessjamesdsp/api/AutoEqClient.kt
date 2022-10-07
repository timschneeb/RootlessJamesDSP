package me.timschneeberger.rootlessjamesdsp.api

import android.content.Context
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.model.api.AeqSearchResult
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit

class AutoEqClient(val context: Context) {

    private val http = OkHttpClient
        .Builder()
        .callTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(UserAgentInterceptor("RootlessJamesDSP v${BuildConfig.VERSION_NAME}"))
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(API_URL)
        .client(http)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service: AutoEqService = retrofit.create(AutoEqService::class.java)

    fun queryProfiles(query: String, onResponse: ((Array<AeqSearchResult>, Boolean /* isPartialResult */) -> Unit), onFailure: ((String) -> Unit)?) {
        val call = service.queryProfiles(query)
        call.enqueue(object : Callback<Array<AeqSearchResult>> {
            override fun onResponse(call: Call<Array<AeqSearchResult>>, response: Response<Array<AeqSearchResult>>) {
                if (response.code() == 200) {
                    onResponse.invoke(response.body()!!, response.headers().get(HEADER_PARTIAL_RESULT) == "1")
                }
                else {
                    onFailure?.invoke(context.getString(R.string.geq_api_network_error,
                        context.getString(R.string.geq_api_network_error_details_code, response.code())
                    ))
                    Timber.e("queryProfiles: Server responded with ${response.code()} (${response.errorBody().toString()})")
                }
            }

            override fun onFailure(call: Call<Array<AeqSearchResult>>, t: Throwable) {
                Timber.e("queryProfiles: $t")
                onFailure?.invoke(context.getString(R.string.geq_api_network_error, t.localizedMessage))
            }
        })
    }

    fun getProfile(id: Long, onResponse: ((String, AeqSearchResult) -> Unit), onFailure: ((String) -> Unit)?) {
        val call = service.getProfile(id)
        call.enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                if (response.code() == 200) {
                    val result = AeqSearchResult(
                        response.headers().get(HEADER_PROFILE_NAME),
                        response.headers().get(HEADER_PROFILE_SOURCE),
                        response.headers().get(HEADER_PROFILE_RANK)?.toIntOrNull(),
                        response.headers().get(HEADER_PROFILE_ID)?.toLongOrNull()
                    )

                    onResponse.invoke(response.body()!!, result)
                }
                else {
                    onFailure?.invoke(context.getString(R.string.geq_api_network_error,
                        context.getString(R.string.geq_api_network_error_details_code, response.code())
                    ))
                    Timber.e("getProfile: Server responded with ${response.code()} (${response.errorBody().toString()})")
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                Timber.e("getProfile: $t")
                onFailure?.invoke(context.getString(R.string.geq_api_network_error, t.localizedMessage))
            }
        })
    }

    companion object {
        private const val API_URL = "https://aeq.timschneeberger.me/"
        private const val HEADER_PARTIAL_RESULT = "X-Partial-Result"
        private const val HEADER_PROFILE_ID = "X-Profile-Id"
        private const val HEADER_PROFILE_NAME = "X-Profile-Name"
        private const val HEADER_PROFILE_SOURCE = "X-Profile-Source"
        private const val HEADER_PROFILE_RANK = "X-Profile-Rank"
    }
}