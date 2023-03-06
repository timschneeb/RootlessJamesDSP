package me.timschneeberger.rootlessjamesdsp.flavor.updates.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class UpdateCheckResponse(
    @SerializedName("name") var name: String? = null,
    @SerializedName("versionName") var versionName: String? = null,
    @SerializedName("versionCode") var versionCode: Int? = null
) : Serializable
