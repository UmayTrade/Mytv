// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.util.Base64
import android.util.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.fixUrlNull
import com.lagradost.cloudstream3.utils.getAndUnpack
import com.lagradost.cloudstream3.utils.newExtractorLink

open class CloseLoadFm : ExtractorApi() {
    override val name            = "CloseLoadFm"
    override val mainUrl         = "https://closeload.filmmakinesi.to"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val extRef = referer ?: "$mainUrl/"
        Log.d("CloseLoadFm", "url » $url")
        Log.d("CloseLoadFm", "referer » $extRef")

        // 1) Iframe sayfasını al
        val iSource  = app.get(url, referer = extRef)
        val document = iSource.document

        // 2) Altyazıları çek
        document.select("track").forEach { track ->
            val lang = track.attr("label").ifBlank { "Unknown" }
            val src  = track.attr("src")
            if (src.isNotBlank()) {
                subtitleCallback.invoke(
                    SubtitleFile(
                        lang = lang,
                        url  = if (src.startsWith("http")) src else "$mainUrl/${src.trimStart('/')}"
                    )
                )
            }
        }

        // 3) dc_hello base64 string'ini bul
        var b64: String? = null

        // Direkt script'lerde ara
        for (script in document.select("script")) {
            val scriptData = script.data()
            if (scriptData.contains("dc_hello")) {
                val match = Regex("""dc_hello\(\s*["']([^"']+)["']\s*\)""").find(scriptData)
                if (match != null) {
                    b64 = match.groupValues[1]
                    Log.d("CloseLoadFm", "b64 (direct) » $b64")
                    break
                }
            }
        }

        // Bulunamazsa, getAndUnpack ile packed script'leri aç
        if (b64.isNullOrBlank()) {
            for (script in document.select("script")) {
                val raw = script.data().trim()
                if (raw.contains("eval(function(p,a,c,k,e,d)")) {
                    try {
                        val unpacked = getAndUnpack(raw)
                        val match = Regex("""dc_hello\(\s*["']([^"']+)["']\s*\)""").find(unpacked)
                        if (match != null) {
                            b64 = match.groupValues[1]
                            Log.d("CloseLoadFm", "b64 (unpacked) » $b64")
                            break
                        }
                    } catch (e: Exception) {
                        Log.e("CloseLoadFm", "unpack hata: ${e.message}")
                    }
                }
            }
        }

        if (b64.isNullOrBlank()) {
            Log.e("CloseLoadFm", "dc_hello base64 bulunamadı!")
            return
        }

        // 4) m3u8 linkini decode et
        val m3uLink = decodeDcHello(b64)
        Log.d("CloseLoadFm", "m3uLink » $m3uLink")

        if (m3uLink.isBlank()) {
            Log.e("CloseLoadFm", "m3uLink boş!")
            return
        }

        callback.invoke(
            newExtractorLink(
                source = this.name,
                name   = this.name,
                url    = m3uLink,
                type   = ExtractorLinkType.M3U8
            ) {
                this.referer = "$mainUrl/"
                this.quality = Qualities.Unknown.value
                this.headers = mapOf(
                    "Referer"    to "$mainUrl/",
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                )
            }
        )
    }

    /**
     * dc_hello decode:
     *   base64(str1) -> ters çevir -> base64 decode -> "xxx|URL"
     */
    private fun decodeDcHello(input: String): String {
        return try {
            val first = String(Base64.decode(input, Base64.DEFAULT))
            Log.d("CloseLoadFm", "first    » $first")

            val reversed = first.reversed()
            Log.d("CloseLoadFm", "reversed » $reversed")

            val second = String(Base64.decode(reversed, Base64.DEFAULT))
            Log.d("CloseLoadFm", "second   » $second")

            when {
                second.contains("|") -> second.split("|")[1]
                second.contains("+") -> second.substringAfterLast("+")
                else                 -> second
            }
        } catch (e: Exception) {
            Log.e("CloseLoadFm", "decode hata: ${e.message}")
            ""
        }
    }
}
