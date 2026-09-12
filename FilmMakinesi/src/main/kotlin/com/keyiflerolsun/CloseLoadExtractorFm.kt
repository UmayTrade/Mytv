// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.util.Base64
import android.util.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*

open class CloseLoadFm : ExtractorApi() {
    override val name            = "CloseLoadFm"
    override val mainUrl         = "https://closeload.filmmakinesi.to" // Güncel domain
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val extRef = referer ?: ""
        Log.d("CloseLoadFm", "url » $url")

        val iSource = app.get(url, referer = extRef)
        val document = iSource.document

        // Altyazıları çek
        document.select("track").forEach {
            val lang = it.attr("label").let { label ->
                when (label) {
                    "Turkish" -> "Turkish"
                    "English" -> "English"
                    "French"  -> "French"
                    else -> label
                }
            }
            subtitleCallback.invoke(
                SubtitleFile(
                    lang = lang,
                    url  = fixUrl(it.attr("src"))
                )
            )
        }

        // ! DÜZELTME: Script'ler içinde dc_hello fonksiyonunu ara
        val scripts = document.select("script[type=text/javascript]")
        var m3uLink: String? = null

        for (script in scripts) {
            val scriptData = script.data()
            if (scriptData.contains("dc_hello")) {
                // dc_hello("base64string") formatını regex ile yakala
                val regex = Regex("""dc_hello\("([^"]+)"""")
                val match = regex.find(scriptData)
                if (match != null) {
                    val b64 = match.groupValues[1]
                    Log.d("CloseLoadFm", "b64 = $b64")
                    m3uLink = decodeDcHello(b64)
                    Log.d("CloseLoadFm", "m3uLink = $m3uLink")
                    break
                }
            }
        }

        if (m3uLink.isNullOrBlank()) {
            Log.e("CloseLoadFm", "m3uLink bulunamadı!")
            return
        }

        callback.invoke(
            newExtractorLink(
                source = this.name,
                name = this.name,
                url = m3uLink,
                type = ExtractorLinkType.M3U8
            ) {
                this.referer = "$mainUrl/"
                this.quality = Qualities.Unknown.value
                this.headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                )
            }
        )
    }

    private fun decodeDcHello(input: String): String {
        // 1. Base64 decode
        val firstDecoded = String(Base64.decode(input, Base64.DEFAULT))
        Log.d("CloseLoadFm", "firstDecoded = $firstDecoded")

        // 2. Reverse ve tekrar Base64 decode
        val reversed = firstDecoded.reversed()
        Log.d("CloseLoadFm", "reversed = $reversed")

        val secondDecoded = String(Base64.decode(reversed, Base64.DEFAULT))
        Log.d("CloseLoadFm", "secondDecoded = $secondDecoded")

        // 3. "xxx|URL" formatından URL'yi al
        return if (secondDecoded.contains("|")) {
            secondDecoded.split("|")[1]
        } else if (secondDecoded.contains("+")) {
            secondDecoded.substringAfterLast("+")
        } else {
            secondDecoded
        }
    }
}
