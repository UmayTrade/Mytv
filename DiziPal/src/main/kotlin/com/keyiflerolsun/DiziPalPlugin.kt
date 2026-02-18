package com.keyiflerolsun

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import okhttp3.*
import java.io.IOException
import java.util.concurrent.TimeUnit

@CloudstreamPlugin
class DiziPalPlugin : Plugin() {

    override fun load(context: Context) {

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(WebView(context), true)

        val client = app.baseClient.newBuilder()
            .cookieJar(JavaNetCookieJar(java.net.CookieManager()))
            .addInterceptor(GlobalInterceptor(context))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        app.baseClient = client

        registerMainAPI(DiziPal())
    }
}
