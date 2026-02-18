package com.keyiflerolsun

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response

@CloudstreamPlugin
class DiziPalPlugin : Plugin() {

    override fun load(context: Context) {

        // 🌍 GLOBAL HEADER INJECTOR
        app.baseClient = app.baseClient.newBuilder()
            .addInterceptor(GlobalHeaderInterceptor())
            .build()

        registerMainAPI(DiziPal())
    }
}

class GlobalHeaderInterceptor : Interceptor {

    private val mainUrl = "https://dizipal1539.com"

    override fun intercept(chain: Interceptor.Chain): Response {

        val original = chain.request()

        val request = original.newBuilder()
            .header("User-Agent", USER_AGENT)
            .header("Referer", mainUrl)
            .header("Origin", mainUrl)
            .header("Accept", "*/*")
            .header("Connection", "keep-alive")
            .build()

        return chain.proceed(request)
    }
}
