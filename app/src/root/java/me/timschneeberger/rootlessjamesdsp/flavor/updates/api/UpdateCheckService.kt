package me.timschneeberger.rootlessjamesdsp.flavor.updates.api

import me.timschneeberger.rootlessjamesdsp.flavor.updates.model.UpdateCheckResponse
import me.timschneeberger.rootlessjamesdsp.model.api.AeqSearchResult
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming

interface UpdateCheckService {
    @GET("updates/check/rootlessjamesdsp/{flavor}/{versionCode}")
    fun checkUpdate(@Path("flavor") flavor: String, @Path("versionCode") versionCode: String): Call<UpdateCheckResponse>

    @Streaming
    @GET("updates/download/rootlessjamesdsp/{flavor}")
    suspend fun downloadUpdate(@Path("flavor") flavor: String): ResponseBody
}