package me.timschneeberger.rootlessjamesdsp.model.api

import com.google.gson.annotations.SerializedName

data class AeqSearchResult (
    @SerializedName("n") var name: String? = null,
    @SerializedName("s") var source: String? = null,
    @SerializedName("r") var rank: Int? = null,
    @SerializedName("i") var id: Long? = null
)