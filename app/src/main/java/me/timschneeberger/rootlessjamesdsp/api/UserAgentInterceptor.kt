package me.timschneeberger.rootlessjamesdsp.api

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class UserAgentInterceptor(private val userAgent: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originRequest: Request = chain.request()
        val requestWithUserAgent: Request = originRequest.newBuilder()
            .header("User-Agent", userAgent)
            .build()
        return chain.proceed(requestWithUserAgent)
    }
}