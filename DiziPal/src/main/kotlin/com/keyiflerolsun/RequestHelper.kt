package com.keyiflerolsun

import kotlinx.coroutines.delay
import okhttp3.Request

object RequestHelper {

    private const val REQUEST_DELAY = 150L

    suspend fun get(url: String, referer: String? = null): String? {
        delay(REQUEST_DELAY)

        val requestBuilder = Request.Builder()
            .url(url)
            .get()

        referer?.let {
            requestBuilder.header("Referer", it)
            requestBuilder.header("Origin", it)
        }

        val response = HttpClient.client
            .newCall(requestBuilder.build())
            .execute()

        if (!response.isSuccessful) return null

        return response.body?.string()
    }
}
