package me.timschneeberger.rootlessjamesdsp.api

import me.timschneeberger.rootlessjamesdsp.model.api.AeqSearchResult
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface AutoEqService {
    @GET("results/search/{query}")
    fun queryProfiles(@Path("query") query: String): Call<Array<AeqSearchResult>>
    @GET("results/{id}")
    fun getProfile(@Path("id") id: Long): Call<String>
}